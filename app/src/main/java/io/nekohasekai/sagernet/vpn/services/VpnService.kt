package io.nekohasekai.sagernet.vpn.services

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.bg.test.V2RayTestInstance
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.plugin.PluginManager
import io.nekohasekai.sagernet.ui.VpnRequestActivity
import io.nekohasekai.sagernet.vpn.interfaces.VpnEventListener
import io.nekohasekai.sagernet.vpn.models.ListItem
import io.nekohasekai.sagernet.vpn.models.ListSubItem
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.allServers
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.allServersOriginal
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.appSetting
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.debugLog
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.getItemName
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.isBestServerSelected
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.setAllServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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

    suspend fun silentUrlTestAsync() = withContext(Dispatchers.IO) {
        val link = appSetting.urlTest.link
        val timeout = appSetting.urlTest.timeout
        var working = AtomicInteger(0)
        var unavailable = AtomicInteger(0)
        var bestPing = 9999
        lateinit var bestServer: ListItem

        val allServerItemsDeferred = allServers.map { entry ->
            async {
                entry.dropdownItems.forEach {
                    var result = 99999
                    var status = 1
                    var error = ""

                    val profile = ProfileManager.getProfile(it.id)
                    val instance = profile?.let { profileItem ->
                        V2RayTestInstance(profileItem, link, timeout)
                    }

                    try {
                        result = instance.use { testInstance ->
                            testInstance?.doTest() ?: -1
                        }
                        debugLog("onPingTest: " + entry.countryCode + " - $result")
                        val workingCounter = working.incrementAndGet()
                        if (result < bestPing) {
                            val emptyList: MutableList<ListSubItem> = mutableListOf()
                            bestPing = result
                            isBestServerSelected = true
                            bestServer = ListItem(
                                name = getItemName(entry.countryCode, true),
                                countryCode = entry.countryCode,
                                dropdownItems = emptyList,
                                isExpanded = false,
                                isBestServer = true,
                                id = it.id,
                                pointToIndex = it.profileIndex
                            )
                        }
                    } catch (e: PluginManager.PluginNotFoundException) {
                        val unavailableCounter = unavailable.incrementAndGet()
                        result = -1
                        status = -1
                        error = e.readableMessage
                    } catch (e: Exception) {
                        val unavailableCounter = unavailable.incrementAndGet()
                        result = -1
                        status = 3
                        error = e.readableMessage
                    } finally {
                        it.ping = result
                        it.status = status
                        it.error = error
                    }
                }
                entry
            }
        }
        allServers = allServerItemsDeferred.awaitAll().toMutableList()

        bestServer.let {
            if (allServers[0].isBestServer) {
                allServers.removeAt(0)
            }

            allServers.add(0, it)
        }

        allServersOriginal = allServers
        setAllServer(allServers)
        listeners.forEach { it.onPingTestFinished() }

        val workingCounter = working.get()
        val unavailableCounter = unavailable.get()
        debugLog("WorkingServers: $workingCounter - UnavailableServers: $unavailableCounter")
        debugLog("BestServer: " + bestServer.countryCode + " - " + bestServer.pointToIndex)
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