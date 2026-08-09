package app.yap.feature.auth.data.local

import kotlinx.serialization.json.Json

/**
 * Encodes the stored session for platform secure storage. Unreadable stored text decodes to
 * `null` so a corrupted blob is treated as "no session" instead of failing startup.
 */
internal object SessionSerialization {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(text: String): SessionDb? = runCatching {
        json.decodeFromString(SessionDb.serializer(), text)
    }.getOrNull()

    fun encode(session: SessionDb): String = json.encodeToString(SessionDb.serializer(), session)
}
