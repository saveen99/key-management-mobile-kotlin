package com.example.myapplication
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import com.example.myapplication.DatabaseHelper
import com.example.myapplication.R
import com.example.myapplication.User // Use your own User class, not Firebase
import androidx.work.* // Add this import for WorkManager



class AddKeyDashboard : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 100
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_key_dashboard)

        // Initialize WorkManager
        workManager = WorkManager.getInstance(applicationContext)



        // Initialize DatabaseHelper
        var dbHelper = DatabaseHelper(this)

        val addKeyButton = findViewById<Button>(R.id.add)

        // Check and request SMS permission if necessary
        checkSmsPermission()

        addKeyButton.setOnClickListener {
            val keyName = findViewById<EditText>(R.id.keyname).text.toString()
            val takenBy = findViewById<EditText>(R.id.takenby).text.toString()
            val pNumber = findViewById<EditText>(R.id.number).text.toString()

            // Validate phone number
            if (pNumber.length != 10 || !pNumber.all { it.isDigit() }) {
                Toast.makeText(this, "Please enter a valid 10-digit phone number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate takenBy name (only letters and spaces)
            if (!takenBy.all { it.isLetter() || it.isWhitespace() }) {
                Toast.makeText(this, "Taken by name can only contain letters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get current time for taken_time
            val currentTime = System.currentTimeMillis().toString()

            // Add user data to the database using your custom User class
            dbHelper.addKey(User(keyName, takenBy, pNumber, currentTime))
            Toast.makeText(this, "Key added successfully", Toast.LENGTH_SHORT).show()

            // Send SMS if phone number is valid
            sendSms(pNumber, "Key has been Taken")
            startSmsWorker(pNumber)

            // Clear input fields
            findViewById<EditText>(R.id.keyname).text.clear()
            findViewById<EditText>(R.id.takenby).text.clear()
            findViewById<EditText>(R.id.number).text.clear()
        }


    }

    private fun startSmsWorker(phoneNumber: String) {
        val data = Data.Builder()
            .putString("PHONE_NUMBER", phoneNumber)
            .build()

        // Schedule the worker to run every 2 hours, with an initial delay of 2 hours
        val smsWorkRequest = PeriodicWorkRequestBuilder<SmsWorker>(2, java.util.concurrent.TimeUnit.HOURS)
            .setInputData(data)
            .setInitialDelay(2, java.util.concurrent.TimeUnit.HOURS)
            .build()

        // Enqueue the work request
        workManager.enqueueUniquePeriodicWork(
            "SmsWorker_$phoneNumber",
            ExistingPeriodicWorkPolicy.REPLACE,
            smsWorkRequest
        )
    }


    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "SMS Failed to send, please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }





}
