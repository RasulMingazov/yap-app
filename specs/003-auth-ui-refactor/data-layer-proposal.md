# Proposal: simplifying the auth data layer

**Status**: awaiting approval. No file under
`apps/mobile/feature-auth/impl/src/*/kotlin/app/yap/feature/auth/data/` changes before it is
approved (FR-022, SC-008).

**Method**: `IMPL/data/` and the one domain port it implements were read in full. No research item
covers this layer, so every candidate below names what changes, why the current form is excessive,
what the result looks like, and what could go wrong.

## Summary

Seven candidates were reviewed. **Three are worth doing**, **two are worth doing only together with
a named follow-up**, and **two should be left alone** — the reasoning for leaving them is recorded
so the question is not reopened by accident.

| # | Candidate | Verdict |
| --- | --- | --- |
| 1 | `AuthRepository.loginWithGoogle()` is provider-specific | **Change** — rename to `login()` |
| 2 | `CurrentTime` port | **Keep** |
| 3 | `NonceGenerator` port | **Change** — fold into the repository's collaborator list as a function type |
| 4 | `SessionLocal` duplicates `SessionDto` | **Keep** |
| 5 | `AuthStateSource` | **Change** — move behind the repository |
| 6 | The `hasReadStorage` / `readMutex` pair | **Change with follow-up** |
| 7 | The `Lazy<AuthRemoteDataSource>` cycle break | **Change with follow-up** |

---

## 1. `AuthRepository.loginWithGoogle()` names one provider — **change**

**What changes**: `loginWithGoogle()` becomes `login()` on
`domain/repository/AuthRepository.kt` and `data/repository/DefaultAuthRepository.kt`.
`GoogleProviderLogin` is the only caller.

**Why the current form is excessive**: after this feature the provider is chosen by
`DefaultLoginUseCase` and served by a `ProviderLogin`. A repository method that names Google
re-states, at the wrong layer, a decision the registry already made — and a second provider would
either add a second near-identical method or reuse a misnamed one. The private
`requestSession(nonce)` already dispatches on the credential shape, so the Google-ness is an
implementation detail, not a contract.

**Resulting structure**:

```kotlin
internal interface AuthRepository {
    fun observe(): Flow<AuthState>
    suspend fun login(): LoginOutcome
    suspend fun accessTokenLifetimeSeconds(): Long?
    suspend fun renewSession()
}
```

**Risks**: none beyond a rename; `StubAuthRepository`, `DefaultAuthRepositoryTest`, and
`GoogleProviderLoginTest` follow it. The name will need revisiting when a second provider ships a
repository-backed path — at that point `login()` takes the credential source as a parameter. Not
now: that abstraction would own no behaviour yet.

## 2. `CurrentTime` — **keep**

**Why not**: it looks like a single-implementation port, but its purpose is testability of expiry,
and it *is* substituted — `DefaultAuthRepositoryTest` and `DefaultAccessTokenProviderTest` drive
expiry through it. `Clock.System` is not injectable otherwise, and expiry arithmetic is exactly the
kind of rule that must be tested without wall-clock sleeps. Removing it would trade a five-line
interface for untestable behaviour.

## 3. `NonceGenerator` — **change**

**What changes**: `data/identity/NonceGenerator.kt` loses its `fun interface` and keeps only
`RandomNonceGenerator`, injected as `NonceGenerator` → replaced by a `() -> String` parameter named
`nonce` on `DefaultAuthRepository`, bound in Koin as `{ RandomNonceGenerator()::generate }`.

**Why the current form is excessive**: the interface has one method, one production implementation,
and one call site, and its only other purpose is to be replaced in tests — which a function type
does just as well. `docs/mobile/002-domain.md` allows a second implementation "only for a real
substitution boundary"; a nonce generator has none.

**Resulting structure**: `RandomNonceGenerator` stays a class (it owns the hex encoding and is
worth its own test), but nothing declares an interface for it.

**Risks**: low. A function-typed dependency is slightly less self-describing at the injection site;
the parameter name carries that. If a second nonce strategy ever appears (a platform secure random,
say), the interface comes back — one file, no callers changed.

## 4. `SessionLocal` and `SessionDto` are field-identical — **keep**

**Why not**: they are identical *today*, which is the argument for keeping them apart, not for
merging them. Constitution Principle IV is explicit: a DTO is not a storage model, and mapping
happens at the repository boundary. Collapsing them would make the on-device storage format a
function of the server's wire format, so a server field rename would silently invalidate every
stored session on every installed device. `SessionMapper.toLocal()` is four lines; that is the price
of the boundary, and it is the right price.

