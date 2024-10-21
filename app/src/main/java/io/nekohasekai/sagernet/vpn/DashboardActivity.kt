package io.nekohasekai.sagernet.vpn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.databinding.ActivityDashboardBinding
import io.nekohasekai.sagernet.databinding.ActivityForceUpdateBinding
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ui.ConfigurationFragment
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.vpn.interfaces.RewardedAdListener
import io.nekohasekai.sagernet.vpn.interfaces.VpnEventListener
import io.nekohasekai.sagernet.vpn.nav.MenuFragment
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.debugLog
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.appSetting
import io.nekohasekai.sagernet.vpn.repositories.AppRepository.recyclerView
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import io.nekohasekai.sagernet.vpn.repositories.UserRepository
import io.nekohasekai.sagernet.vpn.serverlist.ServersListFragment
import io.nekohasekai.sagernet.vpn.services.AdManagerService
import io.nekohasekai.sagernet.vpn.services.VpnService
import io.nekohasekai.sagernet.vpn.utils.InternetConnectionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : BaseThemeActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener,
    NavigationView.OnNavigationItemSelectedListener,
    VpnEventListener,
    RewardedAdListener {

    lateinit var binding: ActivityDashboardBinding
    private lateinit var powerIcon: LottieAnimationView
    private lateinit var ivAll: ImageView
    private lateinit var ivMtn: ImageView
    private lateinit var ivMci: ImageView
    private lateinit var stateTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var appTitle: TextView
    private lateinit var addTimeTextView: TextView
    private lateinit var tvSelectedServer: TextView
    private var timerRunning = false
    private var timeRemainingMillis: Long = 0
    private var ivAllClicked = true // Set IVall as clicked by default
    private var ivMtnClicked = false // Add a variable to track IVMTN click state
    private var ivMciClicked = false // Add a variable to track IVMCI click state
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdRepository.internetChecker = InternetConnectionChecker(this)
        AdRepository.appOpenAdManager.showAdIfAvailable(this)

        VpnService.addVpnEventListener(this)
        VpnService.initialize(this)

        AdManagerService.initialize(this)
        AdManagerService.loadRewardedAd(this)

        // load BannerAd and RewardedAd
        //AdRepository.loadBannerAd(this@DashboardActivity)
//        AdRepository.loadRewardedAd(this)

        AuthRepository.getUserAccountInfo()

        AuthRepository.getUserActiveServices().forEach {
            debugLog("getUserSubscriptionLinks: " + it.server_group + " - " + it.sublink)
        }

        // Ask user's permission for Notifications
        requestNotification()

        AppRepository.sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        val shareIcon = findViewById<ImageView>(R.id.ivShareIcon)
        val connection = SagerConnection(true)

        tvSelectedServer = findViewById<TextView>(R.id.tvSelectedServer)

        val tvDataLeft = findViewById<TextView>(R.id.tvDataLeft)
        tvDataLeft.text = getString(R.string.dataleft, AuthRepository.getSelectedService()?.remain_traffic)

        val clPremium = findViewById<ConstraintLayout>(R.id.clPremium)
        val tvPremium = findViewById<TextView>(R.id.tvPremium)
        clPremium.visibility = showForUpgradableServices()
        tvPremium.text = if (UserRepository.hasUpgradableService()) {
            getString(R.string.upgrade_service)
        } else {
            getString(R.string.premium)
        }
        clPremium.setOnClickListener {navigateToPremiumActivity()}

        // <DO NOT DELETE THIS COMMENT CODES>
        // Set an OnClickListener to MainActivity
        shareIcon.setOnClickListener { // Start the MainActivity
            val intent = Intent(this@DashboardActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Set onClickListener for shareIcon
//        shareIcon.setOnClickListener {
//            shareLinkWithMessage(AppRepository.ShareCustomMessage)
//        }

        // Define shareLinkWithMessage function outside of OnClickListener
        fun shareLinkWithMessage(message: String) {
            // Create an Intent with ACTION_SEND
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "$message\n" + AppRepository.ShareApplicationLink
                )
            }
            // Start the system's chooser to share the content
            startActivity(Intent.createChooser(sendIntent, "Share link with:"))
        }

        // Initialize the fragment container
        val fragmentContainer = findViewById<View>(R.id.flFragmentContainer)

//        val pingBtn = findViewById<ConstraintLayout>(R.id.clIconPing)
//        pingBtn.setOnClickListener {
//            AppRepository.urlTest(this)
//            showNotConnectedState()
//            stopTimer()
//        }

        // Find the NavMenuIcon ImageView and set an OnClickListener
        val navMenuIcon = findViewById<ImageView>(R.id.ivNavMenuIcon)
        navMenuIcon.setOnClickListener {
            // Create an instance of the NavMenuFragment
            val navMenuFragment = MenuFragment()

            // Get the FragmentManager and start a transaction
            val fragmentManager: FragmentManager = supportFragmentManager
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()

            // Replace the entire activity content with the NavMenuFragment
            transaction.replace(android.R.id.content, navMenuFragment)

            // Commit the transaction
            transaction.commit()
        }

        powerIcon = findViewById(R.id.laPulseButton)
        ivAll = findViewById(R.id.ivAll)
        ivMtn = findViewById(R.id.ivMtn)
        ivMci = findViewById(R.id.ivMci)
        stateTextView = findViewById(R.id.tvPowerState)
        timerTextView = findViewById(R.id.tvTimer)
        appTitle = findViewById(R.id.tvApplicationName)
        addTimeTextView = findViewById(R.id.tvAddTime)
        addTimeTextView.text = getString(R.string.plus_x_minutes, appSetting.freeVpnTimer)

        timerTextView.visibility = showForFreeUsers()
        addTimeTextView.visibility = showForFreeUsers()

        // Check if returning from a fragment
        if (savedInstanceState != null) {
            // Handle the initial state
            val initialState = savedInstanceState.getString("currentState", "Connect")
            stateTextView.text = initialState

            // Handle the fragment visibility
            val isFragmentVisible = savedInstanceState.getBoolean("isFragmentVisible", true)
            if (!isFragmentVisible) {
                fragmentContainer.visibility = View.INVISIBLE
            }

            // Handle IVall, IVMTN, and IVMCI click states
            ivAllClicked = savedInstanceState.getBoolean("ivAllClicked", true)
            ivMtnClicked = savedInstanceState.getBoolean("ivMtnClicked", false)
            ivMciClicked = savedInstanceState.getBoolean("ivMciClicked", false)

            updateIVAllIcon()
            updateIVMtnIcon()
            updateIVMciIcon()
        }

        // Ensure IVall is selected and fragmentContainer is visible when the activity starts
        fragmentContainer.visibility = if (ivAllClicked) View.VISIBLE else View.INVISIBLE
        if (ivAllClicked) {
            val fragment = ServersListFragment()
            val bundle = Bundle()
            bundle.putString("iconClicked", "IVAll") // Pass the clicked icon value to the fragment
            fragment.arguments = bundle
            val fragmentManager: FragmentManager = supportFragmentManager
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.flFragmentContainer, fragment)
            transaction.commit()
        }

        powerIcon.setOnClickListener {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                // Internet is connected, proceed with your code
                VpnService.toggleVpn()
            } else {
                // Internet is not connected, show a toast
                Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            }
        }

        binding.clIconPing.setOnClickListener {
            binding.clIconPing.visibility = View.INVISIBLE
            binding.pbPing.visibility = View.VISIBLE
            showNotConnectedState()
            stopTimer()
            lifecycleScope.launch {
                VpnService.silentUrlTestAsync()
            }
        }

        connection.connect(this, this)
        DataStore.configurationStore.registerChangeListener(this)
