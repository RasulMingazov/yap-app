package app.yap.feature.auth.data.local

import kotlinx.serialization.json.Json

/**
 * Encodes the stored session for platform secure storage. Unreadable stored text decodes to
 * `null` so a corrupted blob is treated as "no session" instead of failing startup.
 */
internal object SessionSerialization {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(text: String): SessionLocal? = runCatching {
        json.decodeFromString(SessionLocal.serializer(), text)
    }.getOrNull()

    fun encode(session: SessionLocal): String = json.encodeToString(SessionLocal.serializer(), session)
}
