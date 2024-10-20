package io.nekohasekai.sagernet.vpn.repositories

object UserRepository {

    fun isFreeUser(): Boolean {
        return AuthRepository.getSelectedService()?.show_ad ?: true
    }

    fun hasUpgradableService(): Boolean {
        return AuthRepository.getSelectedService()?.upgradable ?: true
    }
}
