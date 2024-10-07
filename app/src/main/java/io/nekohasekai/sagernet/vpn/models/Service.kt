package io.nekohasekai.sagernet.vpn.models

import com.google.gson.annotations.SerializedName

data class Service(
    val sid: Int,
    val userid: Int,
    val email: String,
    val packageid: Int,
    @SerializedName("package") val packageName: String,
    val paid_date: String,
    val expire_date: String,
    val billing: String,
    val amount: String,
    val iplimit: String,
    val speedlimit: String,
    val server_group: String,
    val traffic: String,
    val used_traffic: String,
    val today: String,
    val recent_omline: String,
    val status: String,
    val reset_traffic: String,
    val sublink: String
)
