package com.verbanode.mobile.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore(name = "verbanode_mobile")

data class ServerProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val spkiSha256: String,
    val instanceId: String? = null,
    val deviceId: String? = null,
    val encryptedDeviceToken: String? = null,
    val lastServerVersion: String? = null,
) {
    val paired: Boolean get() = !deviceId.isNullOrBlank() && !encryptedDeviceToken.isNullOrBlank()
}

class SecretBox {
    private val alias = "verbanode_mobile_device_credentials_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(1 + cipher.iv.size + encrypted.size)
        payload[0] = cipher.iv.size.toByte()
        cipher.iv.copyInto(payload, 1)
        encrypted.copyInto(payload, 1 + cipher.iv.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(payload: String): String? = runCatching {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val ivSize = bytes[0].toInt() and 0xff
        require(ivSize in 12..32 && bytes.size > 1 + ivSize)
        val iv = bytes.copyOfRange(1, 1 + ivSize)
        val encrypted = bytes.copyOfRange(1 + ivSize, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrNull()
}

class ProfileStore(private val context: Context) {
    private val profilesKey = stringPreferencesKey("server_profiles_v1")
    val secretBox = SecretBox()

    suspend fun profiles(): List<ServerProfile> {
        val raw = context.dataStore.data.first()[profilesKey] ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ServerProfile(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = item.optString("name", "VerbaNode"),
                        baseUrl = item.optString("base_url"),
                        spkiSha256 = item.optString("spki_sha256").lowercase(),
                        instanceId = item.optString("instance_id").ifBlank { null },
                        deviceId = item.optString("device_id").ifBlank { null },
                        encryptedDeviceToken = item.optString("device_token").ifBlank { null },
                        lastServerVersion = item.optString("server_version").ifBlank { null },
                    )
                )
            }
        }.filter { it.baseUrl.isNotBlank() && it.spkiSha256.length == 64 }
    }

    suspend fun upsert(profile: ServerProfile) {
        val profiles = profiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id || (!profile.instanceId.isNullOrBlank() && it.instanceId == profile.instanceId) }
        if (index >= 0) profiles[index] = profile else profiles.add(profile)
        save(profiles)
    }

    suspend fun remove(profileId: String) {
        save(profiles().filterNot { it.id == profileId })
    }

    private suspend fun save(profiles: List<ServerProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("base_url", profile.baseUrl)
                    .put("spki_sha256", profile.spkiSha256)
                    .put("instance_id", profile.instanceId ?: "")
                    .put("device_id", profile.deviceId ?: "")
                    .put("device_token", profile.encryptedDeviceToken ?: "")
                    .put("server_version", profile.lastServerVersion ?: "")
            )
        }
        context.dataStore.edit { it[profilesKey] = array.toString() }
    }
}
