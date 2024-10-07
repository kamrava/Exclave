package io.nekohasekai.sagernet.vpn.repositories

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.nekohasekai.sagernet.vpn.models.InfoApiResponse
import io.nekohasekai.sagernet.vpn.models.Service
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.android.ext.android.get
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AuthRepository : KoinComponent {
    private var token: String? = null
    private lateinit var email: String
    private lateinit var lastValidationError: String
    private lateinit var userAccountInfo: InfoApiResponse
    private val infoApiResponse: InfoApiResponse by inject()

    private fun setUserToken(data: String) {
        AppRepository.sharedPreferences.edit().putString("userToken", data).apply()
        token = data
    }

    fun isUserAlreadyLogin(): Boolean {
        val userAccountInfo = getUserAccountInfo()
        return userAccountInfo.data.token != null
    }

    fun clearUserToken() {
        AppRepository.sharedPreferences.edit().remove("userToken").apply()
        token = null
    }

    fun clearUserInfo() {
        AppRepository.sharedPreferences.edit().remove("userAccountInfo").apply()
        token = null
    }

    private fun setLastValidationError(data: String) {
        lastValidationError = data
    }
    fun setUserEmail(data: String) {
        email = data
    }

    fun getUserToken(): String? {
        return AppRepository.sharedPreferences.getString("userToken", null)
    }
    fun getLastValidationError(): String? {
        return lastValidationError
    }

    private fun setUserAccountInfo(userAccountInfoPram: InfoApiResponse) {
        userAccountInfo = userAccountInfoPram
       val userAccountInfoJsonString : String = Gson().toJson(userAccountInfoPram)
        AppRepository.sharedPreferences.edit().putString("userAccountInfo", userAccountInfoJsonString).apply()
    }

    fun getUserAccountInfo(): InfoApiResponse {
        val userAccountDataString = AppRepository.sharedPreferences.getString("userAccountInfo", null)
        AppRepository.debugLog("STEP_100: " + userAccountDataString.toString())
        if (userAccountDataString == null) {
            return infoApiResponse;
        }
        userAccountDataString?.let {
            userAccountInfo = Gson().fromJson(userAccountDataString, InfoApiResponse::class.java)
        }
        AppRepository.debugLog("STEP_101: ")
        return userAccountInfo
    }

    fun getUserEmail(): String {
        return userAccountInfo.data.email
    }

    fun getUserActiveServices(): List<Service> {
        val services = userAccountInfo.data.services.filter {
            it.status == "فعال"
        }

        services.sortedByDescending { it.server_group  }

        return services
    }

    fun token(): Int {
        val client = OkHttpClient()
        val url = AppRepository.getUserLoginUrl()

        val request = Request.Builder()
            .url(url)
            .header("xmplus-authorization", AppRepository.getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val dataJsonObject = gson.fromJson(jsonObject.get("data").asJsonObject, JsonObject::class.java)
                val token = dataJsonObject.get("token").asString
                AppRepository.debugLog("SetUserToken")
                setUserToken(token)

            }
            return response.code;
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1;
        }
    }

    fun login(email: String, password: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        requestBuilder.add("passwd", password)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserLoginUrl()

        val request = Request.Builder()
            .url(url)
            .header("xmplus-authorization", AppRepository.getPanelApiHeaderToken())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val apiResponse = gson.fromJson(responseBody, InfoApiResponse::class.java)
                setUserAccountInfo(apiResponse)

//                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
//                val dataJsonObject = gson.fromJson(jsonObject.get("data").asJsonObject, JsonObject::class.java)
//                val token = dataJsonObject.get("token").asString
//                setUserToken(token)

            }
            return response.code;
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1;
        }
    }

    fun register(email: String, password: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        requestBuilder.add("passwd", password)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserRegisterUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if(response.code == 422) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val errors = gson.fromJson(jsonObject.get("errors").asJsonObject, JsonObject::class.java)
                setLastValidationError(errors.asJsonObject.entrySet().first().value.asString)
            }
            return response.code
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1
        }
    }

    fun checkEmailAvailabilityAndSendCode(email: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserCheckEmailAvailabilityUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if(response.code == 422) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val errors = gson.fromJson(jsonObject.get("errors").asJsonObject, JsonObject::class.java)
                setLastValidationError(errors.asJsonObject.entrySet().first().value.asString)
            }
            return response.code
        } catch (e: Exception) {
            println("Check email availability request failed: ${e.message}")
            return -1
        }
    }

    fun sendEmailVerificationCode(email: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserCheckEmailAvailabilityUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if(response.code == 422) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val errors = gson.fromJson(jsonObject.get("errors").asJsonObject, JsonObject::class.java)
                setLastValidationError(errors.asJsonObject.entrySet().first().value.asString)
            }
            return response.code
        } catch (e: Exception) {
            println("Check email availability request failed: ${e.message}")
            return -1
        }
    }

    fun verify(email: String, password: String, verifyCode: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        requestBuilder.add("passwd", password)
        requestBuilder.add("code", verifyCode)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserVerifyUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val gson = Gson()
            val apiResponse = gson.fromJson(responseBody, InfoApiResponse::class.java)
            setUserAccountInfo(apiResponse)
            return response.code
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1
        }
    }

    fun sendResetPasswordEmail(email: String): Int {
        val requestBuilder = FormBody.Builder()
        requestBuilder.add("email", email)
        val formBody = requestBuilder.build()

        val client = OkHttpClient()
        val url = AppRepository.getUserResetPasswordUrl()

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            return response.code
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            return -1
        }
    }
}
