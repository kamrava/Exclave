package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityPremiumBinding
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import io.nekohasekai.sagernet.vpn.repositories.PremiumServicesRepository
import kotlinx.coroutines.launch

class PremiumActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivIconAngle.setOnClickListener { navigateToDashboard() }

        // Fetch service data and set up spinners
        lifecycleScope.launch {
            PremiumServicesRepository.fetchServiceData()
            setupServiceSelector()
        }
    }

    private fun setupServiceSelector() {
        val serviceNames = PremiumServicesRepository.services.map { it.name }

        binding.spServiceSelector.adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            serviceNames
        )

        binding.spServiceSelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val selectedService = PremiumServicesRepository.services[position]
                    setupServiceItems(selectedService)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    binding.tvShowInfo.visibility = View.GONE
                    binding.clShowSelectedEmail.visibility = View.GONE
                    binding.clShowInvoice.visibility = View.GONE
                    binding.clShowSelectedPlan.visibility = View.GONE
                }
            }
    }

    private fun setupServiceItems(service: PremiumServicesRepository.ServiceData) {
        binding.serviceSubItem.adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            service.items
        )

        binding.serviceSubItem.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val selectedPlan = parent.getItemAtPosition(position).toString()
                    val userEmail = AuthRepository.getUserEmail()

                    binding.tvShowSelectedEmail.text = userEmail
                    binding.tvShowSelectedPlan.text = selectedPlan
                    binding.tvShowInvoice.text = service.prices[selectedPlan]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}
