package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import io.nekohasekai.sagernet.R
class CommentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)
        val iconAngle = view.findViewById<ImageView>(R.id.ivCommentIconAngle)
        val btnSend = view.findViewById<AppCompatButton>(R.id.btnSend)
        val tvTitleBox = view.findViewById<EditText>(R.id.tvTitleBox)
        val tvMessageBox = view.findViewById<EditText>(R.id.tvMessageBox)

        // Add click listener to iconAngle
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        btnSend.setOnClickListener {
            val titleText = tvTitleBox.text.toString().trim()
            val messageText = tvMessageBox.text.toString().trim()

            if (titleText.isNotEmpty() && messageText.isNotEmpty()) {
                // Both title and message are not empty, proceed with your logic
                Toast.makeText(context, "Your comment has been submitted", Toast.LENGTH_LONG).show()
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                // Show a toast or handle the case where either title or message is empty
                Toast.makeText(context, "Please fill in both title and message", Toast.LENGTH_LONG).show()
            }
        }

        return view
    }
}
