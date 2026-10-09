package app.yap.core.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

interface TabReselects {

    val reselects: Flow<NavKey>
}
