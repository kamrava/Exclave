package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.system.ErrnoException
import android.util.ArrayMap
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.SagerNet
import android.content.DialogInterface
import io.nekohasekai.sagernet.SubscriptionType
import androidx.core.view.isGone
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.group.RawUpdater
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.plugin.PluginManager
import io.nekohasekai.sagernet.bg.test.V2RayTestInstance
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.vpn.helpers.GenericHelper
import io.nekohasekai.sagernet.vpn.models.AppSetting
import io.nekohasekai.sagernet.vpn.models.ListItem
import io.nekohasekai.sagernet.vpn.models.ListSubItem
import io.nekohasekai.sagernet.vpn.serverlist.ListItemAdapter
import io.nekohasekai.sagernet.vpn.utils.CustomTestDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

object AppRepository {
    var LogTag: String = "HAMED_LOG"
    var appName: String = "UnitaVPN"
    private var subscriptionLink: String = "https://Apanel.holyip.workers.dev/link/9RTsfMryrGwgWZVb48eN?config=1"
    private var apiServersListUrl: String = "https://api.unitavpn.com/api/user/servers"
    private var baseUrl: String = "https://api.unitavpn.com/"
    private var userLoginUrl: String = "https://api.unitavpn.com/api/auth/login"
    private var userRegisterUrl: String = "https://api.unitavpn.com/api/client/register"
    private var userCheckEmailAvailability: String = "https://api.unitavpn.com/api/auth/register/check"
    private var userVerifyUrl: String = "https://api.unitavpn.com/api/auth/verify"
    private var userResetPasswordUrl: String = "https://api.unitavpn.com/password/reset"
    private var userStateUrl: String = "https://unitavpn.com/api/client/account/info"
    private var panelApiHeaderToken: String = "9f8a833ca1383c5449e1d8800b45fd54"
    private var panelSettingsUrl = "https://api.unitavpn.com/api/settings"
    var selectedServerId: Long = -1
    var ShareCustomMessage: String = "Share $appName whit your friends and family"
    var ShareApplicationLink: String = "https://play.google.com/store/apps/details?id=com.File.Manager.Filemanager&pcampaignid=web_share"
    var telegramLink: String = "https://t.me/unitavpn"
    var allServers: MutableList<ListItem> = mutableListOf()
    var allServersOriginal: MutableList<ListItem> = mutableListOf()
    lateinit var allServersRaw: JsonObject
    lateinit var recyclerView: RecyclerView
    var isBestServerSelected: Boolean = false
    lateinit var sharedPreferences: SharedPreferences
    var isConnected: Boolean = false
    var filterServersBy: String = "all"
    var appVersionCode: Int = 0
    var appShouldForceUpdate: Boolean = false
    lateinit var appSetting: AppSetting
    private var isInternetConnected = true

    fun getAllServer(): MutableList<ListItem> {
        val allServersString = sharedPreferences.getString("allServers", null)
        if (allServersString === null) {
            filterServersByTag(filterServersBy)
            return allServers
        }
        val gson = Gson()
        val itemType = object : TypeToken<MutableList<ListItem>>() {}.type
        val allServersList = gson.fromJson<MutableList<ListItem>>(allServersString, itemType)
        sharedPreferences.edit().remove("allServers").apply()
        allServers = allServersList
        return allServersList
    }

    fun setAllServer(servers: MutableList<ListItem>) {
        val gson = Gson()
        val allServersInJson = gson.toJson(servers)
        sharedPreferences.edit().putString("allServers", allServersInJson).apply()
    }

    fun clearServerSelections() {
        allServers.map { entry ->
            entry.isSelected = false
            entry.dropdownItems.forEach {
                it.isSelected = false
            }
        }

        setAllServer(allServers)
        refreshServersListView()
    }

    fun getSubscriptionLink(): String {
        return subscriptionLink
    }

    private fun getBaseUrl(): String? {
        return baseUrl
    }

    fun getUserLoginUrl(): String {
        return userLoginUrl
    }

    fun getUserVerifyUrl(): String {
        return userVerifyUrl
    }

