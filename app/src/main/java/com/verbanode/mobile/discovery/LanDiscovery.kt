package com.verbanode.mobile.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.util.concurrent.ConcurrentHashMap


data class DiscoveredServer(
    val serviceName: String,
    val baseUrl: String,
    val instanceId: String?,
    val version: String?,
    val apiVersion: Int?,
    val wsVersion: Int?,
    val spkiSha256: String?,
)

class LanDiscovery(
    context: Context,
    private val onUpdate: (List<DiscoveredServer>) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val services = ConcurrentHashMap<String, DiscoveredServer>()
    private var listener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start() {
        if (listener != null) return
        runCatching {
            multicastLock = wifi.createMulticastLock("VerbaNodeDiscovery").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("LAN discovery could not start ($errorCode)")
                stop()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                services.remove(serviceInfo.serviceName)
                publish()
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.contains("_verbanode._tcp")) return
                resolve(serviceInfo)
            }
        }
        listener = discoveryListener
        try {
            nsd.discoverServices("_verbanode._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (error: SecurityException) {
            listener = null
            releaseLock()
            onError("Local-network permission is required for discovery")
        } catch (error: Exception) {
            listener = null
            releaseLock()
            onError(error.message ?: "LAN discovery failed")
        }
    }

    @Suppress("DEPRECATION")
    private fun resolve(serviceInfo: NsdServiceInfo) {
        nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = if (Build.VERSION.SDK_INT >= 34) {
                    info.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress
                        ?: info.hostAddresses.firstOrNull()?.hostAddress
                } else {
                    info.host?.hostAddress
                } ?: return
                val attributes = info.attributes.mapValues { (_, bytes) -> bytes.toString(Charsets.UTF_8) }
                if (attributes["product"] != null && attributes["product"] != "VerbaNode") return
                services[info.serviceName] = DiscoveredServer(
                    serviceName = info.serviceName.substringBefore("._verbanode"),
                    baseUrl = "https://$host:${info.port}",
                    instanceId = attributes["instance_id"],
                    version = attributes["version"],
                    apiVersion = attributes["api"]?.toIntOrNull(),
                    wsVersion = attributes["ws"]?.toIntOrNull(),
                    spkiSha256 = attributes["spki"]?.lowercase()?.takeIf { it.length == 64 },
                )
                publish()
            }
        })
    }

    private fun publish() = onUpdate(services.values.sortedBy { it.serviceName.lowercase() })

    fun stop(clearResults: Boolean = true) {
        val current = listener
        listener = null
        if (current != null) runCatching { nsd.stopServiceDiscovery(current) }
        releaseLock()
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
