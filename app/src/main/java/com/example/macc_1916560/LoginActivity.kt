package com.example.macc_1916560

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.macc_1916560.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        fun dpToPx(dp: Float): Float {
            return dp * resources.displayMetrics.density
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            } else {
                Toast.makeText(this, "Sign-In Failed or Cancelled", Toast.LENGTH_SHORT).show()
            }
        }


        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                signIn()
            }
        }

        val guestLoginButton = findViewById<Button>(R.id.guestLoginButton)
        guestLoginButton.setOnClickListener {
            signInAsGuest()
        }

        val leftTargetY = dpToPx(00f)
        val rightTargetX = dpToPx(80f)

        val leftArm = findViewById<ImageView>(R.id.leftArm)
        val rightArm = findViewById<ImageView>(R.id.rightArm)

        rightArm.post {
            rightArm.pivotX = rightArm.width / 2f
            rightArm.pivotY = rightArm.height / 1.2f
        }

        val leftToCenter = ObjectAnimator.ofFloat(leftArm,"translationX", leftTargetY)
        val rightToCenter = ObjectAnimator.ofFloat(rightArm,"translationX", rightTargetX)

        val moveTogether = AnimatorSet().apply {
            playTogether(leftToCenter, rightToCenter)
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }

        val flexUp = ObjectAnimator.ofFloat(rightArm, "rotation", 0f, -45f)
        val flexDown = ObjectAnimator.ofFloat(rightArm, "rotation", -45f, 0f)

        val flexSet = AnimatorSet().apply {
            playSequentially(flexUp, flexDown)
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        moveTogether.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                flexSet.start()
            }
        })

        moveTogether.start()
    }

    private fun signIn() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {

            val account = completedTask.getResult(ApiException::class.java)
            Log.d("LoginActivity", "firebaseAuthWithGoogle:" + account?.id)

            Log.d("LoginActivity", "Signed in as ${account?.displayName}")

            firebaseAuthWithGoogle(account?.idToken!!)
        } catch (e: ApiException) {

            Log.w("LoginActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d("LoginActivity", "signInWithCredential:success")
                    val user = auth.currentUser
                    Log.d("LoginActivity", "Firebase user: ${user?.displayName}")

                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("isGuest", false).apply()

                    Toast.makeText(baseContext, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("username", user?.displayName)
                    startActivity(intent)
                    finish()
                } else {

                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInAsGuest() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInAnonymously:success")
                    Toast.makeText(baseContext, "Welcome Guest", Toast.LENGTH_SHORT).show()

                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("isGuest", true).apply()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("isGuest", true)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w("LoginActivity", "signInAnonymously:failure", task.exception)
                    Toast.makeText(baseContext, "Guest login failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }



}

