package app.yap.core.common.platform

import android.content.Context
import android.provider.Settings

class AndroidMotionPreferences(
    private val context: Context,
) : MotionPreferences {

    override fun isReduced(): Boolean = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        DEFAULT_ANIMATOR_DURATION_SCALE,
    ) == 0f

    private companion object {
        const val DEFAULT_ANIMATOR_DURATION_SCALE = 1f
    }
}
