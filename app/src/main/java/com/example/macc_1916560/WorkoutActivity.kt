package com.example.macc_1916560

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class WorkoutActivity : BaseActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI Components
    private lateinit var bodyPartSpinner: Spinner
    private lateinit var exerciseSpinner: Spinner
    private lateinit var weightEditText: EditText
    private lateinit var repsEditText: EditText
    private lateinit var setsEditText: EditText
    private lateinit var saveWorkoutButton: Button
    private lateinit var returnHomeButton: Button
    private lateinit var workoutTable: TableLayout
    private var workoutListener: ListenerRegistration? = null
    private lateinit var clearAllButton: Button

    private val exercisesMap = mapOf(
        "Chest" to listOf("Bench Press", "Push-up", "Chest Fly", "Incline Press"),
        "Back" to listOf("Pull-up", "Lat Pulldown", "Deadlift", "Bent-over Row"),
        "Legs" to listOf("Squat", "Lunges", "Leg Press", "Leg Curl"),
        "Arms" to listOf("Bicep Curl", "Tricep Dip", "Hammer Curl", "Skull Crushers"),
        "Shoulders" to listOf("Overhead Press", "Lateral Raise", "Front Raise", "Shrug")
    )

    override fun updateStepCountUI() {
//        val stepTextView = findViewById<TextView>(R.id.stepCounterText)
//        stepTextView.text = "Steps this session: ${StepCounter.getSteps()}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupSpinners()
        setupButtons()

        loadWorkouts()

    }

    override fun onStart(){
        super.onStart()
        setupRealTimeUpdates()
    }

    override fun onStop(){
        super.onStop()
        workoutListener?.remove()
    }

    private fun setupRealTimeUpdates() {
        val currentUser = auth.currentUser ?: return

        workoutListener?.remove()


        val query = db.collection("workouts")
            .document(currentUser.uid)
            .collection("userWorkouts")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)//asking for last 10 workouts


        workoutListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                showError("Error loading workouts: ${error.message}")
                return@addSnapshotListener
            }

            while (workoutTable.childCount > 0) {
                workoutTable.removeViewAt(0)
            }

            if (snapshot == null || snapshot.isEmpty) {
                showEmptyState()
                return@addSnapshotListener
            }


            for (document in snapshot.documents) {
                val workout = document.toObject(Workout::class.java)
                if (workout != null) {
                    addWorkoutToTable(workout, document.id)
                }
            }
        }
    }

    private fun initializeViews() {
        bodyPartSpinner = findViewById(R.id.bodypartSpinner)
        exerciseSpinner = findViewById(R.id.exerciseSpinner)
        weightEditText = findViewById(R.id.weightEditText)
        repsEditText = findViewById(R.id.repsEditText)
        setsEditText = findViewById(R.id.setsEditText)
        saveWorkoutButton = findViewById(R.id.saveWorkoutButton)
        returnHomeButton = findViewById(R.id.returnHomeButton)
        workoutTable = findViewById(R.id.workoutTable)
        clearAllButton = findViewById(R.id.clearAllButton)
    }

    private fun setupSpinners() {
        val bodyParts = exercisesMap.keys.toList()
        val bodyPartAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bodyParts)
        bodyPartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bodyPartSpinner.adapter = bodyPartAdapter

        bodyPartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBodyPart = bodyParts[position]
                val exercises = exercisesMap[selectedBodyPart] ?: emptyList()

                val exerciseAdapter = ArrayAdapter(
                    this@WorkoutActivity,
                    android.R.layout.simple_spinner_item,
                    exercises
                )
                exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                exerciseSpinner.adapter = exerciseAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        saveWorkoutButton.setOnClickListener {
            saveWorkout()
        }

        returnHomeButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        clearAllButton.setOnClickListener {
            clearAllWorkouts()
        }
    }

    private fun saveWorkout() {

        val bodyPart = bodyPartSpinner.selectedItem.toString()
        val exercise = exerciseSpinner.selectedItem.toString()
        val weight = weightEditText.text.toString().toDoubleOrNull()
        val reps = repsEditText.text.toString().toIntOrNull()
        val sets = setsEditText.text.toString().toIntOrNull()


        when {
            weight == null -> showError("Please enter a valid weight")
            reps == null -> showError("Please enter valid reps")
            sets == null -> showError("Please enter valid sets")
            else -> {

                val workout = hashMapOf(
                    "bodyPart" to bodyPart,
                    "exercise" to exercise,
                    "weight" to weight,
                    "reps" to reps,
                    "sets" to sets,
                    "date" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "dateString" to getCurrentDateTime()
                )


                val currentUser = auth.currentUser
                if (currentUser == null) {
                    showError("User not authenticated")
                    return
                }

                // Save to Firestore
                db.collection("workouts")
                    .document(currentUser.uid)
                    .collection("userWorkouts")
                    .add(workout)
                    .addOnSuccessListener {
                        showSuccess("Workout saved successfully!")
                        //clearInputs()
                    }
                    .addOnFailureListener { e ->
                        showError("Failed to save workout: ${e.message}")
                    }
            }
        }
    }

    private fun loadWorkouts() {
        while (workoutTable.childCount > 0) {
            workoutTable.removeViewAt(0)
        }

        val currentUser = auth.currentUser ?: return

        db.collection("workouts")
            .document(currentUser.uid)
            .collection("userWorkouts")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val workout = document.toObject(Workout::class.java)
                    addWorkoutToTable(workout, document.id)
                }
            }
            .addOnFailureListener { e ->
                showError("Failed to load workouts: ${e.message}")
            }
    }

    private fun addWorkoutToTable(workout: Workout, documentId: String) {
        val row = TableRow(this).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )

            val bgColor = if (workoutTable.childCount % 2 == 0) {
                ContextCompat.getColor(this@WorkoutActivity, R.color.very_light_grey)
            } else {
                ContextCompat.getColor(this@WorkoutActivity, R.color.white)
            }
            setBackgroundColor(bgColor)

        }

        createTableCell(workout.exercise).also { row.addView(it) }
        createTableCell(workout.reps.toString()).also { row.addView(it) }
        createTableCell(workout.sets.toString()).also { row.addView(it) }
        createTableCell("${workout.weight} kg").also { row.addView(it) }

        val formattedDate = workout.date?.toDate()?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
        } ?: "Unknown Date"
        createTableCell(formattedDate).also { row.addView(it) }

        workoutTable.addView(row)
    }

    private fun clearAllWorkouts() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("User not authenticated")
            return
        }

        val userWorkoutsRef = db.collection("workouts")
            .document(currentUser.uid)
            .collection("userWorkouts")

        userWorkoutsRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    showError("No workouts to delete")
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        showSuccess("All workouts deleted")
                        loadWorkouts()
                    }
                    .addOnFailureListener { e ->
                        showError("Failed to delete workouts: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showError("Error accessing workouts: ${e.message}")
            }
    }


    private fun createTableCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
        }
    }

    private fun showEmptyState() {
        val row = TableRow(this)
        val emptyText = TextView(this).apply {
            text = "No workouts saved yet"
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER
        }
        row.addView(emptyText)
        workoutTable.addView(row)
    }

    private fun clearInputs() {
        weightEditText.text.clear()
        repsEditText.text.clear()
        setsEditText.text.clear()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    data class Workout(
        val bodyPart: String = "",
        val exercise: String = "",
        val weight: Double = 0.0,
        val reps: Int = 0,
        val sets: Int = 0,
        val date: com.google.firebase.Timestamp? = null,
        val dateString: String = ""
    )
}