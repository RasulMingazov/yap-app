# Presentation Stubs

## Context

Presentation tests repeatedly construct `DataState`, `UiState`, nested UI values, and resource selections. Keeping them together makes mapper and model expectations easier to read.

## Decision

Create one `internal object SliceStubs` beside the presentation tests:

```kotlin
internal object ProfileStubs {

    fun stubDataState(
        user: User?,
        isLoading: Boolean,
        hasError: Boolean,
    ) = DataState(
        user = user,
        isLoading = isLoading,
        hasError = hasError,
    )

    fun stubUiState(
        name: String,
        email: String,
        isLoading: Boolean,
        error: UiState.Error?,
    ) = UiState(
        name = name,
        email = email,
        isLoading = isLoading,
        error = error,
    )

    fun stubContentUiState(
        name: String = StubUser.NAME,
        email: String = StubUser.EMAIL_VALUE,
        error: UiState.Error? = null,
    ) = stubUiState(
        name = name,
        email = email,
        isLoading = false,
        error = error,
    )

    fun stubError(
        message: StringResource = Res.string.profile_error,
    ) = UiState.Error(message = message)
}
```

- Name the object after the presentation slice, such as `ProfileStubs` or `SignInStubs`.
- Keep `DataState`, `UiState`, nested UI values, and presentation resources for that slice in the same object.
- Use base builders with explicit fields when a test must describe the whole mapping input or output.
- Add named scenario builders with defaults for recurring states, such as `stubContentUiState()`.
- Keep every parameter named and overridable.
- Keep call behavior in behavioral stubs from [Test Stubs](003-test-stubs.md), not in this object.
