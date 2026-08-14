package app.yap.core.common.platform

import android.app.Activity

class ActivityProvider {

    private var resumedActivity: Activity? = null

    fun current(): Activity? = resumedActivity

    fun onActivityResumed(activity: Activity) {
        resumedActivity = activity
    }

    fun onActivityPaused(activity: Activity) {
        if (resumedActivity === activity) {
            resumedActivity = null
        }
    }
}
