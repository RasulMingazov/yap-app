package app.yap.contract.common

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(val error: String)
