package io.nekohasekai.sagernet.vpn.serverlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.getColorAttr
import io.nekohasekai.sagernet.ktx.getColour
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import moe.matsuri.nb4a.Protocols

class DropdownAdapter(
    private val subItems: MutableList<ListSubItem>,
    private val subItemClickListener: (ListSubItem) -> Unit
) : RecyclerView.Adapter<DropdownAdapter.ViewHolder>() {

    private var lastSelectedPosition: Int = RecyclerView.NO_POSITION

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dropdownItemName: TextView = itemView.findViewById(R.id.tvDropdownItemName)
        val subItemLayout: ConstraintLayout = itemView.findViewById(R.id.subItemLayout)
        val selectedView: LinearLayout = itemView.findViewById(R.id.llSelectedView)
        val profileStatus: TextView = itemView.findViewById(R.id.tvProfileStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.dropdown_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subItem = subItems[position]

        holder.dropdownItemName.text = subItem.name

        var profileStatusText: String? = null
        var profileStatusColor = 0

        // Set the profile status text based on the status property
        when (subItem.status) {
            -1 -> {
                profileStatusText = subItem.error
                profileStatusColor = holder.itemView.context.getColorAttr(android.R.attr.textColorSecondary)
            }

            0 -> {
                profileStatusText = holder.itemView.context.getString(R.string.connection_test_testing)
                profileStatusColor = holder.itemView.context.getColour(R.color.material_light_white)
            }

            1 -> {
                profileStatusText = holder.itemView.context.getString(R.string.available, subItem.ping)
                profileStatusColor = if (subItem.ping <= 600) {
                    holder.itemView.context.getColour(R.color.material_green_500)
                } else {
                    holder.itemView.context.getColour(R.color.material_red_500)
                }
            }

            2 -> {
                profileStatusText = subItem.error
                profileStatusColor = holder.itemView.context.getColour(R.color.material_red_500)
            }

            3 -> {
                val err = subItem.error ?: ""
                val msg = Protocols.genFriendlyMsg(err)
                profileStatusText = if (msg != err) msg else holder.itemView.context.getString(R.string.unavailable)
                profileStatusColor = holder.itemView.context.getColour(R.color.material_red_500)
            }
        }

        // Set the profile status text and color
        holder.profileStatus.text = profileStatusText
        holder.profileStatus.setTextColor(profileStatusColor)

        // Set up click listener for the expand/collapse functionality
        holder.subItemLayout.setOnClickListener {
            // Update the visibility of selectedView for the last selected item
            if (lastSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(lastSelectedPosition)
            }

            // Toggle the visibility of selectedView based on isSelected
            holder.selectedView.visibility = View.VISIBLE
            AppRepository.resetAllSubItemsStatus()
            subItem.isSelected = true

            // Update the last selected position
            lastSelectedPosition = holder.adapterPosition
            // Handle item click by calling the lambda function
            subItemClickListener(subItem)

            AppRepository.isBestServerSelected = false
//            AppRepository.refreshServersListView()
        }
        subItem.isSelected = false
        // Set the initial visibility of selectedView
        if (lastSelectedPosition == position) {
            holder.selectedView.visibility = View.VISIBLE
        } else {
            holder.selectedView.visibility = View.INVISIBLE
        }
        if(AppRepository.isBestServerSelected) {
            holder.selectedView.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return subItems.size
    }
}
