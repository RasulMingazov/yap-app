# Auth API Contract

Three endpoints and four DTOs. Serialized types live in `shared/contract/auth` and are shared by
the mobile client and the server; server-only request/response shapes stay beside their routes.
Mobile maps DTO to domain at the repository boundary; the server maps DTO to feature models at
the route boundary.

Base path: `/v1/auth`. All three endpoints are unauthenticated — they are how a caller obtains
credentials — so none is marked `authenticated()` on the client, and all three are rate limited
per originating IP at 100 requests per minute (FR-027, `research.md` R13).

Google login has **two entry points** because the two client paths return different artefacts
(FR-029): the native flow yields an ID token, the browser flow yields an authorization code. They
converge on the same account resolution, so only the front door differs.

## Shared DTOs (`shared/contract/auth`)

```kotlin
@Serializable
data class GoogleCredentialsDto(
    val idToken: String,
    val nonce: String,
)

@Serializable
data class GoogleAuthorizationCodeDto(
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
)

@Serializable
data class RefreshCredentialsDto(
    val refreshToken: String,
)

@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long,
)
```

Field order follows the wire contract, not alphabetical order — `docs/001-code-conventions.md`
exempts serialization contracts from alphabetical ordering.

`SessionDto` carries credentials only. No profile field is added here: the shared contracts guide
requires credential and profile responses to stay separate, and nothing in this feature reads a
profile.

`refreshTokenExpiresAtEpochSeconds` is the session's own 90-day expiry, sent so the client can
decide at launch whether a stored session is worth trusting without a network round trip
(FR-011). It is stated by the server rather than computed on the device, so a client never has to
guess the window from a TTL it does not own.

## `POST /v1/auth/google`

Exchange a Google ID token for a Yap session. Creates the account on first use.

**Request**: `GoogleCredentialsDto`

**Responses**

| Status | Body | When |
| --- | --- | --- |
| `200 OK` | `SessionDto` | Token verified; account found or created |
| `400 Bad Request` | error body | Malformed request |
| `401 Unauthorized` | error body | Signature invalid, wrong `aud` or `iss`, expired, nonce mismatch, or `sub` absent |
| `429 Too Many Requests` | error body | Per-IP rate limit exceeded (FR-027) |
| `503 Service Unavailable` | error body | Google's key set unreachable |

**Server behavior**

1. Verify the ID token per `research.md` R5. The server establishes this for itself and never
   trusts the caller's word for it (FR-031); nothing below runs until it succeeds, so no `users`
   or `provider_identities` row is written for a request that fails here. A token with no `sub`
   fails here too — it cannot be matched on a later login (FR-026).
2. Look up `provider_identities` by `(google, sub)`.
3. Missing — insert `users` and `provider_identities` in one transaction. A unique-violation from
   a concurrent first login is caught and retried as a lookup, so the loser returns the winner's
   account rather than an error.
4. Create a refresh token, persist its hash as a new `sessions` row, and issue tokens through
   `TokenService.issueTokens`.

The same Google account always resolves to the same `users` row (FR-007), and a different provider
would produce a separate row because nothing matches on email (FR-008).

## `POST /v1/auth/google/code`

Exchange a Google authorization code for a Yap session. This is the browser-fallback door
(FR-029); everything after the exchange is identical to `/v1/auth/google`.

**Request**: `GoogleAuthorizationCodeDto`

**Responses**

| Status | Body | When |
| --- | --- | --- |
| `200 OK` | `SessionDto` | Code exchanged and the resulting ID token verified |
| `400 Bad Request` | error body | Malformed request |
| `401 Unauthorized` | error body | Code invalid, expired, already used, PKCE verifier mismatch, or the resulting ID token fails verification (including an absent `sub`) |
| `429 Too Many Requests` | error body | Per-IP rate limit exceeded (FR-027) |
| `503 Service Unavailable` | error body | Google's token endpoint or key set unreachable |

**Server behavior**

1. `POST` to Google's token endpoint with the code, `code_verifier`, `redirect_uri`, and the
   **Android** client ID. This door is Android-only by construction: the browser fallback exists
   because Credential Manager needs Google's services (`research.md` R14), while iOS's SDK already
   runs in the system browser and always returns an ID token. So the request carries no platform
   field and the server needs no selection rule — a code always came from Android. Installed-app
   clients carry no secret, so PKCE is what proves the caller, which is why `codeVerifier` is
   required rather than optional.
