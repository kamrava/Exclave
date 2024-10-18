package io.nekohasekai.sagernet.vpn.models

data class AppSetting(
    val baseUrl: String,
    val userLoginUrl: String,
    val userResetPasswordUrl: String,
    val getUserInfoUrl: String,
    val userRegisterUrl: String,
    val verifiedUserRegisterUrl: String,
    val userCheckEmailAvailability: String,
    val userStateUrl: String,
    val urlTest: UrlData,
    val panelApiHeaderToken: String,
    val versionCode: Int,
    val forceUnder: Int,
    val freeVpnTimer: Int,
)

data class UrlData(
    val link: String,
    val timeout: Int
)