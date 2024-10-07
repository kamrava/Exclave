package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.ArrayMap
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.group.RawUpdater
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.vpn.models.InfoApiResponse
import io.nekohasekai.sagernet.vpn.serverlist.ListItem
import io.nekohasekai.sagernet.vpn.serverlist.ListSubItem
import io.nekohasekai.sagernet.vpn.serverlist.MyAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.android.ext.android.get
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.*

object AppRepository {
    var LogTag: String = "HAMED_LOG"
    var appName: String = "UnitaVPN"
    private var subscriptionLink: String = "https://Apanel.holyip.workers.dev/link/9RTsfMryrGwgWZVb48eN?config=1"
    private var apiServersListUrl: String = "https://api.unitavpn.com/api/user/servers"
    private var baseUrl: String = "https://api.unitavpn.com/"
//    private var userLoginUrl: String = "https://unitavpn.com/api/client/token"
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
    private var isInternetConnected = true

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun getAllServer(): MutableList<ListItem> {
        val allServersString = sharedPreferences.getString("allServers", null)
        if(allServersString === null) {
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

    fun setSubscriptionLink(url: String) {
        subscriptionLink = url
    }

    fun getSubscriptionLink(): String {
        return subscriptionLink
    }

    private fun getBaseUrl(): String? {
        return baseUrl
    }

    fun getUrl(path: String): String {
        return getBaseUrl() + path
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

    fun getUserStateUrl(): String? {
        return userStateUrl
    }

    fun getUserResetPasswordUrl(): String {
        return userResetPasswordUrl
    }

    fun setUserResetPasswordUrl(url: String) {
        userResetPasswordUrl = url
    }

    fun setUserLoginUrl(path: String) {
        userStateUrl = path
    }

    fun setUserRegisterUrl(path: String) {
        userRegisterUrl = path
    }

    fun setUserCheckEmailAvailability(path: String) {
        userCheckEmailAvailability = path
    }

    fun setUserStateUrl(path: String) {
        userStateUrl = path
    }

    fun setPanelApiHeaderToken(token: String) {
        panelApiHeaderToken = token
    }

    fun getSettings() {
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
                    val gson = Gson()
                    val jsonObject = gson.fromJson(response.body!!.string(), JsonObject::class.java)
                    val baseUrl = jsonObject.get("baseUrl").asString
                    val userLoginUrl = jsonObject.get("userLoginUrl").asString
                    val userRegisterUrl = jsonObject.get("userRegisterUrl").asString
                    val userCheckEmailAvailability = jsonObject.get("userCheckEmailAvailability").asString
                    val userResetPasswordUrl = jsonObject.get("userResetPasswordUrl").asString
                    val userStateUrl = jsonObject.get("userStateUrl").asString
                    val panelApiHeaderToken = jsonObject.get("panelApiHeaderToken").asString
                    val versionCode = jsonObject.get("versionCode").asInt
                    val forceUnder = jsonObject.get("forceUnder").asInt

                    setBaseUrl(baseUrl)
                    setUserLoginUrl(userLoginUrl)
                    setUserRegisterUrl(userRegisterUrl)
                    setUserCheckEmailAvailability(userCheckEmailAvailability)
                    setUserResetPasswordUrl(userResetPasswordUrl)
                    setUserStateUrl(userStateUrl)
                    setPanelApiHeaderToken(panelApiHeaderToken)
                    setVersionCode(versionCode, forceUnder)
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
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val baseUrl = jsonObject.get("baseUrl").asString
                val userLoginUrl = jsonObject.get("userLoginUrl").asString
                val userRegisterUrl = jsonObject.get("userRegisterUrl").asString
                val userStateUrl = jsonObject.get("userStateUrl").asString
                val panelApiHeaderToken = jsonObject.get("panelApiHeaderToken").asString
                val versionCode = jsonObject.get("versionCode").asInt
                val forceUnder = jsonObject.get("forceUnder").asInt

                setBaseUrl(baseUrl)
                setUserLoginUrl(userLoginUrl)
                setUserRegisterUrl(userRegisterUrl)
                setUserStateUrl(userStateUrl)
                setPanelApiHeaderToken(panelApiHeaderToken)
                setVersionCode(versionCode, forceUnder)
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
        val client = OkHttpClient()
        val url = apiServersListUrl
        val userAccountInfo = AuthRepository.getUserAccountInfo()

        val request = Request.Builder()
            .url(url)
            .header("xmplus-authorization", getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + userAccountInfo.data.token)
            .get()
            .build()

        try {
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

    fun getRawServersConfigAsString(): String
    {
        var serversConfigString: MutableList<String> = mutableListOf()
        allServersRaw.entrySet().forEach { it ->
            it.value.asJsonArray.forEach { it ->
                serversConfigString.add(it.asJsonObject.get("config").asString)
            }
        }
        return serversConfigString.joinToString("\n")
    }


    fun getHttpRequest(url: String, formParams: HashMap<String, String>?, requestType: String = "GET"): Request {
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

        if(requestType == "GET") {
            request.get()
        } else if(requestType == "POST") {
            request.post(formBody)
        }

        return request.build()
    }

    fun flagNameMapper(countryCode: String): String
    {
        val countries = mapOf(
            "af" to "Afghanistan",
            "al" to "Albania",
            "dz" to "Algeria",
            "as" to "American Samoa",
            "ad" to "Andorra",
            "ao" to "Angola",
            "ai" to "Anguilla",
            "ag" to "Antigua and Barbuda",
            "ar" to "Argentina",
            "am" to "Armenia",
            "aw" to "Aruba",
            "au" to "Australia",
            "at" to "Austria",
            "az" to "Azerbaijan",
            "bs" to "Bahamas",
            "bh" to "Bahrain",
            "bd" to "Bangladesh",
            "bb" to "Barbados",
            "by" to "Belarus",
            "be" to "Belgium",
            "bz" to "Belize",
            "bj" to "Benin",
            "bm" to "Bermuda",
            "bt" to "Bhutan",
            "bo" to "Bolivia",
            "ba" to "Bosnia and Herzegovina",
            "bw" to "Botswana",
            "br" to "Brazil",
            "bn" to "Brunei",
            "bg" to "Bulgaria",
            "bf" to "Burkina Faso",
            "bi" to "Burundi",
            "cv" to "Cabo Verde",
            "kh" to "Cambodia",
            "cm" to "Cameroon",
            "ca" to "Canada",
            "ky" to "Cayman Islands",
            "cf" to "Central African Republic",
            "td" to "Chad",
            "cl" to "Chile",
            "cn" to "China",
            "co" to "Colombia",
            "km" to "Comoros",
            "cd" to "Congo, Democratic Republic of the",
            "cg" to "Congo, Republic of the",
            "cr" to "Costa Rica",
            "ci" to "Côte d'Ivoire",
            "hr" to "Croatia",
            "cu" to "Cuba",
            "cy" to "Cyprus",
            "cz" to "Czech Republic",
            "dk" to "Denmark",
            "dj" to "Djibouti",
            "dm" to "Dominica",
            "do" to "Dominican Republic",
            "ec" to "Ecuador",
            "eg" to "Egypt",
            "sv" to "El Salvador",
            "gq" to "Equatorial Guinea",
            "er" to "Eritrea",
            "ee" to "Estonia",
            "et" to "Ethiopia",
            "fk" to "Falkland Islands",
            "fo" to "Faroe Islands",
            "fj" to "Fiji",
            "fi" to "Finland",
            "fr" to "France",
            "gf" to "French Guiana",
            "pf" to "French Polynesia",
            "ga" to "Gabon",
            "gm" to "Gambia",
            "ge" to "Georgia",
            "de" to "Germany",
            "gh" to "Ghana",
            "gi" to "Gibraltar",
            "gr" to "Greece",
            "gd" to "Grenada",
            "gp" to "Guadeloupe",
            "gu" to "Guam",
            "gt" to "Guatemala",
            "gn" to "Guinea",
            "gw" to "Guinea-Bissau",
            "gy" to "Guyana",
            "ht" to "Haiti",
            "hn" to "Honduras",
            "hu" to "Hungary",
            "is" to "Iceland",
            "in" to "India",
            "id" to "Indonesia",
            "ir" to "Iran",
            "iq" to "Iraq",
            "ie" to "Ireland",
            "il" to "Israel",
            "it" to "Italy",
            "jm" to "Jamaica",
            "jp" to "Japan",
            "jo" to "Jordan",
            "kz" to "Kazakhstan",
            "ke" to "Kenya",
            "ki" to "Kiribati",
            "kp" to "North Korea",
            "kr" to "South Korea",
            "kw" to "Kuwait",
            "kg" to "Kyrgyzstan",
            "la" to "Laos",
            "lv" to "Latvia",
            "lb" to "Lebanon",
            "ls" to "Lesotho",
            "lr" to "Liberia",
            "ly" to "Libya",
            "li" to "Liechtenstein",
            "lt" to "Lithuania",
            "lu" to "Luxembourg",
            "mk" to "North Macedonia",
            "mg" to "Madagascar",
            "mw" to "Malawi",
            "my" to "Malaysia",
            "mv" to "Maldives",
            "ml" to "Mali",
            "mt" to "Malta",
            "mh" to "Marshall Islands",
            "mr" to "Mauritania",
            "mu" to "Mauritius",
            "mx" to "Mexico",
            "fm" to "Micronesia",
            "md" to "Moldova",
            "mc" to "Monaco",
            "mn" to "Mongolia",
            "me" to "Montenegro",
            "ma" to "Morocco",
            "mz" to "Mozambique",
            "mm" to "Myanmar",
            "na" to "Namibia",
            "nr" to "Nauru",
            "np" to "Nepal",
            "nl" to "Netherlands",
            "nc" to "New Caledonia",
            "nz" to "New Zealand",
            "ni" to "Nicaragua",
            "ne" to "Niger",
            "ng" to "Nigeria",
            "nu" to "Niue",
            "nf" to "Norfolk Island",
            "mp" to "Northern Mariana Islands",
            "no" to "Norway",
            "om" to "Oman",
            "pk" to "Pakistan",
            "pw" to "Palau",
            "pa" to "Panama",
            "pg" to "Papua New Guinea",
            "py" to "Paraguay",
            "pe" to "Peru",
            "ph" to "Philippines",
            "pn" to "Pitcairn Islands",
            "pl" to "Poland",
            "pt" to "Portugal",
            "pr" to "Puerto Rico",
            "qa" to "Qatar",
            "re" to "Réunion",
            "ro" to "Romania",
            "ru" to "Russia",
            "rw" to "Rwanda",
            "bl" to "Saint Barthélemy",
            "sh" to "Saint Helena",
            "kn" to "Saint Kitts and Nevis",
            "lc" to "Saint Lucia",
            "mf" to "Saint Martin",
            "pm" to "Saint Pierre and Miquelon",
            "vc" to "Saint Vincent and the Grenadines",
            "ws" to "Samoa",
            "sm" to "San Marino",
            "sa" to "Saudi Arabia",
            "sn" to "Senegal",
            "rs" to "Serbia",
            "sc" to "Seychelles",
            "sl" to "Sierra Leone",
            "sg" to "Singapore",
            "sx" to "Sint Maarten",
            "sk" to "Slovakia",
            "si" to "Slovenia",
            "sb" to "Solomon Islands",
            "so" to "Somalia",
            "za" to "South Africa",
            "ss" to "South Sudan",
            "es" to "Spain",
            "lk" to "Sri Lanka",
            "sd" to "Sudan",
            "sr" to "Suriname",
            "sz" to "Swaziland",
            "se" to "Sweden",
            "ch" to "Switzerland",
            "sy" to "Syria",
            "tw" to "Taiwan",
            "tj" to "Tajikistan",
            "tz" to "Tanzania",
            "th" to "Thailand",
            "tl" to "Timor-Leste",
            "tg" to "Togo",
            "tk" to "Tokelau",
            "to" to "Tonga",
            "tt" to "Trinidad and Tobago",
            "tn" to "Tunisia",
            "tr" to "Turkey",
            "tm" to "Turkmenistan",
            "tv" to "Tuvalu",
            "ug" to "Uganda",
            "ua" to "Ukraine",
            "ae" to "United Arab Emirates",
            "gb" to "United Kingdom",
            "us" to "United States",
            "uy" to "Uruguay",
            "uz" to "Uzbekistan",
            "vu" to "Vanuatu",
            "va" to "Vatican City",
            "ve" to "Venezuela",
            "vn" to "Vietnam",
            "wf" to "Wallis and Futuna",
            "eh" to "Western Sahara",
            "ye" to "Yemen",
            "zm" to "Zambia",
            "zw" to "Zimbabwe",
            "other" to "Others"
        )

//        return countries[countryCode].toString()
        val name = countries[countryCode] ?: "Unknown"

        // Don't add flag if country name is "Other"
        return if (name == "Others" || name == "Unknown") {
            name
        } else {
            val flag = countryCode.uppercase().map { char -> 0x1F1E6 - 'A'.code + char.code }
                .map { String(Character.toChars(it)) }
                .joinToString("")
            "$flag   $name"
        }
    }

    fun countryCodeMapper(countryName: String): String
    {
        val countries = mapOf(
            "Afghanistan" to "af",
            "Albania" to "al",
            "Algeria" to "dz",
            "American Samoa" to "as",
            "Andorra" to "ad",
            "Angola" to "ao",
            "Anguilla" to "ai",
            "Antigua and Barbuda" to "ag",
            "Argentina" to "ar",
            "Armenia" to "am",
            "Aruba" to "aw",
            "Australia" to "au",
            "Austria" to "at",
            "Azerbaijan" to "az",
            "Bahamas" to "bs",
            "Bahrain" to "bh",
            "Bangladesh" to "bd",
            "Barbados" to "bb",
            "Belarus" to "by",
            "Belgium" to "be",
            "Belize" to "bz",
            "Benin" to "bj",
            "Bermuda" to "bm",
            "Bhutan" to "bt",
            "Bolivia" to "bo",
            "Bosnia and Herzegovina" to "ba",
            "Botswana" to "bw",
            "Brazil" to "br",
            "Brunei" to "bn",
            "Bulgaria" to "bg",
            "Burkina Faso" to "bf",
            "Burundi" to "bi",
            "Cabo Verde" to "cv",
            "Cambodia" to "kh",
            "Cameroon" to "cm",
            "Canada" to "ca",
            "Cayman Islands" to "ky",
            "Central African Republic" to "cf",
            "Chad" to "td",
            "Chile" to "cl",
            "China" to "cn",
            "Colombia" to "co",
            "Comoros" to "km",
            "Congo, Democratic Republic of the" to "cd",
            "Congo, Republic of the" to "cg",
            "Costa Rica" to "cr",
            "Côte d'Ivoire" to "ci",
            "Croatia" to "hr",
            "Cuba" to "cu",
            "Cyprus" to "cy",
            "Czech Republic" to "cz",
            "Denmark" to "dk",
            "Djibouti" to "dj",
            "Dominica" to "dm",
            "Dominican Republic" to "do",
            "Ecuador" to "ec",
            "Egypt" to "eg",
            "El Salvador" to "sv",
            "Equatorial Guinea" to "gq",
            "Eritrea" to "er",
            "Estonia" to "ee",
            "Ethiopia" to "et",
            "Falkland Islands" to "fk",
            "Faroe Islands" to "fo",
            "Fiji" to "fj",
            "Finland" to "fi",
            "France" to "fr",
            "French Guiana" to "gf",
            "French Polynesia" to "pf",
            "Gabon" to "ga",
            "Gambia" to "gm",
            "Georgia" to "ge",
            "Germany" to "de",
            "Ghana" to "gh",
            "Gibraltar" to "gi",
            "Greece" to "gr",
            "Grenada" to "gd",
            "Guadeloupe" to "gp",
            "Guam" to "gu",
            "Guatemala" to "gt",
            "Guinea" to "gn",
            "Guinea-Bissau" to "gw",
            "Guyana" to "gy",
            "Haiti" to "ht",
            "Honduras" to "hn",
            "Hungary" to "hu",
            "Iceland" to "is",
            "India" to "in",
            "Indonesia" to "id",
            "Iran" to "ir",
            "Iraq" to "iq",
            "Ireland" to "ie",
            "Israel" to "il",
            "Italy" to "it",
            "Jamaica" to "jm",
            "Japan" to "jp",
            "Jordan" to "jo",
            "Kazakhstan" to "kz",
            "Kenya" to "ke",
            "Kiribati" to "ki",
            "North Korea" to "kp",
            "South Korea" to "kr",
            "Kuwait" to "kw",
            "Kyrgyzstan" to "kg",
            "Laos" to "la",
            "Latvia" to "lv",
            "Lebanon" to "lb",
            "Lesotho" to "ls",
            "Liberia" to "lr",
            "Libya" to "ly",
            "Liechtenstein" to "li",
            "Lithuania" to "lt",
            "Luxembourg" to "lu",
            "North Macedonia" to "mk",
            "Madagascar" to "mg",
            "Malawi" to "mw",
            "Malaysia" to "my",
            "Maldives" to "mv",
            "Mali" to "ml",
            "Malta" to "mt",
            "Marshall Islands" to "mh",
            "Mauritania" to "mr",
            "Mauritius" to "mu",
            "Mexico" to "mx",
            "Micronesia" to "fm",
            "Moldova" to "md",
            "Monaco" to "mc",
            "Mongolia" to "mn",
            "Montenegro" to "me",
            "Morocco" to "ma",
            "Mozambique" to "mz",
            "Myanmar" to "mm",
            "Namibia" to "na",
            "Nauru" to "nr",
            "Nepal" to "np",
            "Netherlands" to "nl",
            "New Caledonia" to "nc",
            "New Zealand" to "nz",
            "Nicaragua" to "ni",
            "Niger" to "ne",
            "Nigeria" to "ng",
            "Niue" to "nu",
            "Norfolk Island" to "nf",
            "Northern Mariana Islands" to "mp",
            "Norway" to "no",
            "Oman" to "om",
            "Pakistan" to "pk",
            "Palau" to "pw",
            "Panama" to "pa",
            "Papua New Guinea" to "pg",
            "Paraguay" to "py",
            "Peru" to "pe",
            "Philippines" to "ph",
            "Pitcairn Islands" to "pn",
            "Poland" to "pl",
            "Portugal" to "pt",
            "Puerto Rico" to "pr",
            "Qatar" to "qa",
            "Réunion" to "re",
            "Romania" to "ro",
            "Russia" to "ru",
            "Rwanda" to "rw",
            "Saint Barthélemy" to "bl",
            "Saint Helena" to "sh",
            "Saint Kitts and Nevis" to "kn",
            "Saint Lucia" to "lc",
            "Saint Martin" to "mf",
            "Saint Pierre and Miquelon" to "pm",
            "Saint Vincent and the Grenadines" to "vc",
            "Samoa" to "ws",
            "San Marino" to "sm",
            "Saudi Arabia" to "sa",
            "Senegal" to "sn",
            "Serbia" to "rs",
            "Seychelles" to "sc",
            "Sierra Leone" to "sl",
            "Singapore" to "sg",
            "Sint Maarten" to "sx",
            "Slovakia" to "sk",
            "Slovenia" to "si",
            "Solomon Islands" to "sb",
            "Somalia" to "so",
            "South Africa" to "za",
            "South Sudan" to "ss",
            "Spain" to "es",
            "Sri Lanka" to "lk",
            "Sudan" to "sd",
            "Suriname" to "sr",
            "Swaziland" to "sz",
            "Sweden" to "se",
            "Switzerland" to "ch",
            "Syria" to "sy",
            "Taiwan" to "tw",
            "Tajikistan" to "tj",
            "Tanzania" to "tz",
            "Thailand" to "th",
            "Timor-Leste" to "tl",
            "Togo" to "tg",
            "Tokelau" to "tk",
            "Tonga" to "to",
            "Trinidad and Tobago" to "tt",
            "Tunisia" to "tn",
            "Turkey" to "tr",
            "Turkmenistan" to "tm",
            "Tuvalu" to "tv",
            "Uganda" to "ug",
            "Ukraine" to "ua",
            "United Arab Emirates" to "ae",
            "United Kingdom" to "gb",
            "United States" to "us",
            "Uruguay" to "uy",
            "Uzbekistan" to "uz",
            "Vanuatu" to "vu",
            "Vatican City" to "va",
            "Venezuela" to "ve",
            "Vietnam" to "vn",
            "Wallis and Futuna" to "wf",
            "Western Sahara" to "eh",
            "Yemen" to "ye",
            "Zambia" to "zm",
            "Zimbabwe" to "zw",
            "Others" to "other"
        )
        return countries[countryName].toString()
    }

    fun refreshServersListView()
    {
        val adapter = MyAdapter(getAllServer()) { }
        recyclerView.adapter = adapter
    }

    fun resetAllSubItemsStatus()
    {
        var servers = allServersOriginal
        servers.forEach { item ->
            item.dropdownItems.forEach { subItem ->
                subItem.isSelected = false
            }
        }
        allServers = servers.filter { element -> element in allServers }.toMutableList()
    }

    fun filterServersByTag(tag: String): Unit
    {
        filterServersBy = tag
        var servers = allServersOriginal
        if(tag === "all") {
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

    fun setServerPing(serverId: Long, ping: Int, status: Int): ArrayMap<String, String> {
        val arrayMap: ArrayMap<String, String> = ArrayMap()
        arrayMap["countryCode"] = ""
        arrayMap["serverName"] = ""
        allServers.forEach { item ->
            item.dropdownItems.forEach{
                if(it.id == serverId) {
                    arrayMap["countryCode"] = countryCodeMapper(item.name)
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
            if(response == 200) {
                val serversString = getRawServersConfigAsString()
                val proxies = RawUpdater.parseRaw(serversString)
                if(!proxies.isNullOrEmpty()) {
                    import(proxies, context)
                }
            }
            "Finished"
        }
    }

    @SuppressLint("DiscouragedApi")
    suspend fun import(proxies: List<AbstractBean>, context: Context) {
        removeAllProfiles()
        val targetId = DataStore.selectedGroupForImport()
        var counter = 0
        allServers = mutableListOf()
        allServers.clear()
        allServersOriginal = mutableListOf()
        allServersOriginal.clear()
        setAllServer(allServers)
        allServersRaw.entrySet().forEach { entry ->
            val serverSubItems: MutableList<ListSubItem> = mutableListOf()
            val countryCode = entry.key
            val resourceName = "ic_${countryCode}_flag"
            val countryName = flagNameMapper(countryCode)
            entry.value.asJsonArray.forEach { it ->
                val profile = ProfileManager.createProfile(targetId, proxies[counter])
                val serverId = it.asJsonObject.get("id").asInt
                val tagsArray = it.asJsonObject.getAsJsonArray("tags")
                val tags = Array(tagsArray.size()) { tagsArray[it].asString }
                serverSubItems.add(
                    ListSubItem(profile.id, serverId, it.asJsonObject.get("name").asString, profile.status, profile.error, profile.ping, tags = tags)
                )
                counter++;
            }
            allServers.add(
                ListItem(
                    countryName,
                    serverSubItems,
                    iconResId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
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
}
