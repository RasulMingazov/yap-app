package app.yap.core.common.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

class IosMotionPreferences : MotionPreferences {

    override fun isReduced(): Boolean = UIAccessibilityIsReduceMotionEnabled()
}
