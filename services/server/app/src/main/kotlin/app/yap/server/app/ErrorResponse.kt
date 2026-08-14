package app.yap.server.app

import kotlinx.serialization.Serializable

@Serializable
internal data class ErrorResponse(val error: String)
