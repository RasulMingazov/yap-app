# Contract: Auth API and client-side ports

**Branch**: `feature/001-login-screen` | **Feature ID**: `001-login-screen` | **Refreshed**: 2026-08-15

Three endpoints, four auth DTOs, and one shared error shape. Serialized types live in
`shared/contract/*` and are shared by the mobile client and the server; server-only request shapes
stay beside their routes. Mobile maps DTO to domain at the repository boundary; the server maps DTO
to feature models at the route boundary.

Base path: `/v1/auth`. All three endpoints are unauthenticated — they are how a caller obtains
credentials — so the client sends them with `authenticated = false`, and all three are rate limited
per originating IP at 100 requests per minute (FR-033, [research.md](../research.md) R13).

Google login has **two entry points** because the two client paths return different artefacts
(FR-016): the native flow yields an ID token, the browser flow yields an authorization code. They
converge on the same account resolution, so only the front door differs.

## Shared DTOs

```kotlin
// shared/contract/auth
@Serializable data class GoogleCredentialsDto(val idToken: String, val nonce: String)

@Serializable data class GoogleAuthorizationCodeDto(
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)

@Serializable data class RefreshCredentialsDto(val refreshToken: String)

@Serializable data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long,
)

// shared/contract/common
@Serializable data class ErrorResponseDto(val error: String)

object ApiErrorCode {
    const val INVALID_REQUEST = "invalid_request"
    const val UNAUTHORIZED = "unauthorized"
    const val PROVIDER_UNAVAILABLE = "provider_unavailable"
}
```

Field order follows the wire contract, not alphabetical order —
`docs/001-code-conventions.md` exempts serialization contracts.

`SessionDto` carries credentials only; no profile field is added here.
`refreshTokenExpiresAtEpochSeconds` is the session's own 90-day expiry, sent so the client can
decide at launch whether a stored session is worth trusting without a network round trip (FR-024).
It is stated by the server rather than computed on the device.

`ErrorResponseDto` is shared rather than server-only: the client matches on the code the server
names, while the copy the user sees is always the app's own Russian string (FR-030 forbids exposing
provider or transport internals).

## `POST /v1/auth/google`

Exchange a Google ID token for a Yap session. Creates the account on first use.

**Request**: `GoogleCredentialsDto`

| Status | Body | When |
| --- | --- | --- |
| `200` | `SessionDto` | Token verified; account found or created |
| `400` | `ErrorResponseDto(invalid_request)` | Malformed request |
| `401` | `ErrorResponseDto(unauthorized)` | Bad signature, wrong `aud`/`iss`, expired, nonce mismatch, or `sub` absent |
| `429` | — | Per-IP rate limit exceeded (FR-033) |
| `503` | `ErrorResponseDto(provider_unavailable)` | Google's key set unreachable |

**Server behaviour**

1. Verify the ID token per [research.md](../research.md) R5. The server establishes this for itself
   and never trusts the caller (FR-017); nothing below runs until it succeeds, so no row is written
   for a request that fails here. A token with no `sub` fails here too (FR-021).
2. Look up `provider_identities` by `(google, sub)`.
3. Missing — insert `users` and `provider_identities` in one transaction. A unique violation from a
   concurrent first login is caught and retried as a lookup, so the loser returns the winner's
   account rather than an error.
4. Create a refresh token, persist its hash as a new `sessions` row, and issue tokens through
   `TokenService.issueTokens`.

The same Google account always resolves to the same `users` row (FR-019); a different provider
produces a separate row, because nothing matches on email (FR-020).

## `POST /v1/auth/google/code`

The browser-fallback door (FR-016); everything after the exchange is identical to `/v1/auth/google`.

**Request**: `GoogleAuthorizationCodeDto`. `401` additionally covers an invalid, expired, replayed,
or PKCE-mismatched code; `503` additionally covers Google's token endpoint being unreachable.

**Server behaviour**: `POST` to Google's token endpoint with the code, `code_verifier`,
`redirect_uri`, and the **Android** client ID, then verify the returned ID token exactly as above
and resolve the account. This door is Android-only by construction — the browser fallback exists
because Credential Manager needs Google's services, while iOS's SDK already runs in the system
browser and always returns an ID token — so the request carries no platform field and the server
needs no selection rule. Installed-app clients carry no secret, so PKCE is what proves the caller.
The code is single-use: replaying it returns `401`.

