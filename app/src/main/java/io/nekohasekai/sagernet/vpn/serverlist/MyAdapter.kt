package io.nekohasekai.sagernet.vpn.serverlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import kotlinx.coroutines.sync.Mutex

class MyAdapter(
    private val itemList: List<ListItem>,
    private val itemClickListener: (ListItem) -> Unit
) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    private var lastExpandedPosition: Int = RecyclerView.NO_POSITION

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        val dropdownList: RecyclerView = itemView.findViewById(R.id.dropdownList)
        val itemHeader: LinearLayout = itemView.findViewById(R.id.itemHeader)
        val selectedView: LinearLayout = itemView.findViewById(R.id.llSelectedView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for the list item
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data for the current item
        val item = itemList[position]

        // Bind basic item information
        holder.itemName.text = item.name

        // Bind expand/collapse state
        val isExpanded = item.isExpanded
        holder.expandIcon.setImageResource(if (isExpanded) R.drawable.ic_minus else R.drawable.ic_plus)
        holder.dropdownList.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.expandIcon.visibility = if (item.isBestServer) View.INVISIBLE else View.VISIBLE

        // Set up click listener for the expand/collapse functionality
        holder.itemHeader.setOnClickListener {
            if (item.isBestServer) {
                holder.selectedView.visibility = View.VISIBLE
                AppRepository.isBestServerSelected = true
                AppRepository.refreshServersListView()
            } else {
                if (lastExpandedPosition != RecyclerView.NO_POSITION && lastExpandedPosition != position) {
                    // Close the last expanded item
                    itemList[lastExpandedPosition].isExpanded = false
                    notifyItemChanged(lastExpandedPosition)
                }

                item.isExpanded = !isExpanded

                // Use notifyItemChanged to refresh the specific item
                notifyItemChanged(position)

                holder.selectedView.visibility = View.INVISIBLE

                // Scroll smoothly to the clicked position
                holder.dropdownList.smoothScrollToPosition(0)
            }

            // Update the last expanded position
            lastExpandedPosition = if (item.isExpanded) position else RecyclerView.NO_POSITION

            // Handle item click by calling the lambda function
            itemClickListener(item)
        }

        // Set visibility of selectedView based on the last expanded position and isBestServer
        if (item.isBestServer && AppRepository.isBestServerSelected) {
            holder.selectedView.visibility = View.VISIBLE
        } else {
            holder.selectedView.visibility = View.INVISIBLE
        }

        // Create and set a RecyclerView adapter for the dropdown list
        val dropdownAdapter = DropdownAdapter(item.dropdownItems) { clickedItem ->
            AppRepository.selectedServerId = clickedItem.id
            DataStore.selectedProxy = clickedItem.id
            if (DataStore.serviceState.connected) {
                SagerNet.reloadService()
            }
        }
        holder.dropdownList.layoutManager = LinearLayoutManager(holder.dropdownList.context)
        holder.dropdownList.adapter = dropdownAdapter
    }

    override fun getItemCount(): Int {
        // Return the number of items in the list
        return itemList.size
    }
}
