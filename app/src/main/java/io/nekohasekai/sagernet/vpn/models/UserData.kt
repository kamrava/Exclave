package io.nekohasekai.sagernet.vpn.models

data class UserData(
    val uid: Int,
    val username: String,
    val email: String,
    val money: String,
    val services: List<Service>,
    val count: Int,
    val xmp_token: String?,
    val token: String?
)