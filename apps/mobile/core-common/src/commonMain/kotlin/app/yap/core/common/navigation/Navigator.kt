package app.yap.core.common.navigation

import androidx.navigation3.runtime.NavKey

interface Navigator {

    fun navigate(key: NavKey)

    fun back()
}
