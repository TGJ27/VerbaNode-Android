package com.verbanode.mobile.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.verbanode.mobile.network.TlsTrust
import com.verbanode.mobile.storage.ServerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


data class DiscoveredServer(
    val serviceName: String,
    val baseUrl: String,
    val instanceId: String?,
    val version: String?,
    val apiVersion: Int?,
    val wsVersion: Int?,
    val spkiSha256: String?,
    val discoverySource: DiscoverySource = DiscoverySource.MDNS,
    val verified: Boolean = true,
)

private data class ResolveRequest(val info: NsdServiceInfo, val attempt: Int)
private data class LocalIpv4(val address: String, val prefixLength: Int)

/**
 * Multi-path local discovery. mDNS and UDP are hints only: a server is not
 * published until an unauthenticated HTTPS /api/client-info probe validates
 * its protocol contract and certificate identity.
 */
class LanDiscovery(
    context: Context,
    private val savedProfiles: List<ServerProfile> = emptyList(),
    private val onUpdate: (List<DiscoveredServer>) -> Unit,
    private val onStage: (DiscoveryStage) -> Unit = {},
    private val onWarning: (String) -> Unit = {},
) {
    companion object {
        private const val UDP_LISTEN_MS = 3_200L
        private const val SUBNET_DELAY_MS = 1_900L
        private const val SUBNET_CONNECT_TIMEOUT_MS = 300L
        private const val SUBNET_READ_TIMEOUT_MS = 600L
        private const val MAX_SUBNET_CONCURRENCY = 32
    }

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val services = ConcurrentHashMap<String, DiscoveredServer>()
    private val inFlight: MutableSet<String> = newConcurrentMutableSet()
    private val resolveQueue = ConcurrentLinkedQueue<ResolveRequest>()
    private val resolveBusy = AtomicBoolean(false)
    private val running = AtomicBoolean(false)
    private var listener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    @Volatile private var udpSocket: DatagramSocket? = null
    private var subnetJob: Job? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        acquireMulticastLock()
        onStage(DiscoveryStage.SAVED)
        startNsd()
        scope.launch { probeSavedProfiles() }
        scope.launch { runUdpDiscovery() }
        subnetJob = scope.launch {
            delay(SUBNET_DELAY_MS)
            if (!running.get()) return@launch
            onStage(DiscoveryStage.SUBNET)
            runSubnetFallback()
        }
        scope.launch {
            delay(250L)
            if (running.get()) onStage(DiscoveryStage.LAN)
        }
    }

    private fun acquireMulticastLock() {
        runCatching {
            multicastLock = wifi.createMulticastLock("VerbaNodeDiscovery").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.onFailure { onWarning("Wi-Fi multicast could not be enabled; fallback discovery will still run.") }
    }

    private suspend fun probeSavedProfiles() = coroutineScope {
        savedProfiles.map { profile ->
            launch {
                verifyBaseUrl(
                    baseUrl = profile.baseUrl,
                    expectedSpki = profile.spkiSha256,
                    source = DiscoverySource.SAVED,
                    fallbackName = profile.name,
                    instanceIdHint = profile.instanceId,
                    connectTimeoutMs = 800L,
                    readTimeoutMs = 1_200L,
                )
            }
        }.joinAll()
    }

    private fun startNsd() {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                listener = null
                onWarning("mDNS discovery could not start ($errorCode); active LAN discovery is still running.")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.contains("_verbanode._tcp")) return
                enqueueResolve(serviceInfo, 0)
            }
        }
        listener = discoveryListener
        try {
            nsd.discoverServices("_verbanode._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (_: SecurityException) {
            listener = null
            onWarning("Local-network permission is required for mDNS; HTTPS fallback discovery is still running.")
        } catch (error: Exception) {
            listener = null
            onWarning(error.message ?: "mDNS discovery failed; fallback discovery is still running.")
        }
    }

    private fun enqueueResolve(info: NsdServiceInfo, attempt: Int) {
        if (!running.get()) return
        resolveQueue.offer(ResolveRequest(info, attempt))
        drainResolveQueue()
    }

    @Suppress("DEPRECATION")
    private fun drainResolveQueue() {
        if (!running.get() || !resolveBusy.compareAndSet(false, true)) return
        val request = resolveQueue.poll()
        if (request == null) {
            resolveBusy.set(false)
            return
        }
        try {
            nsd.resolveService(request.info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    resolveBusy.set(false)
                    val retryDelay = resolveRetryDelayMs(request.attempt)
                    if (retryDelay != null && running.get()) {
                        scope.launch {
                            delay(retryDelay)
                            enqueueResolve(serviceInfo, request.attempt + 1)
                        }
                    }
                    drainResolveQueue()
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    resolveBusy.set(false)
                    resolveNsdCandidate(serviceInfo)?.let(::verifyCandidateAsync)
                    drainResolveQueue()
                }
            })
        } catch (error: Exception) {
            resolveBusy.set(false)
            val retryDelay = resolveRetryDelayMs(request.attempt)
            if (retryDelay != null && running.get()) {
                scope.launch {
                    delay(retryDelay)
                    enqueueResolve(request.info, request.attempt + 1)
                }
            }
            drainResolveQueue()
        }
    }

    private fun resolveNsdCandidate(info: NsdServiceInfo): DiscoveryCandidate? {
        val host = if (Build.VERSION.SDK_INT >= 34) {
            info.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress
                ?: info.hostAddresses.firstOrNull()?.hostAddress
        } else {
            @Suppress("DEPRECATION")
            info.host?.hostAddress
        } ?: return null
        val attributes = info.attributes.mapValues { (_, bytes) -> bytes.toString(Charsets.UTF_8) }
        if (attributes["product"] != null && attributes["product"] != "VerbaNode") return null
        return DiscoveryCandidate(
            serviceName = info.serviceName.substringBefore("._verbanode").substringBefore("._tcp"),
            host = host,
            httpsPort = info.port,
            instanceId = attributes["instance_id"],
            version = attributes["version"],
            apiVersion = attributes["api"]?.toIntOrNull(),
            wsVersion = attributes["ws"]?.toIntOrNull(),
            spkiSha256 = attributes["spki"]?.lowercase()?.takeIf { it.length == 64 },
            source = DiscoverySource.MDNS,
        )
    }

    private fun verifyCandidateAsync(candidate: DiscoveryCandidate) {
        scope.launch {
            verifyBaseUrl(
                baseUrl = candidate.baseUrl,
                expectedSpki = candidate.spkiSha256,
                source = candidate.source,
                fallbackName = candidate.serviceName,
                instanceIdHint = candidate.instanceId,
                connectTimeoutMs = 900L,
                readTimeoutMs = 1_500L,
            )
        }
    }

    private suspend fun runUdpDiscovery() {
        val socket = runCatching {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = 250
                wifiNetwork()?.bindSocket(this)
                bind(InetSocketAddress(0))
            }
        }.getOrElse {
            onWarning("Active LAN broadcast discovery could not start; mDNS/HTTPS fallback will continue.")
            return
        }
        udpSocket = socket
        try {
            coroutineScope {
                val sender = launch {
                    val local = localIpv4()
                    val destinations = buildSet {
                        add("255.255.255.255")
                        if (local != null) runCatching {
                            add(directedBroadcastAddress(local.address, local.prefixLength))
                        }
                    }
                    val ports = buildSet {
                        add(DEFAULT_VERBANODE_PORT)
                        savedProfiles.mapNotNullTo(this) { rememberedPort(it.baseUrl) }
                    }
                    repeat(3) { round ->
                        if (round > 0) delay(if (round == 1) 650L else 900L)
                        destinations.forEach { host ->
                            ports.forEach { port ->
                                runCatching {
                                    val bytes = ACTIVE_DISCOVERY_REQUEST.toByteArray(Charsets.US_ASCII)
                                    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(host), port))
                                }
                            }
                        }
                    }
                }
                val deadline = System.nanoTime() + UDP_LISTEN_MS * 1_000_000L
                val buffer = ByteArray(8_192)
                while (isActive && running.get() && System.nanoTime() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    } catch (_: Exception) {
                        if (running.get()) onWarning("LAN broadcast response could not be read; HTTPS fallback will continue.")
                        break
                    }
                    val host = packet.address?.hostAddress ?: continue
                    val payload = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                    val candidate = runCatching {
                        val json = JSONObject(payload)
                        val keys = listOf(
                            "product", "discovery_protocol", "instance_id", "instance_name", "version",
                            "https_port", "api_version", "websocket_protocol_version", "spki_sha256",
                        )
                        val fields = keys.associateWith { key -> json.opt(key)?.toString().orEmpty() }
                        parseActiveDiscoveryFields(fields, host, DiscoverySource.UDP)
                    }.getOrNull() ?: continue
                    verifyCandidateAsync(candidate)
                }
                sender.join()
            }
        } finally {
            if (udpSocket === socket) udpSocket = null
            socket.close()
        }
    }

    private suspend fun runSubnetFallback() {
        val local = localIpv4() ?: run {
            onWarning("Android could not read the Wi-Fi IPv4 address; mDNS and UDP discovery were still attempted.")
            return
        }
        val hosts = runCatching { subnetFallbackHosts(local.address, local.prefixLength) }.getOrElse { return }
        val ports = buildSet {
            add(DEFAULT_VERBANODE_PORT)
            savedProfiles.mapNotNullTo(this) { rememberedPort(it.baseUrl) }
        }
        val semaphore = Semaphore(MAX_SUBNET_CONCURRENCY)
        coroutineScope {
            hosts.flatMap { host ->
                ports.map { port ->
                    launch {
                        semaphore.withPermit {
                            if (!running.get()) return@withPermit
                            verifyBaseUrl(
                                baseUrl = "https://$host:$port",
                                expectedSpki = null,
                                source = DiscoverySource.SUBNET,
                                fallbackName = "VerbaNode",
                                instanceIdHint = null,
                                connectTimeoutMs = SUBNET_CONNECT_TIMEOUT_MS,
                                readTimeoutMs = SUBNET_READ_TIMEOUT_MS,
                            )
                        }
                    }
                }
            }.joinAll()
        }
    }

    private suspend fun verifyBaseUrl(
        baseUrl: String,
        expectedSpki: String?,
        source: DiscoverySource,
        fallbackName: String,
        instanceIdHint: String?,
        connectTimeoutMs: Long,
        readTimeoutMs: Long,
    ) {
        if (!running.get()) return
        val hintKey = discoveryIdentityKey(instanceIdHint, expectedSpki, baseUrl)
        if (!inFlight.add(hintKey)) return
        try {
            val result = runCatching {
                TlsTrust.probe(
                    baseUrl,
                    expectedSpki,
                    connectTimeoutMs,
                    readTimeoutMs,
                    wifiNetwork()?.socketFactory,
                )
            }.getOrNull() ?: return
            if (!running.get()) return
            val info = result.clientInfo
            val verified = DiscoveredServer(
                serviceName = info.instanceName?.takeIf { it.isNotBlank() } ?: fallbackName,
                baseUrl = result.baseUrl,
                instanceId = info.instanceId,
                version = info.serverVersion,
                apiVersion = info.apiVersion,
                wsVersion = info.websocketVersion,
                spkiSha256 = result.certificateSpkiSha256,
                discoverySource = source,
                verified = true,
            )
            val key = discoveryIdentityKey(verified.instanceId, verified.spkiSha256, verified.baseUrl)
            services[key] = choosePreferred(services[key], verified)
            publish()
        } finally {
            inFlight.remove(hintKey)
        }
    }

    private fun choosePreferred(existing: DiscoveredServer?, candidate: DiscoveredServer): DiscoveredServer {
        if (existing == null) return candidate
        val rank = mapOf(
            DiscoverySource.SAVED to 4,
            DiscoverySource.UDP to 3,
            DiscoverySource.MDNS to 2,
            DiscoverySource.SUBNET to 1,
        )
        return if ((rank[candidate.discoverySource] ?: 0) >= (rank[existing.discoverySource] ?: 0)) candidate else existing
    }

    // Deliberately enumerate every connected network so discovery can stay on Wi-Fi
    // even when Android chooses cellular as the default route.
    @Suppress("DEPRECATION")
    private fun wifiNetwork(): Network? = connectivity.allNetworks.firstOrNull { network ->
        connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    private fun localIpv4(): LocalIpv4? {
        val network = wifiNetwork() ?: connectivity.activeNetwork ?: return null
        val properties = connectivity.getLinkProperties(network) ?: return null
        val link = properties.linkAddresses.firstOrNull { entry ->
            val address = entry.address
            address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress
        } ?: return null
        val host = link.address.hostAddress ?: return null
        return LocalIpv4(host, link.prefixLength)
    }

    private fun publish() = onUpdate(
        services.values.sortedWith(compareBy<DiscoveredServer> { it.serviceName.lowercase() }.thenBy { it.baseUrl })
    )

    fun stop(clearResults: Boolean = true) {
        if (!running.getAndSet(false)) return
        subnetJob?.cancel()
        subnetJob = null
        udpSocket?.close()
        udpSocket = null
        val current = listener
        listener = null
        if (current != null) runCatching { nsd.stopServiceDiscovery(current) }
        resolveQueue.clear()
        resolveBusy.set(false)
        releaseLock()
        scope.cancel()
        onStage(DiscoveryStage.COMPLETE)
        if (clearResults) {
            services.clear()
            publish()
        }
    }

    private fun releaseLock() {
        runCatching { if (multicastLock?.isHeld == true) multicastLock?.release() }
        multicastLock = null
    }
}
