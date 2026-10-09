package app.yap.core.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

/** Emits a tab's root key when its tab is re-tapped while already selected: content scrolls to top. */
interface TabReselects {

    val reselects: Flow<NavKey>
}
