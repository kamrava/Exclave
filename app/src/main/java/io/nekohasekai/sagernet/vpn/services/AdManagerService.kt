package io.nekohasekai.sagernet.vpn.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import io.nekohasekai.sagernet.vpn.interfaces.RewardedAdListener
import io.nekohasekai.sagernet.vpn.repositories.AdRepository.consentInformation
import io.nekohasekai.sagernet.vpn.repositories.AdRepository.getRewardedAdUnitID
import io.nekohasekai.sagernet.vpn.repositories.AdRepository.internetChecker
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import io.nekohasekai.sagernet.vpn.repositories.UserRepository

class AdManagerService private constructor() {
    companion object {
        private var rewardedAd: RewardedAd? = null // Use nullable type
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context
        private var loadingAd: Boolean = false // Flag to prevent multiple ad loads
        private lateinit var rewardedAdListener: RewardedAdListener

        private fun canRequestAds(): Boolean {
            return consentInformation.canRequestAds()
        }

        fun initialize(rewardedAdListener: RewardedAdListener) {
            this.rewardedAdListener = rewardedAdListener
        }

        fun loadRewardedAd(context: Context) {
            if (!canRequestAds() && !UserRepository.isFreeUser()) {
                return
            }
            if (loadingAd || rewardedAd != null) {
                // Prevent multiple ad loads if an ad is already loading or loaded
                AppRepository.debugLog("Ad is already loading or loaded.")
                return
            }

            this.context = context
            loadingAd = true // Set flag to indicate loading
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(context, getRewardedAdUnitID(), adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AppRepository.debugLog("Rewards_onAdFailedToLoad: " + adError.message)
                    internetChecker.startChecking()
                    loadingAd = false // Reset flag if load failed
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    internetChecker.stopChecking()
                    AppRepository.debugLog("Rewards_onAdLoaded")
                    loadingAd = false // Reset flag once ad is loaded
                }
            })
        }

        fun showRewardedAd() {
            if (!shouldShowAd()) {
                return
            }
            rewardedAd?.let { ad ->
                ad.show((this.context as Activity), OnUserEarnedRewardListener { rewardItem ->
                    rewardedAdListener.onUserEarnedReward(rewardItem)
                })
                // After showing the ad, set rewardedAd to null to allow reloading
                rewardedAd = null
                loadRewardedAd(this.context)
            } ?: AppRepository.debugLog("The rewarded ad wasn't ready yet.")
        }

        fun shouldShowAd(): Boolean {
            return AuthRepository.getSelectedService()?.show_ad ?: true
        }
    }
}