    fun getPanelApiHeaderToken(): String {
        return panelApiHeaderToken
    }

    fun getUserRegisterUrl(): String {
        return userRegisterUrl
    }

    fun getUserCheckEmailAvailabilityUrl(): String {
        return userCheckEmailAvailability
    }

    fun getUserResetPasswordUrl(): String {
        return userResetPasswordUrl
    }

    fun getAppSettings() {
        val client = OkHttpClient()
        val request = getHttpRequest(panelSettingsUrl, null, "GET")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code ${response.code}")
                    }
                    appSetting = Gson().fromJson(response.body!!.string(), AppSetting::class.java)
                }
            }
        })
    }

    fun getSettingsSync(): Int {
        val client = OkHttpClient()
        val url = panelSettingsUrl

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                appSetting = Gson().fromJson(response.body!!.string(), AppSetting::class.java)
            }
            return response.code
        } catch (e: Exception) {
            debugLog("Get_Settings_Request_Failed: ${e.message}")
            return -1
        }
    }

    private fun setVersionCode(versionCodeParam: Int, forceUnderParam: Int) {
        appVersionCode = versionCodeParam
        appShouldForceUpdate = BuildConfig.VERSION_CODE <= forceUnderParam
    }

    fun getServersListAsync() {
        val client = OkHttpClient()
        val request = getHttpRequest(apiServersListUrl, null, "GET")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code ${response.code}")
                    }
                    val gson = Gson()
                    val jsonObject = gson.fromJson(response.body!!.string(), JsonObject::class.java)
                    val servers = jsonObject.get("servers").asJsonObject
                    servers.entrySet().forEach { it ->
                        println("HAMED_LOG_SERVER: " + it.toString())
                    }
                }
            }
        })
    }

    fun getServersListSync(): Int {
        // Configure OkHttpClient with timeouts
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()

        val url = apiServersListUrl
        val userAccountInfo = AuthRepository.getUserAccountInfo()

        // Build the request with headers
        val request = Request.Builder()
            .url(url)
            .header("xmplus-authorization", getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + userAccountInfo.data.token)
            .get()
            .build()

        try {
            // Execute the request
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val serversObject = gson.fromJson(responseBody, JsonObject::class.java)
                val servers = serversObject.get("servers").asJsonObject
                allServersRaw = servers
            }
            return response.code
        } catch (e: Exception) {
            debugLog("Get_Servers_Request_Failed: ${e.message}")
            return -1
        }
    }

    private fun getRawServersConfigAsString(): String {
        var serversConfigString: MutableList<String> = mutableListOf()
        allServersRaw.entrySet().forEach { it ->
            it.value.asJsonArray.forEach { it ->
                serversConfigString.add(it.asJsonObject.get("config").asString)
            }
        }
        return serversConfigString.joinToString("\n")
    }


    fun getHttpRequest(
        url: String, formParams: HashMap<String, String>?, requestType: String = "GET"
    ): Request {
        val requestBuilder = FormBody.Builder()

        formParams?.forEach { (key, value) ->
            requestBuilder.add(key, value)
        }

        val formBody = requestBuilder.build()

        val request = Request.Builder()
            .header("xmplus-authorization", getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .url(url)

        if (requestType == "GET") {
            request.get()
        } else if (requestType == "POST") {
            request.post(formBody)
        }

        return request.build()
    }

    fun countryCodeToFlag(countryCode: String): String {
        try {
            var iso = countryCode
            if (countryCode.lowercase() == "other") {
                iso = "us"
            }
            val flag = iso.uppercase()
                .map { char -> 0x1F1E6 - 'A'.code + char.code }
                .map { String(Character.toChars(it)) }
                .joinToString("")
            return flag
        } catch (e: ErrnoException) {
            return countryCodeToFlag("us")
        }
    }

    fun getItemName(countryCode: String, isBestServer: Boolean = false): String {
        val flag = countryCodeToFlag(countryCode)
        val name = if (isBestServer) {
            "Best Location"
        } else {
            GenericHelper.countryMapper(countryCode)
        }
        return "$flag   $name"
    }

    fun refreshServersListView() {
        val adapter = ListItemAdapter(getAllServer()) { }
        recyclerView.adapter = adapter
    }

    fun resetAllSubItemsStatus() {
        val servers = allServersOriginal
        servers.forEach { item ->
            item.dropdownItems.forEach { subItem ->
                subItem.isSelected = false
            }
        }
        allServers = servers.filter { element -> element in allServers }.toMutableList()
    }

    fun clearAllItemsSelections() {
        val adapter = ListItemAdapter(getAllServer()) { }
        recyclerView.adapter = adapter
        adapter.resetAllSubItems(-1L)
    }

    fun filterServersByTag(tag: String): Unit {
        filterServersBy = tag
        var servers = allServersOriginal
        if (tag === "all") {
            allServers = servers
            return
        }
        allServers = servers.map { item ->
            item.copy(dropdownItems = item.dropdownItems.filter { subItem ->
                subItem.tags.contains(tag)
            }.toMutableList())
        }.toMutableList()
    }

    fun setLastInternetSatus(status: Boolean) {
        isInternetConnected = status
    }

    fun isInternetAvailable(): Boolean {
        return isInternetConnected
    }

    fun debugLog(message: String) {
        Log.d(LogTag, message);
    }

    private fun setServerPing(serverId: Long, ping: Int, status: Int): ArrayMap<String, String> {
        val arrayMap: ArrayMap<String, String> = ArrayMap()
        arrayMap["countryCode"] = ""
        arrayMap["serverName"] = ""
        allServers.forEach { item ->
            item.dropdownItems.forEach {
                if (it.id == serverId) {
                    arrayMap["countryCode"] = item.countryCode
                    arrayMap["serverName"] = it.name
                    it.ping = ping
                    it.status = status
                    return arrayMap
                }
            }
        }
        return arrayMap
    }

    suspend fun getServersAndImport(context: Context): String {
        return withContext(Dispatchers.IO) {
            val response = getServersListSync()
            if (response == 200) {
                val serversString = getRawServersConfigAsString()
                val proxies = RawUpdater.parseRaw(serversString)
                if (!proxies.isNullOrEmpty()) {
                    import(proxies, context)
                    silentUrlTestAsync()
                }
            }
            "Finished"
        }
    }

    private suspend fun silentUrlTestAsync() = withContext(Dispatchers.IO) {
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
        val workingCounter = working.get()
        val unavailableCounter = unavailable.get()
        debugLog("WorkingServers: $workingCounter - UnavailableServers: $unavailableCounter")
        debugLog("BestServer: " + bestServer.countryCode + " - " + bestServer.pointToIndex)
    }


    @SuppressLint("DiscouragedApi")
    suspend fun import(proxies: List<AbstractBean>, context: Context) {
        removeAllProfiles()
        val targetId = DataStore.selectedGroupForImport()
        var counter = -1
        allServers = mutableListOf()
        allServers.clear()
        allServersOriginal = mutableListOf()
        allServersOriginal.clear()
        setAllServer(allServers)
        allServersRaw.entrySet().forEach { entry ->
            val serverSubItems: MutableList<ListSubItem> = mutableListOf()
            val countryCode = entry.key
            val countryName = getItemName(countryCode)
            entry.value.asJsonArray.forEach { it ->
                counter++
                val profile = ProfileManager.createProfile(targetId, proxies[counter])
                val serverId = it.asJsonObject.get("id").asInt
                val tagsArray = it.asJsonObject.getAsJsonArray("tags")
                val tags = Array(tagsArray.size()) { tagsArray[it].asString }
                serverSubItems.add(
                    ListSubItem(
                        id = profile.id,
                        serverId = serverId,
                        name = it.asJsonObject.get("name").asString,
                        profile.status,
                        error = profile.error,
                        ping = profile.ping,
                        tags = tags,
                        profileIndex = counter
                    )
                )
            }
            allServers.add(
                ListItem(
                    name = countryName,
                    countryCode = countryCode,
                    dropdownItems = serverSubItems
                )
            )
        }
        allServersOriginal = allServers
        setAllServer(allServers)

        onMainDispatcher {
            DataStore.editingGroup = targetId
        }
    }

    private suspend fun removeAllProfiles() {
        val groupId = DataStore.selectedGroupForImport()
        val profilesUnfiltered = SagerDatabase.proxyDao.getByGroup(groupId)
        val profiles = ConcurrentLinkedQueue(profilesUnfiltered)
        profiles.forEach { it ->
            ProfileManager.deleteProfile2(
                it.groupId, it.id
            )
        }
    }

    fun urlTest(context: Context) {
        val test = CustomTestDialog(context)
        val dialog = test.builder.show()
        val testJobs = mutableListOf<Job>()
        var bestPing = 9999999

        val mainJob = runOnDefaultDispatcher {
            val group = DataStore.currentGroup()
            var profilesUnfiltered = SagerDatabase.proxyDao.getByGroup(group.id)
            if (group.subscription?.type == SubscriptionType.OOCv1) {
                val subscription = group.subscription!!
                if (subscription.selectedGroups.isNotEmpty()) {
                    profilesUnfiltered = profilesUnfiltered.filter { it.requireBean().group in subscription.selectedGroups }
                }
                if (subscription.selectedOwners.isNotEmpty()) {
                    profilesUnfiltered = profilesUnfiltered.filter { it.requireBean().owner in subscription.selectedOwners }
                }
                if (subscription.selectedTags.isNotEmpty()) {
                    profilesUnfiltered = profilesUnfiltered.filter { profile ->
                        profile.requireBean().tags.containsAll(
                            subscription.selectedTags
                        )
                    }
                }
            }
            val profiles = ConcurrentLinkedQueue(profilesUnfiltered)
            stopService()

            val link = appSetting.urlTest.link
            val timeout = appSetting.urlTest.timeout
            lateinit var bestServer: ListItem

            repeat(6) {
                testJobs.add(launch {
                    while (isActive) {
                        val profile = profiles.poll() ?: break
                        profile.status = 0
                        test.insert(profile)

                        try {
                            var countryCode = ""
                            var serverName = ""
                            val instance = V2RayTestInstance(profile, link, timeout)
                            val result = instance.use {
                                it.doTest()
                            }

                            profile.status = 1
                            profile.ping = result
                            val bestServerInfo = setServerPing(profile.id, result, 1)
                            countryCode = bestServerInfo["countryCode"].toString()
                            if (result < bestPing) {
                                val emptyList: MutableList<ListSubItem> = mutableListOf()
                                bestPing = result
                                isBestServerSelected = true
                                bestServer = ListItem(
                                    id = profile.id,
                                    name = getItemName(countryCode, true),
                                    countryCode = countryCode,
                                    dropdownItems = emptyList,
                                    isExpanded = false,
                                    isBestServer = true
                                )
                            }
                            setServerPing(profile.id, result, 1)
                        } catch (e: PluginManager.PluginNotFoundException) {
                            profile.status = -1
                            profile.error = e.readableMessage
                            setServerPing(profile.id, -1, -1)
                        } catch (e: Exception) {
                            profile.status = 3
                            profile.error = e.readableMessage
                            setServerPing(profile.id, -1, 3)
                        }
                        test.update(profile)
                        ProfileManager.updateProfile(profile)
                    }
                })
            }

            testJobs.joinAll()
            test.close()

            onMainDispatcher {
                bestServer.let {
                    if (allServers[0].isBestServer) {
                        allServers.removeAt(0)
                    }

                    allServers.add(0, it)
                    setAllServer(allServers)
                    refreshServersListView()
                }
                test.binding.progressCircular.isGone = true
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(android.R.string.ok)
            }
        }
        test.cancel = {
            mainJob.cancel()
            runOnDefaultDispatcher {
                GroupManager.postReload(DataStore.currentGroupId())
            }
        }
    }

    fun stopService() {
        if (SagerNet.started) SagerNet.stopService()
    }
}
