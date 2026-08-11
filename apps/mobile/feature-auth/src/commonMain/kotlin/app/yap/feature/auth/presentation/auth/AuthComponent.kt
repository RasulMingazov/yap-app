package app.yap.feature.auth.presentation.auth

import app.yap.feature.auth.presentation.login.LoginComponent
import app.yap.feature.auth.presentation.selectprovider.SelectProviderComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value

/**
 * Root of the authentication feature and the single owner of the relationship between its two
 * screens: [login] is a permanent child and [selectProvider] is the navigation primitive that
 * holds the modal provider sheet (R-087, R-090, R-092, R-093).
 */
interface AuthComponent {

    val login: LoginComponent
    val selectProvider: Value<ChildSlot<*, SelectProviderComponent>>

    interface Factory {

        operator fun invoke(componentContext: ComponentContext): AuthComponent
    }
}
