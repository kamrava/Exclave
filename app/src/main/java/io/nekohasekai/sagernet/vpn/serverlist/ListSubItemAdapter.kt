package io.nekohasekai.sagernet.vpn.serverlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.DropdownItemBinding
import io.nekohasekai.sagernet.ktx.getColorAttr
import io.nekohasekai.sagernet.ktx.getColour
import io.nekohasekai.sagernet.vpn.models.ListSubItem
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class ListSubItemAdapter(
    private val subItems: MutableList<ListSubItem>,
    private val subItemClickListener: (ListSubItem) -> Unit
) : RecyclerView.Adapter<ListSubItemAdapter.ViewHolder>() {

    private var lastSelectedPosition: Int = RecyclerView.NO_POSITION

    // ViewHolder using ViewBinding for better readability and performance
    inner class ViewHolder(private val binding: DropdownItemBinding) : RecyclerView.ViewHolder(binding.root) {

        // Bind data to views
        fun bind(subItem: ListSubItem) {
            binding.tvDropdownItemName.text = subItem.name

            // Set profile status and color
            val profileStatusInfo = getProfileStatus(subItem)
            binding.tvProfileStatus.text = profileStatusInfo.first
            binding.tvProfileStatus.setTextColor(profileStatusInfo.second)

            // Set visibility of selected view based on the selected state
            binding.llSelectedView.visibility = if (subItem.isSelected) View.VISIBLE else View.INVISIBLE

            // Handle item clicks
            binding.subItemLayout.setOnClickListener {
                handleItemClick(subItem)
            }
        }

        // Handle profile status logic
        private fun getProfileStatus(subItem: ListSubItem): Pair<String, Int> {
            val context = binding.root.context
            return when (subItem.status) {
                -1 -> Pair(subItem.error ?: "", context.getColorAttr(android.R.attr.textColorSecondary))
                0 -> Pair(context.getString(R.string.connection_test_testing), context.getColour(R.color.material_light_white))
                1 -> {
                    val statusText = context.getString(R.string.available, subItem.ping)
                    val statusColor = when {
                        subItem.ping <= 300 -> context.getColour(R.color.material_green_500)
                        subItem.ping <= 600 -> context.getColour(R.color.material_orange_500)
                        else -> context.getColour(R.color.material_red_500)
                    }
                    Pair(statusText, statusColor)
                }
                2 -> Pair(subItem.error ?: "", context.getColour(R.color.material_red_500))
                3 -> Pair(context.getString(R.string.unavailable), context.getColour(R.color.material_red_500))
                else -> Pair("", context.getColorAttr(android.R.attr.textColorSecondary))
            }
        }

        // Handle the item click logic
        private fun handleItemClick(subItem: ListSubItem) {
            // Update the last selected item
            if (lastSelectedPosition != RecyclerView.NO_POSITION) {
                val previousSelectedItem = subItems[lastSelectedPosition]
                previousSelectedItem.isSelected = false
                notifyItemChanged(lastSelectedPosition)
            }

            // Mark the current item as selected
            subItem.isSelected = true
            notifyItemChanged(adapterPosition)

            // Update the last selected position
            lastSelectedPosition = adapterPosition

            // Perform additional logic on item click
            AppRepository.resetAllSubItemsStatus()
            subItemClickListener(subItem)

            // Deselect the best server mode
            AppRepository.isBestServerSelected = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DropdownItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subItem = subItems[position]
        holder.bind(subItem)
    }

    override fun getItemCount(): Int = subItems.size
}

