package io.nekohasekai.sagernet.vpn.interfaces

interface VpnEventListener {
    fun onVpnStopped()
    fun onVpnStarted()
    fun onVpnServerChanged(newProfileId: Long)
    fun onPingTestFinished()
}