2. Verify the returned ID token exactly as `/v1/auth/google` does. Its `aud` is the Android client
   ID, which is already one of the configured audiences.
3. Resolve or create the account and issue a session — the same steps 2 to 4 as above.

The code is single-use: replaying the same one returns `401`, so a captured request cannot mint a
second session.

## `POST /v1/auth/refresh`

Rotate a session. Called by the client's `AccessTokenProvider` — after a `401`, and at launch when
the stored access token is expired or within five minutes of expiring (FR-032, decided by
`RenewSessionUseCase`). Never by feature code. In this feature
the launch renewal is the only thing that calls it in practice, since every other endpoint here is
unauthenticated.

**Request**: `RefreshCredentialsDto`

**Responses**

| Status | Body | When |
| --- | --- | --- |
| `200 OK` | `SessionDto` | Rotated; the old refresh token is now dead |
| `400 Bad Request` | error body | Malformed token value |
| `401 Unauthorized` | error body | Unknown, expired, or already-rotated session |
| `429 Too Many Requests` | error body | Per-IP rate limit exceeded (FR-027) |

**Server behavior**: parse the session id out of the token, then within one transaction match the
stored hash, check `expires_at`, write the rotated hash and `rotated_at`, push `expires_at`
another 90 days out, and issue new tokens. Two concurrent rotations of the same token therefore
produce one `200` and one `401` — never two valid sessions. Moving `expires_at` on every rotation
is what makes the 90-day window sliding, and a successful rotation here is the only event that
renews a session at all (FR-025).

**What signs the user out, and what does not.** Only `400` and `401` are terminal: the server has
answered and refused the session, so the client clears local storage and publishes `LoggedOut`,
returning the user to the login screen (FR-012). Every other outcome — `429`, any `5xx`, a
timeout, or no connection at all — leaves the stored session intact and is retried later; none of
them is an answer about the session, and treating them as one would sign a user out for losing
signal or for sharing an address with a noisy neighbour (SC-014).

## Errors

Failures are translated once, by the `StatusPages` mapping in `services/server/app`. Routes throw
feature failures; they do not build status codes themselves.

That split decides where each half is proven. A `testApplication` inside `feature-auth` assembles
the feature's routes without `app`'s plugins, so it asserts the failure a route raises and the rows
it does or does not write; the mapping from those failures to `400`, `401`, and `503`, and the
`429` the limiter produces, are proven by `app`'s own tests.

The error body is a server-only contract and stays beside the routes — it never enters
`shared/contract/auth`, because the client renders its own Russian copy rather than displaying a
server string (FR-014 forbids exposing provider or transport internals to the user). `429` is not
special-cased on the client: it takes the ordinary failure path, so the user is told the attempt
failed and can retry, not which server rule stopped them.

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

One port, two credential shapes — which one comes back decides which endpoint the repository
calls. Android's implementation tries Credential Manager and falls back to the browser on its
own; iOS always returns an `IdToken`. Nothing above the repository knows which path ran, which is
what keeps FR-029's "the user is never told which one ran" honest rather than aspirational.

Both implementations signal user dismissal by throwing a cancellation the adapter maps to
`LoginOutcome.Cancelled`, keeping FR-013's "cancellation is silent" rule out of the UI layer.
Dismissing the browser tab is that same cancellation, not a failure.

`RenewSessionUseCase` is the second published port, and it exists for the opposite reason —
`app-root` calls it rather than implements it.

```kotlin
// feature-auth/api — public because app-root calls it at launch
fun interface RenewSessionUseCase {

    suspend operator fun invoke()
}
```

No parameter and no result: the caller knows only that the launch decision has been made and the
user is logged in. *Whether* to renew is read from the stored expiry inside `impl` — FR-032's
five-minute margin — because `SessionLocal` is `internal` and `app-root` cannot see it. Every
outcome is already owned there too: a rotation moves the 90-day window, a `400`/`401` clears
storage and publishes `LoggedOut`, and an unreachable server leaves the session untouched.
Returning a result would invite `app-root` to act on one, and there is no action for it to take.
