package io.nekohasekai.sagernet.vpn.services

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.VpnRequestActivity
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

@SuppressLint("StaticFieldLeak")
object VpnService {
    private lateinit var context: Context
    private lateinit var connect: ActivityResultLauncher<Void?>


    fun initialize(activity: AppCompatActivity) {
        this.context = activity.applicationContext

        connect = activity.registerForActivityResult(VpnRequestActivity.StartService()) {
            if (it) {
                println("HAMED_LOG_" + R.string.vpn_permission_denied)
            }
        }
    }

    fun startVpn() {
        if (AppRepository.canStop) SagerNet.stopService() else connect.launch(null)
    }

    fun startOrReloadVpn() {
        if (SagerNet.started) {
            SagerNet.reloadService()
            return
        }
        startVpn()
    }

    fun startVpnFromProfile(profileId: Long) {
        DataStore.selectedProxy = profileId
        startOrReloadVpn()
    }
}