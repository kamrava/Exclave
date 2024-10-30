package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityPremiumBinding
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import io.nekohasekai.sagernet.vpn.repositories.PremiumServicesRepository

class PremiumActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivIconAngle.setOnClickListener { navigateToDashboard() }

        binding.spServiceSelector.adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            listOf(PremiumServicesRepository.GOLDEN_SERVICE, PremiumServicesRepository.TITANIUM_SERVICE)
        )

        binding.spServiceSelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val selectedService = parent.getItemAtPosition(position).toString()
                    val items = if (selectedService == PremiumServicesRepository.GOLDEN_SERVICE) {
                        PremiumServicesRepository.goldenServiceItems
                    } else {
                        PremiumServicesRepository.titaniumServiceItems
                    }

                    binding.serviceSubItem.adapter = ArrayAdapter(
                        this@PremiumActivity,
                        R.layout.spinner_item,
                        items
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    binding.tvShowInfo.visibility = View.GONE
                    binding.clShowSelectedEmail.visibility = View.GONE
                    binding.clShowInvoice.visibility = View.GONE
                    binding.clShowSelectedPlan.visibility = View.GONE
                }
            }

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

                    val price = if (selectedPlan in PremiumServicesRepository.goldenPrices) {
                        PremiumServicesRepository.goldenPrices[selectedPlan]
                    } else {
                        PremiumServicesRepository.titaniumPrices[selectedPlan]
                    }
                    binding.tvShowInvoice.text = price
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
