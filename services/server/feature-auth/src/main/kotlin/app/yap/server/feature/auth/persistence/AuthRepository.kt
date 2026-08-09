package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.VerifiedIdentity

/** Persistence for the authentication aggregates: challenges, accounts, identities, and sessions. */
internal interface AuthRepository {

    /**
     * Locks [challenge], re-checks it against the stored row, consumes it, resolves or creates the
     * account for [identity], and stores [session] — all in one transaction.
     *
     * Expiry is re-checked against the time read inside the transaction, after the row is locked,
     * never against a time the caller captured earlier: provider verification runs outside the
     * transaction and may itself outlive the challenge.
     *
     * Returns `null` when the locked challenge is no longer usable, which happens when it expired
     * or was already consumed by a concurrent attempt. Nothing is written in that case, so a
     * rejected attempt leaves no account, identity, or session behind.
     */
    suspend fun consumeChallengeAndCreateSession(
        challenge: AuthChallenge,
        identity: VerifiedIdentity,
        session: NewSession,
    ): AuthAccount?

    /**
     * Reads a challenge without locking it, so verification can run before the transaction opens.
     * Its expiry is only a pre-check; [consumeChallengeAndCreateSession] owns the decision.
     */
    suspend fun findChallenge(id: String): AuthChallenge?

    suspend fun insertChallenge(challenge: AuthChallenge)
}
