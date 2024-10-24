package io.nekohasekai.sagernet.vpn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.messaging.FirebaseMessaging
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.databinding.ActivitySplashBinding
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SplashActivity : BaseThemeActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var checkUpdate: AlertDialog

    fun ProxyGroup.init() {
        DataStore.groupType = type
        DataStore.groupOrder = order
        val subscription = subscription ?: SubscriptionBean().applyDefaultValues()
        DataStore.subscriptionType = subscription.type
        DataStore.subscriptionToken = subscription.token
        DataStore.subscriptionLink = AppRepository.getSubscriptionLink()
        DataStore.subscriptionForceResolve = subscription.forceResolve
        DataStore.subscriptionDeduplication = subscription.deduplication
        DataStore.subscriptionUpdateWhenConnectedOnly = subscription.updateWhenConnectedOnly
        DataStore.subscriptionUserAgent = subscription.customUserAgent
        DataStore.subscriptionAutoUpdate = subscription.autoUpdate
        DataStore.subscriptionAutoUpdateDelay = subscription.autoUpdateDelay
        DataStore.frontProxyOutbound = frontProxy
        DataStore.landingProxyOutbound = landingProxy
        DataStore.frontProxy = if (frontProxy >= 0) 1 else 0
        DataStore.landingProxy = if (landingProxy >= 0) 1 else 0
    }

    fun ProxyGroup.serialize() {
        name = DataStore.groupName.takeIf { it.isNotBlank() }
            ?: ("My group " + System.currentTimeMillis() / 1000)
        type = DataStore.groupType
        order = DataStore.groupOrder

        frontProxy = if (DataStore.frontProxy == 1) DataStore.frontProxyOutbound else -1
        landingProxy = if (DataStore.landingProxy == 1) DataStore.landingProxyOutbound else -1

        if (type == GroupType.SUBSCRIPTION) {
            subscription = (subscription ?: SubscriptionBean().applyDefaultValues()).apply {
                type = DataStore.subscriptionType
                link = DataStore.subscriptionLink
                token = DataStore.subscriptionToken
                forceResolve = DataStore.subscriptionForceResolve
                deduplication = DataStore.subscriptionDeduplication
                updateWhenConnectedOnly = DataStore.subscriptionUpdateWhenConnectedOnly
                customUserAgent = DataStore.subscriptionUserAgent
                autoUpdate = DataStore.subscriptionAutoUpdate
                autoUpdateDelay = DataStore.subscriptionAutoUpdateDelay
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up ProgressBar and Try Again button using ViewBinding
        binding.btnTryAgain.setOnClickListener {
            retryLoading()
        }

        // Start the loading process
        //startLoading()

        //Show AdMob Interstitial
//        loadInterstitialAd()

        loadFcmToken()
        AppRepository.sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        lifecycleScope.launch(Dispatchers.IO) {
            val userConsent = AdRepository.checkAdConsent(this@SplashActivity)
            startLoading()
        }


//        GlobalScope.launch(Dispatchers.Main) {
            // Check Ad Consent

//              startWelcomeActivity()
//            showInterstitialAd()
//        }

//        runOnDefaultDispatcher {
//            val entity = SagerDatabase.groupDao.getById(1)
//            ProxyGroup().init()
//            var subscription = ProxyGroup().apply { serialize() }
//            if (entity == null) {
//                GroupManager.createGroup(subscription)
//            }
//            GroupUpdater.startUpdate(subscription, true)
//        }
    }

    private fun checkForUpdate() {
        if (AppRepository.appShouldForceUpdate) {
            showForceUpdateDialog()
        }
    }

    private fun showForceUpdateDialog() {
        val intent = Intent(this, ForceUpdateActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppRepository.debugLog("Fetching FCM registration token failed")
                return@addOnCompleteListener
            }
            val token = task.result
            AppRepository.debugLog("FCM token: $token")
        }
    }

    private fun showInterstitialAd() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                mInterstitialAd = null
            }
        }
        mInterstitialAd?.show(this)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }

    private fun startLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnTryAgain.visibility = View.GONE

        GlobalScope.launch(Dispatchers.Main) {
            val result = withTimeoutOrNull(30000) {
                try {
                    getSettings()
                    binding.progressBar.progress = 40
                    checkForUpdate()
                    binding.progressBar.progress = 60

                    if (!AppRepository.appShouldForceUpdate && AuthRepository.isUserAlreadyLogin()) {
                        getServers()
                        binding.progressBar.progress = 80
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }

            if (result == true) {
                startWelcomeActivity()
            } else {
                showRetryOption()
            }
        }
    }

    private fun retryLoading() {
        startLoading()
    }

    private fun showRetryOption() {
        binding.progressBar.visibility = View.GONE
        binding.btnTryAgain.visibility = View.VISIBLE
    }

    private fun startWelcomeActivity() {
        val intent = if (!AuthRepository.isUserAlreadyLogin()) {
            Intent(this, WelcomeActivity::class.java)
        } else {
            Intent(this, DashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private suspend fun getServers(): String {
        return withContext(Dispatchers.IO) {
            AppRepository.getServersAndImport(this@SplashActivity)
        }
    }

    private suspend fun getSettings(): String {
        return withContext(Dispatchers.IO) {
            AppRepository.getSettingsSync()
            "Finished"
        }
    }
}
