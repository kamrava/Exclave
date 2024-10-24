package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.FragmentGeneralBinding

class GeneralFragment : Fragment() {

    private var _binding: FragmentGeneralBinding? = null // Backing property for binding
    private val binding get() = _binding!! // Non-nullable property for binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneralBinding.inflate(inflater, container, false)

        setupClickListeners()

        return binding.root
    }

    private fun setupClickListeners() {
        // Access views through binding
        binding.tvGeneral.setOnClickListener { showPopupMenu(binding.tvGeneral) }

        binding.ivLocationOrderIcon.setOnClickListener { showPopupMenu(binding.tvGeneral) }

        // Add click listener to iconAngle
        binding.ivConnectionIconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.location_order_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.Geography -> {
                    // Handle Item 1 click and update tvGeneral text
                    updateTvGeneralText("Geography")
                    true
                }
                R.id.Alphabet -> {
                    // Handle Item 2 click and update tvGeneral text
                    updateTvGeneralText("Alphabet")
                    true
                }
                R.id.Latency -> {
                    // Handle Item 3 click and update tvGeneral text
                    updateTvGeneralText("Latency")
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun updateTvGeneralText(text: String) {
        binding.tvGeneral.text = text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference to prevent memory leaks
    }
}
