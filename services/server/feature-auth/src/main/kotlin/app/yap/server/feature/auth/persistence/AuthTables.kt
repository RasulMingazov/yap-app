package app.yap.server.feature.auth.persistence

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

internal object UsersTable : UUIDTable("users") {

    val createdAt = timestamp("created_at")
}

internal object ProviderIdentitiesTable : UUIDTable("provider_identities") {

    val userId = reference("user_id", UsersTable)
    val provider = text("provider")
    val providerUserId = text("provider_user_id")
    val email = text("email").nullable()
    val displayName = text("display_name").nullable()
    val avatarUrl = text("avatar_url").nullable()
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(provider, providerUserId)
    }
}

internal object SessionsTable : UUIDTable("sessions") {

    val userId = reference("user_id", UsersTable)
    val refreshTokenHash = text("refresh_token_hash")
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val rotatedAt = timestamp("rotated_at").nullable()
}
