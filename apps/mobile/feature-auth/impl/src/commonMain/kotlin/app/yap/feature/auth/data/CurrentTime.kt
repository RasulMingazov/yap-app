package app.yap.feature.auth.data

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun interface CurrentTime {

    fun epochSeconds(): Long
}

@OptIn(ExperimentalTime::class)
internal class SystemCurrentTime : CurrentTime {

    override fun epochSeconds(): Long = Clock.System.now().epochSeconds
}
