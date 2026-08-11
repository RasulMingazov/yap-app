# Presentation Stubs

## Context

Presentation tests repeatedly construct `DataState`, `UiState`, `News`, nested presentation values,
and resource selections. Naming them like domain stubs keeps one rule for the whole project.

## Decision

Presentation values follow the value-stub rule from [Test Stubs](003-stubs.md): one
`internal object StubX` per type, in its own file `StubX.kt`, with base builder `stubX`.

```kotlin
internal object StubProfileUiState {

    fun stubProfileUiState(
        email: String = StubUser.EMAIL_VALUE,
        error: ProfileUiState.Error? = null,
        isLoading: Boolean = false,
        name: String = StubUser.NAME,
    ) = ProfileUiState(
        email = email,
        error = error,
        isLoading = isLoading,
        name = name,
    )

    fun stubError(
        message: StringResource = Res.string.profile_error,
    ) = ProfileUiState.Error(message = message)
}
```

- Name the object after the type it builds: `StubProfileUiState`, `StubProfileDataState`,
  `StubProfileNews`. Do not create one `ProfileStubs` object for the whole slice.
- Build nested presentation values, such as `ProfileUiState.Error` or a list row, in the object that
  owns their parent type.
- Include text resources, icon resources or tokens, ordering, visibility, availability, and
  enabled/loading values in builders when they are part of the mapper's observable output.
- Keep `News` payload builders in `Stub<Slice>News`. Do not add a snackbar trigger to a `UiState`
  builder.
- Keep every parameter named and overridable; add named scenario builders with defaults for
  recurring states, such as `stubEmptyUiState()`.
- Keep call behavior in behavioral stubs from [Test Stubs](003-stubs.md), not in these objects.
