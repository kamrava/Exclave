package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.databinding.FragmentCommentBinding

class CommentFragment : Fragment() {

    private var _binding: FragmentCommentBinding? = null // Backing property for binding
    private val binding get() = _binding!! // Non-nullable property for binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewBinding
        _binding = FragmentCommentBinding.inflate(inflater, container, false)

        // Set up click listeners
        setupClickListeners()

        return binding.root
    }

    private fun setupClickListeners() {
        // Add click listener to iconAngle
        binding.ivCommentIconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnSend.setOnClickListener {
            val titleText = binding.tvTitleBox.text.toString().trim()
            val messageText = binding.tvMessageBox.text.toString().trim()

            if (titleText.isNotEmpty() && messageText.isNotEmpty()) {
                // Both title and message are not empty, proceed with your logic
                Toast.makeText(context, "Your comment has been submitted", Toast.LENGTH_LONG).show()
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                // Show a toast or handle the case where either title or message is empty
                Toast.makeText(context, "Please fill in both title and message", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference to prevent memory leaks
    }
}
