package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityRegisterBinding
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import io.nekohasekai.sagernet.vpn.repositories.SocialAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class RegisterActivity : BaseThemeActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var passwordEditText: EditText
    private lateinit var passwordToggle: ImageView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Set the Facebook app client token
        FacebookSdk.setClientToken("30d63da7ac404d3a92fe9c04a1baf590")

        // Initialize the Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)

        binding.txtEmail.setText("1mytestuser@gmail.com")
        binding.txtPassword.setText("12345678")

        SocialAuthRepository.firebaseAuth = FirebaseAuth.getInstance()

        // Facebook SignIn
        callbackManager = CallbackManager.Factory.create()

        // Set up a callback for Facebook Login
        SocialAuthRepository.facebookLoginManager = LoginManager.getInstance()

        SocialAuthRepository.facebookLoginManager.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                // Successfully logged in with Facebook, now authenticate with Firebase
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                // User canceled the Facebook login
            }

            override fun onError(error: FacebookException) {
                // Error occurred during Facebook login
            }
        })

        // Google SignIn
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("673001878781-nc1vjdc36gjab2ma4q3ropog3ccha9en.apps.googleusercontent.com") // Your Web Client ID from Firebase Console
            .requestEmail()
            .build()

        SocialAuthRepository.googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        binding.LLGoogleRegister.setOnClickListener {
            val signInIntent = SocialAuthRepository.googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.tvGoogleRegister.setOnClickListener {
            binding.LLGoogleRegister.performClick()
        }

        binding.LLFacebookRegister.setOnClickListener {
            AccessToken.setCurrentAccessToken(null)
            SocialAuthRepository.facebookLoginManager.logInWithReadPermissions(this, listOf("email", "public_profile"))
        }

        binding.tvFacebookRegister.setOnClickListener {
            binding.LLFacebookRegister.performClick()
        }

        binding.tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.txtEmail.text.toString()
            val password = binding.txtPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                // Change button text
                binding.btnRegister.text = getString(R.string.Checking)
                // Show progress bar
                binding.progressBarLogin.visibility = View.VISIBLE

                // Perform register asynchronously
                GlobalScope.launch(Dispatchers.IO) {
                    // Check if Email is available
                    checkEmailAvailability(email, password)

                    // Update UI on the main thread after login completes
                    withContext(Dispatchers.Main) {
                        // Revert button text and hide progress bar
                        binding.btnRegister.text = getString(R.string.register)
                        binding.progressBarLogin.visibility = View.GONE
                    }
                }
            } else {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_LONG).show()
            }
        }

        // Initialize passwordEditText and passwordToggle
        passwordEditText = findViewById(R.id.txtPassword)
        passwordToggle = findViewById(R.id.passwordToggle)

        // Set an initial icon for password visibility
        passwordToggle.setImageResource(R.drawable.ic_password_hide)

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val inputType = if (isPasswordVisible) {
                // Show password
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Hide password
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordEditText.inputType = inputType

            // Set the passwordToggle icon based on visibility
            passwordToggle.setImageResource(
                if (isPasswordVisible) R.drawable.ic_password_show
                else R.drawable.ic_password_hide
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                println("Google Sign-In Error: " + e.message)
            }
        }
    }

    private fun performRegister(email: String, password: String) {
        runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    val responseCode = AuthRepository.register(email, password)
                    when (responseCode) {
                        200 -> {
                            runOnUiThread {
                                binding.tvValidationError.visibility = View.INVISIBLE
                                navigateToVerifyActivity(email, password)
                            }
                        }
                        409 -> {
                            runOnUiThread {
                                binding.tvValidationError.text = "This Email is already registered"
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                        422 -> {
                            runOnUiThread {
                                binding.tvValidationError.text = AuthRepository.getLastValidationError()
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                        else -> {
                            runOnUiThread {
                                binding.tvValidationError.text = "Something is wrong!"
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
            }
        }
    }

    private fun checkEmailAvailability(email: String, password: String) {
        runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    val responseCode = AuthRepository.checkEmailAvailabilityAndSendCode(email)
                    when (responseCode) {
                        200 -> {
                            runOnUiThread {
                                binding.tvValidationError.visibility = View.INVISIBLE
                                navigateToVerifyActivity(email, password)
                            }
                        }
                        409 -> {
                            runOnUiThread {
                                binding.tvValidationError.text = "Email is not available!"
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                        422 -> {
                            runOnUiThread {
                                binding.tvValidationError.text = AuthRepository.getLastValidationError()
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                        else -> {
                            runOnUiThread {
                                binding.tvValidationError.text = "Something is wrong!"
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        SocialAuthRepository.firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign-in success, start ProfileActivity
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                } else {
                    // If sign-in fails, display a message to the user
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential: AuthCredential = FacebookAuthProvider.getCredential(token.token)

        // Sign in to Firebase with the Facebook credential
        SocialAuthRepository.firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firebase authentication successful
                    val user = SocialAuthRepository.firebaseAuth.currentUser
                    if (user != null) {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    // Firebase authentication failed
                    // Handle the error
                }
            }
    }

    private fun navigateToVerifyActivity(email: String, password: String) {
        val intent = Intent(this, EmailVerify::class.java)
        AuthRepository.setUserEmail(email)
        intent.putExtra("email", email)
        intent.putExtra("password", password)
        startActivity(intent)
        finish()
    }
}
