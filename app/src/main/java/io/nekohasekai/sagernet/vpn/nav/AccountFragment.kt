package io.nekohasekai.sagernet.vpn.nav

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.FragmentAccountBinding
import io.nekohasekai.sagernet.vpn.WelcomeActivity
import io.nekohasekai.sagernet.vpn.models.Service
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository

class AccountFragment : Fragment() {
    private var isFirstSelection = true
    private lateinit var adapter: ArrayAdapter<Service>
    private var _binding: FragmentAccountBinding? = null // Backing property for binding
    private val binding get() = _binding!! // Non-nullable property for binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewBinding
        _binding = FragmentAccountBinding.inflate(inflater, container, false)

        // Set up UI elements and listeners
        setupUI()

        return binding.root
    }

    private fun setupUI() {
        // Set up click listener for iconAngle
        binding.ivAccountIconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set user's email
        val userEmail = AuthRepository.getUserEmail()
        binding.tvEmail.text = userEmail

        // Set up exitAccountButton listener
        binding.btnExitAccount.setOnClickListener {
            AuthRepository.clearUserInfo()
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        // Set up Spinner for active services
        val activeServices = AuthRepository.getUserActiveServices()
        val selectedPosition =
            activeServices.indexOfFirst { it.sid == AuthRepository.getSelectedService()?.sid }
        setupSpinner(activeServices, selectedPosition)
    }

    private fun setupSpinner(activeServices: List<Service>, selectedPosition: Int) {
        binding.serviceSelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
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
        binding.serviceSelector.adapter = adapter
        binding.serviceSelector.setSelection(selectedPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference to prevent memory leaks
    }
}
