package com.example.macc_1916560

import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : BaseActivity() {

    // Step Counter
    private lateinit var stepTextView: TextView
//    private lateinit var sensorManager: SensorManager
//    private var stepSensor: Sensor? = null
//    private var steps = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // 👈 Force light mode

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        stepTextView = findViewById(R.id.stepCounterText)
        updateStepCountUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    100 // You can use any request code you like
                )
            }
        }

//        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
//        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

//        if (stepSensor == null) {
//            Toast.makeText(this, "No Step Counter Sensor !", Toast.LENGTH_SHORT).show()
//            //Log.e("StepSensor", "TYPE_STEP_DETECTOR not found on this device.")
//        }

        val welcomeTextView = findViewById<TextView>(R.id.welcomebackTextView)
        val lastWorkoutTextView = findViewById<TextView>(R.id.lastWorkoutTextView)

        val cameraButton = findViewById<Button>(R.id.CameraButton)
        cameraButton.setOnClickListener {
            val intent = Intent(this, PoseActivity::class.java)
            startActivity(intent)
        }

        val startWorkoutbutton = findViewById<Button>(R.id.startWorkoutBtn)
        startWorkoutbutton.setOnClickListener {
            val intent = Intent(this, WorkoutActivity::class.java)
            startActivity(intent)
        }

        val logoutButton = findViewById<Button>(R.id.logoutbtn)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            StepCounter.reset()
            finish()
        }

//        val isGuest = intent.getBooleanExtra("isGuest", false)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isGuest = prefs.getBoolean("isGuest", false)

        Log.d("MainActivity", "Guest flag received: $isGuest")

        if(isGuest){
            welcomeTextView.text = "Welcome, Guest!"
            lastWorkoutTextView.text = "No previous workouts found."

        }else {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val googleAccount = GoogleSignIn.getLastSignedInAccount(this)

            if (currentUser != null) {
//                val emailName = currentUser?.email?.substringBefore("@")
                val name = googleAccount?.displayName ?: currentUser?.displayName ?: "User"
                welcomeTextView.text = "Welcome, $name!"

                FirebaseFirestore.getInstance()
                    .collection("workouts")
                    .document(currentUser.uid)
                    .collection("userWorkouts")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val workout =
                                documents.first().toObject(WorkoutActivity.Workout::class.java)
                            val summary = "${workout.exercise}, " +
                                    "${workout.sets} sets x ${workout.reps} reps, " +
                                    "${workout.weight} kg," +
                                    " on ${
                                        workout.date?.toDate()?.let {
                                            SimpleDateFormat(
                                                "dd-MM-yyyy",
                                                Locale.getDefault()
                                            ).format(it)
                                        }
                                    } "
                            lastWorkoutTextView.text = summary
                        } else {
                            lastWorkoutTextView.text = "No previous workouts found."
                        }
                    }
                    .addOnFailureListener {
                        lastWorkoutTextView.text = "Failed to load last workout."
                    }
            } else {
                welcomeTextView.text = "Welcome!"
                lastWorkoutTextView.text = "Could not load user info."
            }
        }
    }

    override fun updateStepCountUI() {
        stepTextView.text = "Steps this session: ${StepCounter.getSteps()}"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}