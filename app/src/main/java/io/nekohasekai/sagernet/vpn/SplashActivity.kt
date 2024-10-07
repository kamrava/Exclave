package io.nekohasekai.sagernet.vpn

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.withTimeoutOrNull
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SplashActivity : BaseThemeActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tryAgainButton: AppCompatButton
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var checkUpdate: AlertDialog
    fun ProxyGroup.init() {
//        DataStore.groupName = name ?: AppRepository.appName
        DataStore.groupName = "Ungrouped"
        DataStore.groupType = 1
        DataStore.groupOrder = order
        DataStore.groupIsSelector = isSelector
        DataStore.frontProxy = frontProxy
        DataStore.landingProxy = landingProxy
        DataStore.frontProxyTmp = if (frontProxy >= 0) 3 else 0
        DataStore.landingProxyTmp = if (landingProxy >= 0) 3 else 0

        val subscription = subscription ?: SubscriptionBean().applyDefaultValues()
        DataStore.subscriptionLink = AppRepository.getSubscriptionLink()
//        DataStore.subscriptionLink = subscription.link
        DataStore.subscriptionForceResolve = subscription.forceResolve
        DataStore.subscriptionDeduplication = subscription.deduplication
        DataStore.subscriptionUpdateWhenConnectedOnly = subscription.updateWhenConnectedOnly
        DataStore.subscriptionUserAgent = subscription.customUserAgent
        DataStore.subscriptionAutoUpdate = subscription.autoUpdate
        DataStore.subscriptionAutoUpdateDelay = subscription.autoUpdateDelay
    }

    fun ProxyGroup.serialize() {
        name = DataStore.groupName.takeIf { it.isNotBlank() } ?: "My group"
        type = DataStore.groupType
        order = DataStore.groupOrder
        isSelector = DataStore.groupIsSelector

        frontProxy = if (DataStore.frontProxyTmp == 3) DataStore.frontProxy else -1
        landingProxy = if (DataStore.landingProxyTmp == 3) DataStore.landingProxy else -1

        val isSubscription = type == GroupType.SUBSCRIPTION
        if (isSubscription) {
            subscription = (subscription ?: SubscriptionBean().applyDefaultValues()).apply {
                link = DataStore.subscriptionLink
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
        setContentView(R.layout.activity_splash)

        // Initialize the ProgressBar and Try Again Button
        progressBar = findViewById(R.id.progressBar)
        tryAgainButton = findViewById(R.id.btnTryAgain)



        // Set the Try Again button click listener
        tryAgainButton.setOnClickListener {
            retryLoading()
        }

        // Start the loading process
        //startLoading()


        //Show AdMob Interstitial
//        loadInterstitialAd()

        loadFcmToken()
        AppRepository.sharedPreferences = getSharedPreferences("CountdownPrefs", Context.MODE_PRIVATE)

        lifecycleScope.launch(Dispatchers.IO) {
            val userConsent = AdRepository.checkAdConsent(this@SplashActivity)
            startLoading()
            AppRepository.debugLog("HAMED_LOG_TEST_203: " + userConsent.toString())
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
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppRepository.debugLog("Fetching FCM registration token failed")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            AppRepository.debugLog("FCM token: $token")
        })
    }

    private fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                }
                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    mInterstitialAd = null
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    mInterstitialAd = null
                }
                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                }
            }
            mInterstitialAd?.show(this)
        } else {
//            startNewActivity()
        }
    }

    private fun loadInterstitialAd() {

        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun startLoading() {
        // Show the progress bar and hide the Try Again button
        progressBar.visibility = View.VISIBLE
        tryAgainButton.visibility = View.GONE

        // Launch a coroutine to handle loading
        GlobalScope.launch(Dispatchers.Main) {
            val result = withTimeoutOrNull(30000) { // 30 seconds timeout
                try {
                    getSettings()

                    // Update progress bar to 50% after getSettings() completes successfully
                    progressBar.progress = 40

                    checkForUpdate()

                    progressBar.progress = 60

                    if (!AppRepository.appShouldForceUpdate && AuthRepository.isUserAlreadyLogin()) {
                        getServers()
                        // Update progress bar to 80% after getServers() completes successfully
                        progressBar.progress = 80
                    }
                    true // Indicate success
                } catch (e: Exception) {
                    false // Indicate failure
                }
            }

            if (result == true) {
                startWelcomeActivity() // Start next activity if loading is successful
            } else {
                showRetryOption() // Show the Try Again button if loading fails or times out
            }
        }
    }

    private fun retryLoading() {
        startLoading() // Restart the loading process when Try Again is clicked
    }

    private fun showRetryOption() {
        // Hide the progress bar and show the Try Again button
        progressBar.visibility = View.GONE
        tryAgainButton.visibility = View.VISIBLE
    }

    private fun startWelcomeActivity() {
        // Determine which activity to start based on user authentication status
        val intent = if (!AuthRepository.isUserAlreadyLogin()) {
            Intent(this, WelcomeActivity::class.java)
        } else {
            Intent(this, DashboardActivity::class.java)
        }
        startActivity(intent)
        finish() // Finish the SplashActivity
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
