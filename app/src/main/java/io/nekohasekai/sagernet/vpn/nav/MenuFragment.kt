package io.nekohasekai.sagernet.vpn.nav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.databinding.FragmentMenuBinding
import io.nekohasekai.sagernet.vpn.DashboardActivity
import io.nekohasekai.sagernet.vpn.PremiumActivity
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        setupClickListeners()
        setupBackButtonHandling()
        return binding.root
    }

    private fun setupClickListeners() {
        binding.apply {
            ivPreferencesIconAngle.setOnClickListener { openActivity(DashboardActivity::class.java) }
            llGeneral.setOnClickListener { loadFragment(GeneralFragment()) }
            llAccount.setOnClickListener { loadFragment(AccountFragment()) }
            llPremium.setOnClickListener { openActivity(PremiumActivity::class.java) }
            llShare.setOnClickListener { shareLinkWithMessage(AppRepository.ShareCustomMessage) }
            llTelegram.setOnClickListener { openUri(AppRepository.telegramLink) }
            llPrivacyPolicy.setOnClickListener { openActivity(PrivacyPolicyActivity::class.java) }
            llTermsOfService.setOnClickListener { openActivity(TermsOfServiceActivity::class.java) }
        }
    }

    private fun setupBackButtonHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                openActivity(DashboardActivity::class.java)
            }
        })
    }

    private fun openActivity(activityClass: Class<*>) {
        startActivity(Intent(requireContext(), activityClass))
    }

    private fun openUri(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }

    private fun loadFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun shareLinkWithMessage(message: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$message\n${AppRepository.ShareApplicationLink}")
        }
        startActivity(Intent.createChooser(sendIntent, "Share link with:"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = MenuFragment()
    }
}
