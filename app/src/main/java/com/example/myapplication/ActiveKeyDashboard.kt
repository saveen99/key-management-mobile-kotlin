package com.example.myapplication

import Handover
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActiveKeyDashboard : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 100
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_key_dashboard)

        workManager = WorkManager.getInstance(applicationContext)

        // Check and request SMS permission if necessary
        checkSmsPermission()

        dbHelper = DatabaseHelper(this)

        // Get the LinearLayout where buttons will be added dynamically
        val keyLayout: LinearLayout = findViewById(R.id.key_layout)

        // Fetch all available keys
        val keys = dbHelper.getAllKeys()

        // Dynamically create a button for each key
        for (key in keys) {
            val button = Button(this)

            // Create layout parameters for the button with margins
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150 // Height of the button
            )
            layoutParams.setMargins(16, 16, 16, 16) // Set left, top, right, bottom margins
            button.layoutParams = layoutParams

            // Set the button background to the drawable with rounded corners
            button.background = ContextCompat.getDrawable(this, R.drawable.rounded_button)

            // Set button text, text color, and text size
            button.text = key
            button.setTextColor(ContextCompat.getColor(this, R.color.white)) // Change text color
            button.textSize = 18f // Change text size to 18sp

            // Set an OnClickListener for each button
            button.setOnClickListener {
                showKeyDetailsPopup(key)
            }

            // Add the button to the layout
            keyLayout.addView(button)
        }
    }

    private fun showKeyDetailsPopup(keyName: String) {
        // Inflate the custom layout for the popup
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.activity_alert_dialog, null)
        // Find Quick Dial button in the popup layout
        val quickDialButton: Button = popupView.findViewById(R.id.btn_quickDial)

        // Create the AlertDialog
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(popupView)
            .create()

        // Find input fields in the popup
        val editKeyName: TextView = popupView.findViewById(R.id.editKeyName)
        val editTakenBy: EditText = popupView.findViewById(R.id.editTakenBy)
        val editPhoneNumber: EditText = popupView.findViewById(R.id.editPhoneNumber)
        val editTakenTime: TextView = popupView.findViewById(R.id.editTakenTime)
        val saveButton: Button = popupView.findViewById(R.id.saveButton)
        val handoverButton: Button = popupView.findViewById(R.id.handoverButton)
        val deleteButton: Button = popupView.findViewById(R.id.deleteButton)

        // Fetch key details from the database
        val user = dbHelper.getUser(keyName)
        user?.let {
            editKeyName.text = it.key_name
            editTakenBy.setText(it.taken_by)
            editPhoneNumber.setText(it.p_number)
            editTakenTime.text = it.taken_time
        }

        // Handle save button click
        saveButton.setOnClickListener {
            val phoneNumber = editPhoneNumber.text.toString()

            // Validate that the phone number contains exactly 10 digits
            if (phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }) {
                val updatedUser = User(
                    editKeyName.text.toString(),
                    editTakenBy.text.toString(),
                    phoneNumber,
                    editTakenTime.text.toString()
                )

                dbHelper.updateUser(updatedUser)
                dialogBuilder.dismiss()
            } else {
                // Show an error message
                editPhoneNumber.error = "Please enter a valid 10-digit phone number."
            }
        }


        // Handle handover button click
        handoverButton.setOnClickListener {
            val phoneNumber = editPhoneNumber.text.toString()

            if (phoneNumber.isNotEmpty()) {
                sendSms(phoneNumber, "Handover completed")
                workManager.cancelUniqueWork("SmsWorker_$phoneNumber") // Stop periodic SMS
            } else {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }

            // Get the current time for handover_time
            val currentTime = SimpleDateFormat("yyyy-MM-dd | HH:mm:ss", Locale.getDefault()).format(
                Date()
            )

            val handover = Handover(
                editKeyName.text.toString(),
                editTakenBy.text.toString(),
                phoneNumber,
                editTakenTime.text.toString(),
                handover_time = currentTime  // Set the current time for handover_time
            )

            dbHelper.addHandover(handover)
            refreshKeyList()
            dialogBuilder.dismiss()
        }

        // Set Quick Dial button click listener
        quickDialButton.setOnClickListener {
            val phoneNumber = editPhoneNumber.text.toString()

            if (phoneNumber.isNotEmpty()) {
                // Intent to open the dialer with the phone number
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(dialIntent)
            } else {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle delete button click
        deleteButton.setOnClickListener {
            // Create an AlertDialog for confirmation
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Deletion")
            builder.setMessage("Are you sure you want to delete this key?")

            builder.setPositiveButton("Yes") { dialog, which ->
                // Delete the user from the database
                if (dbHelper.deleteUser(keyName)) {
                    Toast.makeText(this, "Key deleted successfully", Toast.LENGTH_SHORT).show()
                    refreshKeyList() // Refresh the UI to update the list
                } else {
                    Toast.makeText(this, "Failed to delete key", Toast.LENGTH_SHORT).show()
                }
                dialogBuilder.dismiss() // Dismiss the dialog after the action
            }

            builder.setNegativeButton("No") { dialog, which ->
                dialogBuilder.dismiss() // Dismiss the dialog without doing anything
            }

            // Show the confirmation dialog
            builder.show()

        }
        dialogBuilder.show()

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

    private fun refreshKeyList() {
        val keyLayout: LinearLayout = findViewById(R.id.key_layout)

        // Fetch all available keys
        val keys = dbHelper.getAllKeys()

        // Track the current number of buttons in the layout
        val existingButtons = keyLayout.childCount

        // Update existing buttons or create new ones if necessary
        for ((index, key) in keys.withIndex()) {
            if (index < existingButtons) {
                // Update existing button
                val button = keyLayout.getChildAt(index) as Button
                button.text = key
                button.setOnClickListener {
                    showKeyDetailsPopup(key) // Show details when clicked
                }
            } else {
                // Create new button if there are more keys than existing buttons
                val button = Button(this)
                button.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                button.text = key
                button.setOnClickListener {
                    showKeyDetailsPopup(key) // Show details when clicked
                }

                // Add the new button to the layout
                keyLayout.addView(button)
            }
        }

        // Remove extra buttons if there are fewer keys than existing buttons
        if (existingButtons > keys.size) {
            keyLayout.removeViews(keys.size, existingButtons - keys.size)
        }
    }
}
