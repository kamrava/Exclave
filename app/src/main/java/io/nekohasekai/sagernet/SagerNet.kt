package io.nekohasekai.sagernet

import android.annotation.SuppressLint
import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.StrictMode
import android.os.UserManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import go.Seq
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.bg.test.DebugInstance
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.utils.CrashHandler
import io.nekohasekai.sagernet.utils.DefaultNetworkListener
import io.nekohasekai.sagernet.utils.DeviceStorageApp
import io.nekohasekai.sagernet.utils.PackageCache
import io.nekohasekai.sagernet.utils.Theme
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import libcore.Libcore
import libcore.UidDumper
import libcore.UidInfo
import java.net.InetSocketAddress
import androidx.work.Configuration as WorkConfiguration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.firebase.FirebaseApp
import io.nekohasekai.sagernet.vpn.di.appModule
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AdRepository.appOpenAdManager
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import java.util.Date
import org.koin.core.context.startKoin

class SagerNet : Application(),
    UidDumper,
    WorkConfiguration.Provider, Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private val LOG_TAG: String = "HAMED_LOG_SagerNet"
    private var currentActivity: Activity? = null

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Updating the currentActivity only when an ad is not showing.
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        application = this
    }

    val externalAssets by lazy { getExternalFilesDir(null) ?: filesDir }

    override fun onCreate() {
        super.onCreate()

        FacebookSdk.setClientToken("30d63da7ac404d3a92fe9c04a1baf590")
        FirebaseApp.initializeApp(this)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        // Loads DI(Dependency Injection) modules
        startKoin {
            modules(appModule)
        }

        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler)
        DataStore.init()
        updateNotificationChannels()
        Seq.setContext(this)

        runOnDefaultDispatcher {
            PackageCache.register()
        }

        val isMainProcess = ActivityThread.currentProcessName() == BuildConfig.APPLICATION_ID

        if (!isMainProcess) {
            Libcore.setUidDumper(this, Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            if (BuildConfig.DEBUG) {
                DebugInstance().launch()
            }
        }

        Libcore.setenv("v2ray.conf.geoloader", "memconservative")
        externalAssets.mkdirs()
        Libcore.initializeV2Ray(
            filesDir.absolutePath + "/",
            externalAssets.absolutePath + "/",
            "v2ray/",
            { DataStore.rulesProvider == 0 },
            { DataStore.providerRootCA == RootCAProvider.SYSTEM },
            isMainProcess
        )

        Theme.apply(this)
        Theme.applyNightTheme()

        if (BuildConfig.DEBUG) StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build()
        )

        // Ads
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
    }

    /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        currentActivity?.let {
            appOpenAdManager.showAdIfAvailable(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun dumpUid(
        ipProto: Int, srcIp: String, srcPort: Int, destIp: String, destPort: Int
    ): Int {
        return SagerNet.connectivity.getConnectionOwnerUid(
            ipProto, InetSocketAddress(srcIp, srcPort), InetSocketAddress(destIp, destPort)
        )
    }

    override fun getUidInfo(uid: Int): UidInfo {
        PackageCache.awaitLoadSync()

        if (uid <= 1000L) {
            val uidInfo = UidInfo()
            uidInfo.label = PackageCache.loadLabel("android")
            uidInfo.packageName = "android"
            return uidInfo
        }

        val packageNames = PackageCache.uidMap[uid.toInt()]
        if (!packageNames.isNullOrEmpty()) for (packageName in packageNames) {
            val uidInfo = UidInfo()
            uidInfo.label = PackageCache.loadLabel(packageName)
            uidInfo.packageName = packageName
            return uidInfo
        }

        error("unknown uid $uid")
    }

    fun getPackageInfo(packageName: String) = packageManager.getPackageInfo(
        packageName, if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
    )!!

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateNotificationChannels()
    }

    override val workManagerConfiguration: WorkConfiguration = WorkConfiguration.Builder()
        .setDefaultProcessName("${BuildConfig.APPLICATION_ID}:bg")
        .build()

    @SuppressLint("InlinedApi")
    companion object {

        @Volatile
        var started = false

        @SuppressLint("StaticFieldLeak")
        lateinit var application: SagerNet

        val isTv by lazy {
            uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        }

        val deviceStorage by lazy {
            if (Build.VERSION.SDK_INT < 24) application else DeviceStorageApp(application)
        }

        val configureIntent: (Context) -> PendingIntent by lazy {
            {
                PendingIntent.getActivity(
                    it,
                    0,
                    Intent(
                        application, MainActivity::class.java
                    ).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
            }
        }
        val activity by lazy { application.getSystemService<ActivityManager>()!! }
        val clipboard by lazy { application.getSystemService<ClipboardManager>()!! }
        val connectivity by lazy { application.getSystemService<ConnectivityManager>()!! }
        val notification by lazy { application.getSystemService<NotificationManager>()!! }
        val user by lazy { application.getSystemService<UserManager>()!! }
        val uiMode by lazy { application.getSystemService<UiModeManager>()!! }
        val wifi by lazy { application.getSystemService<WifiManager>()!! }
        val location by lazy { application.getSystemService<LocationManager>()!! }

        val packageInfo: PackageInfo by lazy { application.getPackageInfo(application.packageName) }
        val directBootSupported by lazy {
            Build.VERSION.SDK_INT >= 24 && try {
                app.getSystemService<DevicePolicyManager>()?.storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
            } catch (_: RuntimeException) {
                false
            }
        }

        val currentProfile get() = SagerDatabase.proxyDao.getById(DataStore.selectedProxy)

        fun getClipboardText(): String {
            return clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString() ?: ""
        }

        fun trySetPrimaryClip(clip: String) = try {
            clipboard.setPrimaryClip(ClipData.newPlainText(null, clip))
            true
        } catch (e: RuntimeException) {
            Logs.w(e)
            false
        }

        fun updateNotificationChannels() {
            if (Build.VERSION.SDK_INT >= 26) @RequiresApi(26) {
                notification.createNotificationChannels(
                    listOf(
                        NotificationChannel(
                            "service-vpn",
                            application.getText(R.string.service_vpn),
                            if (Build.VERSION.SDK_INT >= 28) NotificationManager.IMPORTANCE_MIN
                            else NotificationManager.IMPORTANCE_LOW
                        ),   // #1355
                        NotificationChannel(
                            "service-proxy",
                            application.getText(R.string.service_proxy),
                            NotificationManager.IMPORTANCE_LOW
                        ), NotificationChannel(
                            "service-subscription",
                            application.getText(R.string.service_subscription),
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                    )
                )
            }
        }

        fun reloadNetwork(network: Network?) {
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return
            val networkType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> "data"
            }
            Libcore.setNetworkType(networkType)
            var ssid: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ssid = DefaultNetworkListener.ssid
            } else {
                val wifiInfo = wifi.connectionInfo
                ssid = wifiInfo?.ssid
            }
            Libcore.setWifiSSID(ssid?.trim { it == '"' } ?: "")
        }

        fun startService() = ContextCompat.startForegroundService(
            application, Intent(application, SagerConnection.serviceClass)
        )

        fun reloadService() =
            application.sendBroadcast(Intent(Action.RELOAD).setPackage(application.packageName))

        fun stopService() =
            application.sendBroadcast(Intent(Action.CLOSE).setPackage(application.packageName))

    }

    override fun onLowMemory() {
        super.onLowMemory()
    }


/** Interface definition for a callback to be invoked when an app open ad is complete. */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    /** Inner class that loads and shows app open ads. */
    inner class AppOpenAdManager {
        private var isLoadingAd = false
        var isShowingAd = false
        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0


        /** Request an ad. */
        fun loadAd(context: Context) {
            println("HAMED_LOG_Start_Loading_AppOpen_Ad_0")
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            println("HAMED_LOG_Start_Loading_AppOpen_Ad_1")
            AppOpenAd.load(
                context, AdRepository.getAppOpenAdUnitID(), request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        println("HAMED_LOG_Ad was loaded.")
                        AdRepository.appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        println("HAMED_LOG_APP_OPEN: " + loadAdError.message)

                        isLoadingAd = false;
                    }
                })
        }

        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            Log.d(LOG_TAG, AdRepository.appOpenAd.toString())