## `POST /v1/auth/refresh`

Rotate a session. Called by the client's `AccessTokenProvider` — after a rejected access token, and
at launch when the stored one is expired or within five minutes of expiring (FR-028). Never by
feature code.

**Request**: `RefreshCredentialsDto`

| Status | Body | When |
| --- | --- | --- |
| `200` | `SessionDto` | Rotated; the old refresh token is now dead |
| `400` | `ErrorResponseDto(invalid_request)` | Malformed token value |
| `401` | `ErrorResponseDto(unauthorized)` | Unknown, expired, or already-rotated session |
| `429` | — | Per-IP rate limit exceeded (FR-033) |

**Server behaviour**: parse the session id from the token, then within one transaction match the
stored hash, check `expires_at`, write the rotated hash and `rotated_at`, push `expires_at` another
90 days out, and issue new tokens. Two concurrent rotations therefore produce one `200` and one
`401` — never two valid sessions. A successful rotation here is the only event that renews a session
at all (FR-026).

**What signs the user out, and what does not.** The client reads this through `ApiResult`: only
`ApiError.Rejected` (a `4xx` carrying a code) and `ApiError.Unauthorized` are terminal — the server
has answered and refused the session, so local storage is cleared and `LoggedOut` is published
(FR-025). `ApiError.Unavailable` — a timeout, no connection, any `5xx`, a `408`, or a `429` — leaves
the stored session intact and is retried later; none of them is an answer about the session, and
treating one as an answer would sign a user out for losing signal or for sharing an address with a
noisy neighbour (SC-014).

## Errors

Failures are translated once, by the `StatusPages` mapping in `services/server/app`
(`ErrorMapping.kt`): `AuthFailure.MalformedInput` → `400 invalid_request`,
`AuthFailure.UnverifiableConfirmation` → `401 unauthorized`, `AuthFailure.ProviderUnavailable` →
`503 provider_unavailable`. Routes raise feature failures; they never build status codes.

That split decides where each half is proven. A `testApplication` inside `feature-auth` assembles
the feature's routes without `app`'s plugins, so it asserts the failure a route raises and the rows
it does or does not write; the mapping to `400`/`401`/`503`, and the `429` the limiter produces, are
proven by `app`'s own tests. `429` is not special-cased on the client: it takes the ordinary failure
path, so the user is told the attempt failed and can retry, not which server rule stopped them.

## Client-side ports

`GoogleCredentialProvider` is not HTTP, but it is a contract this feature publishes — Swift
implements it in the Xcode host.

```kotlin
// feature-auth/api — public because Swift implements it
interface GoogleCredentialProvider {

    suspend fun requestCredential(nonce: String): GoogleCredential
}

sealed interface GoogleCredential {

    data class IdToken(val value: String) : GoogleCredential

    data class AuthorizationCode(
        val code: String,
        val codeVerifier: String,
        val redirectUri: String,
    ) : GoogleCredential
}
```

One port, two credential shapes — which one comes back decides which endpoint the repository calls.
Android's implementation tries Credential Manager and falls back to the browser on its own; iOS
always returns an `IdToken`. Nothing above the repository knows which path ran, which is what keeps
FR-016's "the user is never told which one ran" honest. Both implementations signal user dismissal
by throwing `LoginCancelledException`, which the repository maps to `LoginOutcome.Cancelled` —
dismissing the browser tab is that same cancellation, not a failure.

`RefreshSessionUseCase` is the second published port, and it exists for the opposite reason:
`app-root` calls it rather than implements it.

```kotlin
// feature-auth/api — public because app-root calls it at launch
fun interface RefreshSessionUseCase { suspend operator fun invoke() }
```

No parameter and no result: the caller knows only that the launch decision has been made and the
user is logged in. *Whether* to refresh is decided inside `impl`, by `DefaultAuthSessionRepository`
reading the stored access-token expiry against FR-028's five-minute margin, because `SessionLocal`
is `internal`. Every outcome is owned there too. Returning a result would invite `app-root` to act
on one, and there is no action for it to take.
