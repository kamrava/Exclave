package io.nekohasekai.sagernet.vpn.serverlist

data class ListItem(var name: String, val dropdownItems: MutableList<ListSubItem>, var isExpanded: Boolean = false, var iconResId: Int, var isBestServer: Boolean = false, var id: Long = -1, var isSelected: Boolean = false)
{
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

