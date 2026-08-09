package app.yap.feature.auth.data.time

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun interface CurrentTime {

    fun epochSeconds(): Long
}

@OptIn(ExperimentalTime::class)
internal fun systemCurrentTime(): CurrentTime = CurrentTime { Clock.System.now().epochSeconds }
