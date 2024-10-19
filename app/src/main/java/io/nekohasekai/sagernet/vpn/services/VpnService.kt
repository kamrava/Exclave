package io.nekohasekai.sagernet.vpn.services

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.VpnRequestActivity
import io.nekohasekai.sagernet.vpn.interfaces.VpnEventListener

@SuppressLint("StaticFieldLeak")
object VpnService {
    private lateinit var context: Context
    private lateinit var connect: ActivityResultLauncher<Void?>
    private val listeners = mutableListOf<VpnEventListener>()
    var canStop: Boolean = true

    fun initialize(activity: AppCompatActivity) {
        this.context = activity.applicationContext

        connect = activity.registerForActivityResult(VpnRequestActivity.StartService()) {
            if (it) {
                Toast.makeText(activity, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun startVpn() {
        if (!SagerNet.started) {
            connect.launch(null)
            listeners.forEach { it.onVpnStarted() }
        }
    }

    fun stopVpn() {
        if (canStop) {
            SagerNet.stopService()
            listeners.forEach { it.onVpnStopped() }
        }
    }

    fun toggleVpn() {
        if (isStarted()) stopVpn() else startVpn()
    }

    fun startOrReloadVpn() {
        if (SagerNet.started) {
            SagerNet.reloadService()
            return
        }
        startVpn()
    }

    fun isStarted(): Boolean {
        return SagerNet.started
    }

    fun startVpnFromProfile(profileId: Long) {
        DataStore.selectedProxy = profileId
        startOrReloadVpn()
        listeners.forEach { it.onVpnServerChanged(profileId) }
    }

    // Function to add listeners
    fun addVpnEventListener(listener: VpnEventListener) {
        listeners.add(listener)
    }

    // Function to remove listeners
    fun removeVpnEventListener(listener: VpnEventListener) {
        listeners.remove(listener)
    }
}