package io.nekohasekai.sagernet.vpn.models

data class ListSubItem(
    val id: Long,
    val serverId: Int,
    val name: String,
    var status: Int,
    var error: String? = "",
    var ping: Int = -1,
    var isSelected: Boolean = false,
    var isFree: Boolean = false,
    var config: String = "",
    var tags: Array<String> = arrayOf(),
    val profileIndex: Int
)