//        GroupManager.userInterface = GroupInterfaceAdapter(this)

        // Set an OnClickListener for IVall
        ivAll.setOnClickListener {
            ivAllClicked = !ivAllClicked // Toggle the IVall click state
            updateIVAllIcon() // Update the IVall icon
            // Show/hide the MyFragment based on the click state
            fragmentContainer.visibility = if (ivAllClicked) View.VISIBLE else View.INVISIBLE
            if (ivAllClicked) {
                AppRepository.filterServersByTag("all")
                val fragment = ServersListFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVAll") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.flFragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVMTN and IVMCI click states
            ivMtnClicked = false
            ivMciClicked = false
            updateIVMtnIcon() // Update the IVMTN icon
            updateIVMciIcon() // Update the IVMCI icon
        }

        // Set an OnClickListener for IVMTN
        ivMtn.setOnClickListener {
            ivMtnClicked = !ivMtnClicked // Toggle the IVMTN click state
            updateIVMtnIcon() // Update the IVMTN icon
            // Show/hide the MyFragment based on the click state
            fragmentContainer.visibility = if (ivMtnClicked) View.VISIBLE else View.INVISIBLE
            if (ivMtnClicked) {
                AppRepository.filterServersByTag("mtn")
                val fragment = ServersListFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVMTN") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.flFragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVall and IVMCI click states
            ivAllClicked = false
            ivMciClicked = false
            updateIVAllIcon() // Update the IVall icon
            updateIVMciIcon() // Update the IVMCI icon
        }

        // Set an OnClickListener for IVMCI
        ivMci.setOnClickListener {
            ivMciClicked = !ivMciClicked // Toggle the IVMCI click state
            updateIVMciIcon() // Update the IVMCI icon
            // Show/hide the MyFragment based on the click state
            fragmentContainer.visibility = if (ivMciClicked) View.VISIBLE else View.INVISIBLE
            if (ivMciClicked) {
                AppRepository.filterServersByTag("mci")
                val fragment = ServersListFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVMCI") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.flFragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVall and IVMTN click states
            ivAllClicked = false
            ivMtnClicked = false
            updateIVAllIcon() // Update the IVall icon
            updateIVMtnIcon() // Update the IVMTN icon
        }
    }

    private fun navigateToPremiumActivity() {
        val intent = Intent(this, PremiumActivity::class.java)
        startActivity(intent)
    }

    private fun updateIVAllIcon() {
        ivAll.setImageResource(if (ivAllClicked) R.drawable.ic_all_colorfull else R.drawable.ic_all_gray)
    }

    private fun updateIVMtnIcon() {
        ivMtn.setImageResource(if (ivMtnClicked) R.drawable.ic_mtn_irancell_colorfull else R.drawable.ic_mtn_irancell_gray)
    }

    private fun updateIVMciIcon() {
        ivMci.setImageResource(if (ivMciClicked) R.drawable.ic_mci_hamrahe_aval_colorfull else R.drawable.ic_mci_hamrahe_aval_gray)
    }

    private fun addMinutesToTimer() {
        val remainTime = AppRepository.sharedPreferences.getLong("remainingTime", 0)
        val minutes = appSetting.freeVpnTimer
        timeRemainingMillis = remainTime + (minutes * 60 * 1000L)
        AppRepository.sharedPreferences.edit().putLong("remainingTime", timeRemainingMillis).apply()
        startTimer()
    }

    private fun stopTimer() {
        timerRunning = false
        countDownTimer?.cancel()
    }

    private fun startTimer() {
        val initialTimeMillis = AppRepository.sharedPreferences.getLong("remainingTime", 0)

        countDownTimer = object : CountDownTimer(initialTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                AppRepository.sharedPreferences.edit().putLong("remainingTime", timeRemainingMillis).apply()
                updateTimerText(timeRemainingMillis)
            }

            override fun onFinish() {
                AppRepository.sharedPreferences.edit().remove("remainingTime").apply()
                VpnService.stopVpn()
            }
        }
        if(!timerRunning) {
            timerRunning = true
            countDownTimer?.start()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerText(remainedTime: Long) {
        var initialTimeMillis = AppRepository.sharedPreferences.getLong("remainingTime", 0)
        val minutes = (initialTimeMillis / 1000) / 60
        val seconds = (initialTimeMillis / 1000) % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = formattedTime
    }


    private fun showConnectingState() {
        timerTextView.visibility = View.INVISIBLE
        addTimeTextView.visibility = View.INVISIBLE
        powerIcon.setAnimation(R.raw.pulse_button_yellow)
        powerIcon.playAnimation()
        stateTextView.text = getString(R.string.connecting_state)
        powerIcon.isEnabled = false
    }

    private fun showConnectedState() {
        timerTextView.visibility = showForFreeUsers()
//        addTimeTextView.visibility = showForFreeUsers()
        powerIcon.setAnimation(R.raw.pulse_button_green)
        powerIcon.playAnimation()
        stateTextView.text = getString(R.string.connected_state)
        powerIcon.isEnabled = true
    }

    private fun showNotConnectedState() {
        timerTextView.visibility = View.INVISIBLE
        addTimeTextView.visibility = showForFreeUsers()
        powerIcon.setAnimation(R.raw.pulse_button_gray)
        powerIcon.playAnimation()
        stateTextView.text = getString(R.string.connect_state)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentState", stateTextView.text.toString())
        outState.putBoolean("isFragmentVisible", findViewById<View>(R.id.flFragmentContainer).visibility == View.VISIBLE)
        outState.putBoolean("ivAllClicked", ivAllClicked)
        outState.putBoolean("ivMtnClicked", ivMtnClicked)
        outState.putBoolean("ivMciClicked", ivMciClicked)
    }

    override fun onResume() {
        super.onResume()
        AdRepository.showAppOpenAd(this)
        if(DataStore.startedProfile > 0) {
            showConnectedState()
//            adManager.showRewardedAd()
//            AdRepository.showRewardedAd(this)
        } else {
            showNotConnectedState()
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state, msg, true)
    }

    override fun onServiceConnected(service: ISagerNetService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.SERVICE_MODE -> onBinderDied()
            Key.PROXY_APPS, Key.BYPASS_MODE, Key.INDIVIDUAL -> {
                if (VpnService.canStop) {
                    snackbar(getString(R.string.need_reload)).setAction(R.string.apply) {
                        SagerNet.reloadService()
                    }.show()
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//        if (item.isChecked) binding.drawerLayout.closeDrawers() else {
//            return displayFragmentWithId(item.itemId)
//        }
        return true
    }

    private fun changeState(
        state: BaseService.State,
        msg: String? = null,
        animate: Boolean = false,
    ) {
//        DataStore.serviceState = state
        if (state.toString() === "Connected") {
            AdManagerService.showRewardedAd()
//            AdRepository.showRewardedAd(this)
            AppRepository.isConnected = true
            val profile = SagerDatabase.proxyDao.getById(DataStore.selectedProxy)
            tvSelectedServer.text = profile?.displayName()
            showConnectedState()
        } else if(state.toString() === "Connecting") {
            showConnectingState()
        } else if(state.toString() === "Stopped") {
            AppRepository.isConnected = false
            showNotConnectedState()
            stopTimer()
        }
        VpnService.canStop = state.canStop
//        binding.fab.changeState(state, DataStore.serviceState, animate)
//        binding.stats.changeState(state)
//        if (msg != null) snackbar(getString(R.string.vpn_error, msg)).show()
    }

    fun stopService() {
        if (SagerNet.started) SagerNet.stopService()
    }

    override fun onStop() {
        super.onStop()
        val gson = Gson()
        val allServersInJson = gson.toJson(AppRepository.allServers)
        AppRepository.sharedPreferences.edit().putString("allServers", allServersInJson).apply()
    }

    private suspend fun setServerStatus(profile: ProxyEntity, ping: Int, status: Int, error: String?) {
        val serverName = profile.displayName()
        val countryCode = serverName.substring(serverName.length - 5, serverName.length).substring(0, 2).lowercase()
        val foundItem = AppRepository.allServers.find {
            it.name == AppRepository.getItemName(countryCode)
        }
        val foundSubItem = foundItem?.dropdownItems?.find { it.id == profile.id}
        foundSubItem?.status = status
        foundSubItem?.ping = ping
        foundSubItem?.error = error

        withContext(Dispatchers.Main) {
            val serverName = profile.displayName()
            val countryCode = serverName.substring(serverName.length - 5, serverName.length).substring(0, 2).lowercase()
            val foundItem = AppRepository.allServers.find {
                it.name == AppRepository.getItemName(countryCode)
            }
            val foundSubItem = foundItem?.dropdownItems?.find { it.id == profile.id}
            foundSubItem?.status = status
            foundSubItem?.ping = ping
            foundSubItem?.error = error
        }
    }

    fun getCurrentGroupFragment(): ConfigurationFragment.GroupFragment? {
        return try {
            supportFragmentManager.findFragmentByTag("f" + DataStore.selectedGroup) as ConfigurationFragment.GroupFragment?
        } catch (e: Exception) {
            debugLog(e.toString())
            null
        }
    }

    private fun requestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
    }

    // Handle the VPN stopped event
    override fun onVpnStopped() {
        // Do something when VPN is stopped
        AppRepository.clearAllItemsSelections()
        tvSelectedServer.text = ""
        debugLog("VPN was stopped, handling event in MainActivity")
    }

    // Handle the VPN started event
    override fun onVpnStarted() {
        // Do something when VPN is started
        debugLog("VPN was started, handling event in MainActivity")
    }

    // Handle the VPN server changed event
    override fun onVpnServerChanged(newProfileId: Long) {
        // Do something when VPN server is changed
        debugLog("VPN was changed to $newProfileId, handling event in MainActivity")
    }

    // Catch the reward event here
    override fun onUserEarnedReward(rewardItem: RewardItem) {
        addMinutesToTimer()
        debugLog("User_earned_the_reward")
    }

    override fun onPingTestFinished() {
        debugLog("onPingTestFinishedCalled")
//        AppRepository.refreshServersListView()
//        recyclerView.adapter?.notifyDataSetChanged()
        binding.pbPing.visibility = View.INVISIBLE
        binding.clIconPing.visibility = View.VISIBLE
    }


    override fun onDestroy() {
        VpnService.removeVpnEventListener(this)
        super.onDestroy()
    }

    fun showForFreeUsers():Int {
        return if (UserRepository.isFreeUser()) View.VISIBLE else View.INVISIBLE
    }

    fun showForUpgradableServices():Int {
        return if (UserRepository.hasUpgradableService()) View.VISIBLE else showForFreeUsers()
    }
}
