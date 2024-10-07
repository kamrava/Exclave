package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import io.nekohasekai.sagernet.R

class GeneralFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_general, container, false)
        val tvGeneral = view.findViewById<TextView>(R.id.tvGeneral)
        val iconGeneral = view.findViewById<ImageView>(R.id.ivLocationOrderIcon)
        val iconAngle = view.findViewById<ImageView>(R.id.ivConnectionIconAngle)

        tvGeneral.setOnClickListener { showPopupMenu(tvGeneral) }

        iconGeneral.setOnClickListener { showPopupMenu(tvGeneral) }

        // Add click listener to iconAngle
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.location_order_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Geography -> {
                    // Handle Item 1 click and update tvGeneral text
                    updatetvGeneralText("Geography")
                    true
                }

                R.id.Alphabet -> {
                    // Handle Item 2 click and update tvGeneral text
                    updatetvGeneralText("Alphabet")
                    true
                }

                R.id.Latency -> {
                    // Handle Item 3 click and update tvGeneral text
                    updatetvGeneralText("Latency")
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun updatetvGeneralText(text: String) {
        val tvGeneral = view?.findViewById<TextView>(R.id.tvGeneral)
        tvGeneral?.text = text
    }
}
