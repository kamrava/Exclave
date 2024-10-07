package io.nekohasekai.sagernet.vpn.serverlist

data class ListSubItem(
    var id: Long,
    var serverId: Int,
    var name: String,
    var status: Int,
    var error: String?,  // Add error property
    var ping: Int,
    var isSelected: Boolean = false,
    var isFree: Boolean = false,
    var config: String = "",
    var tags: Array<String> = arrayOf(),
) {
    // Click listener function that can be set from outside the class
    private var clickListener: (() -> Unit)? = null

    // Function to set the click listener
    fun setOnClickListener(listener: () -> Unit) {
        clickListener = listener
    }

    // Function to handle the click event
    fun performClick() {
        clickListener?.invoke()
    }
}

