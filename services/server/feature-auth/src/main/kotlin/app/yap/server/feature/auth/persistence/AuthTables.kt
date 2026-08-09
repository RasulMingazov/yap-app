package app.yap.server.feature.auth.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * The authentication tables, mirroring `db/migration/V1__auth.sql`. Flyway owns the schema; these
 * declarations exist only so queries stay typed. Column order follows the migration, not the
 * project's alphabetical convention.
 */
internal object ChallengeTable : Table("auth_challenge") {
    val id = uuid("id")
    val provider = text("provider")
    val nonceHash = text("nonce_hash").nullable()
    val proof = text("proof").nullable()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")

    override val primaryKey = PrimaryKey(id)
}

internal object AccountTable : Table("auth_account") {
    val id = uuid("id")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

internal object ProviderIdentityTable : Table("auth_provider_identity") {
    val id = uuid("id")
    val accountId = uuid("account_id").references(AccountTable.id)
    val provider = text("provider")
    val subject = text("subject")
    val email = text("email").nullable()
    val isEmailVerified = bool("is_email_verified").nullable()
    val createdAt = timestamp("created_at")
    val lastLoginAt = timestamp("last_login_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("auth_provider_identity_provider_subject_key", provider, subject)
    }
}

internal object SessionTable : Table("auth_session") {
    val id = uuid("id")
    val accountId = uuid("account_id").references(AccountTable.id)
    val refreshTokenHash = text("refresh_token_hash")
    val previousTokenHash = text("previous_token_hash").nullable()
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")
    val absoluteExpiresAt = timestamp("absolute_expires_at")
    val revokedAt = timestamp("revoked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
