package app.yap.server.feature.auth.persistence

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.SessionRotation
import app.yap.server.feature.auth.model.SessionRotationResult
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
     * Removes every expired challenge in its own committed transaction and returns how many rows it
     * deleted. Cleanup is deliberately separate from any login attempt: a rejected attempt rolls
     * back, so a deletion made inside it would never take effect.
     */
    suspend fun deleteExpiredChallenges(): Int

    /**
     * Reads a challenge without locking it, so verification can run before the transaction opens.
     * Its expiry is only a pre-check; [consumeChallengeAndCreateSession] owns the decision.
     */
    suspend fun findChallenge(id: String): AuthChallenge?

    suspend fun insertChallenge(challenge: AuthChallenge)

    /**
     * Locks the session named by [rotation], validates inactivity and absolute expiry against the
     * time read inside the transaction, compares the presented hash with the stored ones, and — for
     * the current hash — moves it to the previous hash and stores the rotated one, all atomically.
     *
     * A credential equal to the stored previous hash is a replay: it revokes the whole session in
     * the same transaction and returns [SessionRotationResult.Replayed]. Every other non-match is
     * [SessionRotationResult.Unknown], decided by reading the locked row rather than by inferring
     * replay from a conditional update that affected no rows.
     */
    suspend fun rotateSession(rotation: SessionRotation): SessionRotationResult
}
