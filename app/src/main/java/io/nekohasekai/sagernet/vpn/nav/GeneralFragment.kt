package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.databinding.FragmentGeneralBinding
import io.nekohasekai.sagernet.vpn.repositories.LanguageSelectorRepository

class GeneralFragment : Fragment() {
    private var _binding: FragmentGeneralBinding? = null // Backing property for binding
    private val binding get() = _binding!! // Non-nullable property for binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewBinding
        _binding = FragmentGeneralBinding.inflate(inflater, container, false)

        // Set up UI elements and listeners
        setupUI()

        return binding.root
    }

    private fun setupUI() {
        binding.ivGeneralIconAngle.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val appVersion = BuildConfig.VERSION_NAME
        binding.tvVersion.text = appVersion

        LanguageSelectorRepository.setupLanguageSelector(
            requireContext(),
            requireActivity(),
            binding.spLanguageSelector
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference to prevent memory leaks
    }
}
