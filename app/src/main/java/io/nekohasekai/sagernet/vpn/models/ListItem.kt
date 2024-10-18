package io.nekohasekai.sagernet.vpn.models

data class ListItem(
    val id: Long = -1,
    val name: String,
    val countryCode: String,
    val dropdownItems: MutableList<ListSubItem>,
    var isExpanded: Boolean = false,
    var isBestServer: Boolean = false,
    var isSelected: Boolean = false,
    var pointToIndex: Int = -1
)
