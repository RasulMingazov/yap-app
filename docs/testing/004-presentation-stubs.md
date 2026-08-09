# Presentation Stubs

## Context

Presentation tests repeatedly construct `Model.DataState`, `Component.UiState`,
`Component.News`, nested presentation values, and resource selections. Keeping them together
makes mapper and model expectations easier to read.

## Decision

Create one `internal object SliceStubs` beside the presentation tests:

```kotlin
internal object ProfileStubs {

    fun stubDataState(
        hasError: Boolean,
        isLoading: Boolean,
        user: User?,
    ) = ProfileModel.DataState(
        hasError = hasError,
        isLoading = isLoading,
        user = user,
    )

    fun stubUiState(
        email: String,
        error: ProfileComponent.UiState.Error?,
        isLoading: Boolean,
        name: String,
    ) = ProfileComponent.UiState(
        email = email,
        error = error,
        isLoading = isLoading,
        name = name,
    )

    fun stubContentUiState(
        email: String = StubUser.EMAIL_VALUE,
        error: ProfileComponent.UiState.Error? = null,
        name: String = StubUser.NAME,
    ) = stubUiState(
        email = email,
        error = error,
        isLoading = false,
        name = name,
    )

    fun stubError(
        message: StringResource = Res.string.profile_error,
    ) = ProfileComponent.UiState.Error(message = message)

    fun stubSnackbarNews(
        message: StringResource = Res.string.profile_error,
    ) = ProfileComponent.News.ShowSnackbar(message = message)
}
```

- Name the object after the presentation slice, such as `ProfileStubs` or `LoginStubs`.
- Keep `Model.DataState`, `Component.UiState`, `Component.News`, nested presentation values, and
  presentation resources for that slice in the same object.
- Include text resources, icon resources or tokens, ordering, visibility, availability, and
  enabled/loading values in builders when they are part of the mapper's observable output.
- Add dedicated builders for one-shot `News` payloads, including snackbar messages. Do not add a
  snackbar trigger to a `UiState` builder.
- Use base builders with explicit fields when a test must describe the whole mapping input or output.
- Add named scenario builders with defaults for recurring states, such as `stubContentUiState()`.
- Keep every parameter named and overridable.
- Keep call behavior in behavioral stubs from [Test Stubs](003-stubs.md), not in this object.
