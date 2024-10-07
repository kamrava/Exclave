package io.nekohasekai.sagernet.vpn.models

data class InfoApiResponse(
    val status: String,
    val code: Int,
    val data: UserData
)
