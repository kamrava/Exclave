package io.nekohasekai.sagernet.vpn.serverlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ListItemBinding
import io.nekohasekai.sagernet.vpn.models.ListItem
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.services.VpnService

class ListItemAdapter(
    private val itemList: MutableList<ListItem>,
    private val itemClickListener: (ListItem) -> Unit
) : RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

    // ViewHolder using ViewBinding for better readability and performance
    inner class ViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListItem) {
            // Bind data to views
            binding.itemName.text = item.name
            binding.expandIcon.setImageResource(if (item.isExpanded) R.drawable.ic_minus else R.drawable.ic_plus)
            binding.dropdownList.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.llSelectedView.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE

            // Manage the best server visibility logic
            binding.expandIcon.visibility = if (item.isBestServer) View.INVISIBLE else View.VISIBLE

            // Dropdown list setup
            // Pass adapterPosition to setupDropdownList
            setupDropdownList(binding, item, adapterPosition)

            // Handle click for expansion and selection
            binding.itemHeader.setOnClickListener {
                if (item.isBestServer) {
                    resetAllSubItems(-1L)
                    // Update the state
                    item.isSelected = true
                    AppRepository.isBestServerSelected = true

                    // Notify that this item has changed so the UI updates
                    // notifyItemChanged(adapterPosition)
                    notifyDataSetChanged()

                    // Perform any other logic like connecting to VPN
                    VpnService.startVpnFromProfile(item.id)
                } else {
                    // Handle non-best-server items
                    toggleExpansion(item)
                }

                itemClickListener(item)
            }
        }

        // Method to handle dropdown list setup
        private fun setupDropdownList(binding: ListItemBinding, item: ListItem, position: Int) {
            val dropdownAdapter = ListSubItemAdapter(item.dropdownItems) { subItem ->
                // Handle sub-item click event
                resetAllSubItems(subItem.id)  // Reset all sub-items across all list items
                resetBestServer() // Reset best server selection in UI
                notifyDataSetChanged()  // Notify adapter to refresh the UI

                // Perform additional logic for the clicked sub-item
                VpnService.startVpnFromProfile(subItem.id)
            }

            binding.dropdownList.layoutManager = LinearLayoutManager(binding.root.context)
            binding.dropdownList.adapter = dropdownAdapter
        }
    }

    // Reset all sub-items to make their llSelectedView visibility invisible
    private fun resetAllSubItems(exceptProfileId: Long) {
        for (listItem in itemList) {
            for (subItem in listItem.dropdownItems) {
                if (subItem.id != exceptProfileId) {
                    subItem.isSelected = false
                }
            }
        }
    }

    // Reset best server to make its llSelectedView visibility invisible
    private fun resetBestServer() {
        itemList[0].isSelected = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the view using ViewBinding
        val binding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind the data to the ViewHolder
        val item = itemList[position]
        holder.bind(item)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val item = itemList[position]
            when (payloads[0]) {
                "EXPAND" -> {
                    // Only update expansion state
                    holder.binding.dropdownList.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
                    holder.binding.expandIcon.setImageResource(if (item.isExpanded) R.drawable.ic_minus else R.drawable.ic_plus)
                }
                "SELECTED_VIEW" -> {
                    // Update selected view visibility
                    holder.binding.llSelectedView.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
                }
            }
        } else {
            // Fallback to default binding if no payloads
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    private fun toggleExpansion(item: ListItem) {
        // Toggle the expanded state of the item
        item.isExpanded = !item.isExpanded
        item.isSelected = false

        // Notify the adapter that the item has changed to refresh the UI
        notifyItemChanged(itemList.indexOf(item)) // items is your list of ListItem
    }
}