//            return AdRepository.appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
            return AdRepository.appOpenAd != null
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                        println("HAMED_LOG_APP_OPEN_onShowAdComplete")
                    }
                }
            )
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                if (AdRepository.consentInformation.canRequestAds()) {
                    loadAd(activity)
                }
                return
            }

            Log.d(LOG_TAG, "Will show ad.")

            AdRepository.appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    /** Called when full screen content is dismissed. */
                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        AdRepository.appOpenAd = null
                        isShowingAd = false
                        Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT).show()

                        onShowAdCompleteListener.onShowAdComplete()
                        if (AdRepository.consentInformation.canRequestAds()) {
                            loadAd(activity)
                        }
                    }

                    /** Called when fullscreen content failed to show. */
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        AdRepository.appOpenAd = null
                        isShowingAd = false
                        Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                        Toast.makeText(activity, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT).show()

                        onShowAdCompleteListener.onShowAdComplete()
                        if (AdRepository.consentInformation.canRequestAds()) {
                            loadAd(activity)
                        }
                    }

                    /** Called when fullscreen content is shown. */
                    override fun onAdShowedFullScreenContent() {
                        Log.d(LOG_TAG, "onAdShowedFullScreenContent.")
                        Toast.makeText(activity, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT).show()
                    }
                }
            isShowingAd = true
            AdRepository.appOpenAd?.show(activity)
        }
    }
}