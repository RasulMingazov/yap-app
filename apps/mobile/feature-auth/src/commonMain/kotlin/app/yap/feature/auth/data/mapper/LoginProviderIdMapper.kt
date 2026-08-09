package app.yap.feature.auth.data.mapper

import app.yap.feature.auth.domain.entity.LoginProviderId

private const val APPLE = "apple"
private const val GOOGLE = "google"
private const val TID = "tid"

internal fun LoginProviderId.toWireValue(): String = when (this) {
    LoginProviderId.Apple -> APPLE
    LoginProviderId.Google -> GOOGLE
    LoginProviderId.Tid -> TID
}