`SessionMapper.toDomain()` is a different matter — it decodes a JWT payload to read `sub`. That is
real behaviour with a real test, and it stays.

## 5. `AuthStateSource` — **change**

**What changes**: `data/AuthStateSource.kt` stops being a separately-bound Koin singleton shared by
`DefaultAuthRepository` and `DefaultAccessTokenProvider`. `DefaultAuthRepository` owns it, and
`DefaultAccessTokenProvider` reports through a narrow callback it is given
(`onSessionChanged: (AuthState) -> Unit`) rather than reaching for shared mutable state.

**Why the current form is excessive**: two classes hold a reference to the same mutable publisher
and both write to it, which makes "who decides the auth state" unanswerable from either file. The
repository is the layer that owns that answer.

**Resulting structure**: one owner, one writer per concern, and the same observable behaviour.

**Risks**: moderate — this is the one candidate that touches session-renewal ordering. It lands
test-first, and `DefaultAccessTokenProviderTest`, `DefaultAuthRepositoryTest`, and
`DefaultRenewSessionUseCaseTest` all run before and after.

## 6. `hasReadStorage` plus `readMutex` — **change, with a follow-up**

**What changes**: the "read storage exactly once" guard becomes a lazily-started
`kotlinx.coroutines` primitive rather than a hand-rolled flag-plus-mutex, so the first-read result
is memoised rather than re-derived, and `store()` no longer has to reach in and set the flag.

**Why the current form is excessive**: `hasReadStorage` is a plain `var` written from two places —
`store()` without the mutex and `resolveFromStorage()` inside it. The unsynchronised write in
`store()` is not currently a bug (the flag is only ever set to `true`, and the state is published
separately), but it is a data race by construction, and the pairing hides the fact that the guard
memoises nothing: a caller that arrives during the first read waits, then re-checks a boolean.

**Resulting structure**: one `suspend` memo, no flag, no mutex, and `store()` invalidating it
explicitly instead of poking a boolean.

**Follow-up required**: the current `observe()` emits `authStateSource.authState.value` *before*
resolving storage, so the first emission is `Unknown` by design and `RootBackStack` renders nothing
until the second. That contract must be preserved exactly, and it is not currently asserted. The
change lands with a test that pins it.

## 7. `Lazy<AuthRemoteDataSource>` in `DefaultAccessTokenProvider` — **change, with a follow-up**

**What changes**: the `Lazy` wrapper is removed by breaking the cycle where it is actually created —
in `coreNetworkModule`, which installs the access-token modifier on the client it has just built,
so `NetworkClient` → `AccessTokenProvider` → `AuthRemoteDataSource` → `NetworkClient` closes only
through Koin's own resolution order.

**Why the current form is excessive**: `Lazy<T>` as a constructor parameter is a dependency-graph
workaround leaking into a class's signature. Nothing about `DefaultAccessTokenProvider`'s behaviour
is lazy; the laziness exists to defer one Koin lookup.

**Resulting structure**: `DefaultAccessTokenProvider(authRemoteDataSource: AuthRemoteDataSource, …)`,
with the deferral expressed once, in the module that owns the cycle.

**Follow-up required**: this is the only candidate that reaches outside `feature-auth` — it touches
`core-network`'s module. If that turns out to widen the change beyond this feature, the fallback is
to keep `Lazy` and record why in a comment rather than leave the reason implicit. **This is the one
item to drop first if the change is to stay small.**

---

## What is out of scope

- `AuthRemoteDataSource` and its `AuthRemoteFailure` translation: this is the adapter boundary doing
  exactly its job — Ktor and status codes stop here, and two named failures cross. No change.
- `SessionStorage` and its two platform implementations: untouched by this feature; the Android
  Keystore path in particular is not something to refactor without a device test.
- `AndroidGoogleCredentialProvider`, `CredentialRequester`, `GoogleBrowserAuthFlow`: platform
  adapters with their own tests, and the fallback chain between them is real behaviour.

## Order of work, once approved

1. Candidate 1 (rename) — no behaviour change, no test of its own.
2. Candidate 3 (nonce) — no behaviour change.
3. Candidate 6 (first-read memo) — test-first, pins the `Unknown`-first contract.
4. Candidate 5 (state ownership) — test-first.
5. Candidate 7 (cycle) — last, and droppable.
6. Run `TEST/data/` and `TEST/domain/` in full and confirm login, session renewal, and
   access-token behaviour are unchanged.
