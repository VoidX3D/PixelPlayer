package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.database.AiCacheDao
import com.theveloper.pixelplay.data.database.AiCacheEntity
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiCacheManager @Inject constructor(
    private val cacheDao: AiCacheDao
) {
    private val CACHE_TTL_MS = 1000L * 60 * 30

    private fun String.sha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(this.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun buildHash(vararg parts: String): String {
        return parts.joinToString("|").sha256()
    }

    suspend fun get(hash: String): String? {
        cacheDao.getCache(hash)?.let { cached ->
            val age = System.currentTimeMillis() - cached.timestamp
            if (age < CACHE_TTL_MS) {
                return cached.responseJson
            }
        }
        return null
    }

    suspend fun put(hash: String, response: String) {
        cacheDao.insert(AiCacheEntity(
            promptHash = hash,
            responseJson = response,
            timestamp = System.currentTimeMillis()
        ))
    }

    suspend fun clearExpired() {
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MS
        cacheDao.clearOldCache(cutoff)
    }

    suspend fun clearAll() {
        cacheDao.clearAllCache()
    }
}
