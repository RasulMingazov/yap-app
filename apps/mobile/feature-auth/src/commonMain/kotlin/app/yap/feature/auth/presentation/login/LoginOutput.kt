package app.yap.feature.auth.presentation.login

sealed interface LoginOutput {

    data object OpenProviderSelection : LoginOutput
}
