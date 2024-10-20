package io.nekohasekai.sagernet.vpn.nav

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.WelcomeActivity
import io.nekohasekai.sagernet.vpn.models.Service
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository

class AccountFragment : Fragment() {
    private var isFirstSelection = true
    private lateinit var adapter: ArrayAdapter<Service>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        val iconAngle = view.findViewById<ImageView>(R.id.ivAccountIconAngle)
        val exitAccountButton = view.findViewById<AppCompatButton>(R.id.btnExitAccount)


        // iconAngle click listener
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        // set user's email
        val userEmail = AuthRepository.getUserEmail()
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        tvEmail.text = userEmail

        // ExitAccountButton
        exitAccountButton.setOnClickListener {
//            SocialAuthRepository.facebookLoginManager.logOut()
//            SocialAuthRepository.firebaseAuth.signOut()
//            SocialAuthRepository.googleSignInClient.signOut()
            AuthRepository.clearUserInfo()

            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        val activeServices = AuthRepository.getUserActiveServices()
        val spinner: Spinner = view.findViewById(R.id.serviceSelector)
        var selectedPosition = 0
        selectedPosition = activeServices.indexOfFirst { it.sid == AuthRepository.getSelectedService()?.sid }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (isFirstSelection) {
                    isFirstSelection = false
                } else {
                    val selectedService = parent.getItemAtPosition(position) as Service
                    AuthRepository.setSelectedService(selectedService)
                    Toast.makeText(requireContext(), getString(R.string.service_activated_successfully, selectedService.packageName), Toast.LENGTH_LONG).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do something when no item is selected
            }
        }

        adapter = object : ArrayAdapter<Service>(requireContext(), R.layout.spinner_item, activeServices) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val dropView = super.getView(position, convertView, parent) as TextView
                val itemName = getString(R.string.service_name, getItem(position)?.packageName, getItem(position)?.sid)
                dropView.text = itemName
                return dropView
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val dropItemView = super.getDropDownView(position, convertView, parent) as TextView
                val itemName = getString(R.string.service_name, getItem(position)?.packageName, getItem(position)?.sid)
                dropItemView.text = itemName
                return dropItemView
            }
        }
        adapter.setDropDownViewResource(R.layout.spinner_each_item)
        spinner.adapter = adapter
        spinner.setSelection(selectedPosition)

        return view
    }
}