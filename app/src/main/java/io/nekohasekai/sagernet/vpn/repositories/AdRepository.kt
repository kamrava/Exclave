package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.vpn.DashboardActivity
import io.nekohasekai.sagernet.vpn.WelcomeActivity
import io.nekohasekai.sagernet.vpn.utils.InternetConnectionChecker
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@SuppressLint("StaticFieldLeak")
object AdRepository {
    //ADMOB unit ID
    private var ADS_APPLICATION_ID: String = "ca-app-pub-3940256099942544~3347511713"
    private var APP_OPEN_AD_UNIT_ID : String = "ca-app-pub-3940256099942544/3419835294"
    private var Banner_Ad_Unit_ID : String = "ca-app-pub-3940256099942544/6300978111"
    private var Interstitial_Ad_Unit_ID : String = "ca-app-pub-3940256099942544/1033173712"
    private var Rewarded_Ad_Unit_ID : String = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    var appOpenAd: AppOpenAd? = null
    lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)
    lateinit var bannerAdView : AdView
    lateinit var appOpenAdManager: SagerNet.AppOpenAdManager

    @SuppressLint("StaticFieldLeak")
    lateinit var internetChecker: InternetConnectionChecker
    lateinit var adContainerView : FrameLayout

    fun getAdsApplicationID(): String {
        return ADS_APPLICATION_ID
    }

    fun getAppOpenAdUnitID(): String {
        return APP_OPEN_AD_UNIT_ID
    }

    fun getBannerAdUnitID(): String {
        return Banner_Ad_Unit_ID
    }

    fun getInterstitialAdUnitID(): String {
        return Interstitial_Ad_Unit_ID
    }

    fun getRewardedAdUnitID(): String {
        return Rewarded_Ad_Unit_ID
    }

    fun setAdsApplicationID(adUnitID: String) {
        ADS_APPLICATION_ID = adUnitID
    }

    fun setAppOpenAdUnitID(adUnitID: String) {
        APP_OPEN_AD_UNIT_ID = adUnitID
    }

    fun setBannerAdUnitID(adUnitID: String) {
        Banner_Ad_Unit_ID = adUnitID
    }

    fun setInterstitialAdUnitID(adUnitID: String) {
        Interstitial_Ad_Unit_ID = adUnitID
    }

    fun setRewardedAdUnitID(adUnitID: String) {
        Rewarded_Ad_Unit_ID = adUnitID
    }

    fun loadRewardedAd(context: Context) {
        if(!canRequestAds()) {
            return
        }
        var adRequest = AdRequest.Builder().build()
        RewardedAd.load(context,getRewardedAdUnitID(), adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                println("HAMED_LOG_Rewards_onAdFailedToLoad: " + adError.message)
//                checkInternet()
                // Start periodic checks
                internetChecker?.startChecking()
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                // Stop periodic checks when no longer needed, e.g., in onDestroy()
                internetChecker?.stopChecking()
                println("HAMED_LOG_Rewards_onAdLoaded")
            }
        })
    }

    fun showRewardedAd(context: Context) {
        if(!canRequestAds()) {
            return
        }
        if(!AppRepository.isInternetAvailable()) {
            return
        }
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    rewardedAd = null
//                    loadRewardedAd(context)
                    loadRewardedAd(context)
                    println("HAMED_LOG_REWARDED_onAdDismissedFullScreenContent")
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    rewardedAd = null
                    println("HAMED_LOG_REWARDS_onAdFailedToShowFullScreenContent")
//                    loadRewardedAd(context)
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                }
            }
            rewardedAd?.let { ad ->
                println("HAMED_LOG_REWARDS_SHOW_1")
                ad.show(context as Activity, OnUserEarnedRewardListener { rewardItem ->
                    println("HAMED_LOG_REWARDS_SHOW_2")
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                })
            } ?: run {

            }
        }
    }

    suspend fun checkAdConsent(context: Context): Boolean = suspendCoroutine { continuation ->
        // Set tag for under age of consent. false means users are not under age
        // of consent.

        val debugSettings = ConsentDebugSettings.Builder(context)
            .setForceTesting(true)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .build()

        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(debugSettings)
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        println("HAMED_LOG_Consent_Status_before1: " + consentInformation.consentStatus.toString())
        println("HAMED_LOG_Consent_Status_before2: " + consentInformation.privacyOptionsRequirementStatus.toString())
        consentInformation.requestConsentInfoUpdate(
            context as Activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    context
                ) { loadAndShowError ->
                    // Consent gathering failed.
                    println("HAMED_LOG_loadAndShowError: " + loadAndShowError?.message)
                    // Consent has been gathered.
                    if (consentInformation.canRequestAds()) {
                        val consentAcquired = canShowAds(context)
                        println("HAMED_LOG_Consent_Status_can_show_ads1: " + consentInformation.canRequestAds().toString())
                        println("HAMED_LOG_Consent_Status_can_show_ads2: " + consentAcquired.toString())
//                        println("HAMED_LOG_appOpenAdManager: " + appOpenAdManager.toString())

                        continuation.resume(consentAcquired)
                        appOpenAdManager.loadAd(context)
//                        appOpenAdManager.showAdIfAvailable(context)
                        initializeMobileAdsSdk(context)
                    } else {
                        continuation.resume(false)
                        println("HAMED_LOG_NOT_CONSENT")
                    }
                }
            },
            {
                requestConsentError ->
                // Consent gathering failed.
                continuation.resume(false)
                println("HAMED_LOG_requestConsentError: " + requestConsentError.message)
            })
    }

    fun isGDPR(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val gdpr = prefs.getInt("IABTCF_gdprApplies", 0)
        return gdpr == 1
    }

    fun canShowAds(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        //https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#in-app-details
        //https://support.google.com/admob/answer/9760862?hl=en&ref_topic=9756841

        val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val vendorConsent = prefs.getString("IABTCF_VendorConsents","") ?: ""
        val vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","") ?: ""
        val purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","") ?: ""

        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, index=googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, index=googleId)

        // Minimum required for at least non-personalized ads
        return hasConsentFor(listOf(1), purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(listOf(2,7,9,10), purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI)

    }

    fun canShowPersonalizedAds(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        //https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#in-app-details
        //https://support.google.com/admob/answer/9760862?hl=en&ref_topic=9756841

        val purposeConsent = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val vendorConsent = prefs.getString("IABTCF_VendorConsents","") ?: ""
        val vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","") ?: ""
        val purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","") ?: ""

        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, index=googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, index=googleId)

        return hasConsentFor(listOf(1,3,4), purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(listOf(2,7,9,10), purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI)
    }

    // Check if a binary string has a "1" at position "index" (1-based)
    private fun hasAttribute(input: String, index: Int): Boolean {
        return input.length >= index && input[index-1] == '1'
    }

    // Check if consent is given for a list of purposes
    private fun hasConsentFor(purposes: List<Int>, purposeConsent: String, hasVendorConsent: Boolean): Boolean {
        return purposes.all { p -> hasAttribute(purposeConsent, p)} && hasVendorConsent
    }

    // Check if a vendor either has consent or legitimate interest for a list of purposes
    private fun hasConsentOrLegitimateInterestFor(purposes: List<Int>, purposeConsent: String, purposeLI: String, hasVendorConsent: Boolean, hasVendorLI: Boolean): Boolean {
        return purposes.all { p ->
            (hasAttribute(purposeLI, p) && hasVendorLI) ||
                    (hasAttribute(purposeConsent, p) && hasVendorConsent)
        }
    }

    private fun initializeMobileAdsSdk(context: Context) {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(context)

        AuthRepository.getUserAccountInfo()

        // Request load ads after user granted consent
//        if (AuthRepository.isUserAlreadyLogin()) {
//            val intent = Intent(context, DashboardActivity::class.java)
//            context.startActivity(intent)
//        }
    }

    fun checkInitializeMobileAdsSdk(context: Context) {
        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk(context)
        }
    }

    private fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    fun loadBannerAd(activity: Activity) {
        if(!canRequestAds()) {
            return
        }
        bannerAdView = AdView(activity)
        adContainerView = activity.findViewById(R.id.bannerAdView)

        adContainerView.addView(bannerAdView)
        bannerAdView.setAdSize(AdSize.BANNER)
        bannerAdView.adUnitId = getBannerAdUnitID()

        MobileAds.initialize(activity) {}

        val adRequest = AdRequest.Builder().build()

        bannerAdView.loadAd(adRequest)
    }

    fun showAppOpenAd(context: Context) {
        appOpenAdManager.showAdIfAvailable(context as Activity)
    }
}
