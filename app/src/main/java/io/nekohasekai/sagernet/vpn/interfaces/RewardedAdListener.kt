package io.nekohasekai.sagernet.vpn.interfaces

import com.google.android.gms.ads.rewarded.RewardItem

interface RewardedAdListener {
    fun onUserEarnedReward(rewardItem: RewardItem)
}