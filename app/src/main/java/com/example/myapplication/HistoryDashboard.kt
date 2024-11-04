package com.example.myapplication

import Handover
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.AlertDialog

class HistoryDashboard : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var historyLayout: LinearLayout
    private lateinit var btnExport : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history_dashboard)

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Find the LinearLayout where buttons will be added
        historyLayout = findViewById(R.id.history_layout)

        btnExport = findViewById(R.id.btn_exportExcel)

        // Load handover records and create buttons dynamically
        loadHandoverRecords()

        btnExport.setOnClickListener {
            // Export handover records to Excel
            if (dbHelper.exportHandoverRecordsToExcel()) {
                Toast.makeText(this, "Exported to Excel successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to export to Excel", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear History Button
        val btnClearHistory: Button = findViewById(R.id.btn_clearHistory)
        btnClearHistory.setOnClickListener {
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear the history?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // If user confirms, proceed with clearing the database
                    if (dbHelper.clearHandoverTable()) {
                        // Success message
                        Toast.makeText(this, "History cleared successfully", Toast.LENGTH_SHORT).show()
                        historyLayout.removeAllViews() // Optional: Clear the layout
                    } else {
                        // Failure message
                        Toast.makeText(this, "Failed to clear history", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss() // Close the dialog
                }
                .setNegativeButton("No") { dialog, _ ->
                    // If user cancels, close the dialog
                    dialog.dismiss()
                }
                .create()
                .show()
        }


        ViewCompat.setOnApplyWindowInsetsListener(historyLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadHandoverRecords() {
        val handoverRecords = dbHelper.getAllHandoverRecords()

        // Check if there are records
        if (handoverRecords.isEmpty()) {
            // Show a message if there are no records (optional)
            val noDataTextView = TextView(this).apply {
                text = "No Handover Records Found"
                textSize = 24f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            historyLayout.addView(noDataTextView)
            return
        }

        val reversedHandoverRecords = handoverRecords.reversed()

        // Create buttons for each handover record
        for (handover in reversedHandoverRecords) {
            val button = Button(this).apply {
                text = handover.key_name // Display key name on the button

                // Set layout parameters with margins
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    150 // Height of the button
                ).apply {
                    setMargins(16, 16, 16, 16) // Add margins
                }

                // Set button background (rounded corners)
                background = ContextCompat.getDrawable(this@HistoryDashboard, R.drawable.rounded_button)

                // Set text color and size
                setTextColor(ContextCompat.getColor(this@HistoryDashboard, R.color.white)) // Change text color to white
                textSize = 18f // Set text size to 18sp

                // Set button click listener
                setOnClickListener {
                    showHandoverDetailsDialog(handover)
                }
            }
            historyLayout.addView(button)
        }
    }

    private fun showHandoverDetailsDialog(handover: Handover) {
        // Create a dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_dialog_handover)

        // Get references to the dialog views
        val tvKeyName: TextView = dialog.findViewById(R.id.tvKeyName)
        val tvTakenBy: TextView = dialog.findViewById(R.id.tvTakenBy)
        val tvPNumber: TextView = dialog.findViewById(R.id.tvPNumber)
        val tvTakenTime: TextView = dialog.findViewById(R.id.tvTakenTime)
        val tvHandoverTime: TextView = dialog.findViewById(R.id.tvHandoverTime)
        val btnClose: Button = dialog.findViewById(R.id.btnClose)

        // Set the details in the dialog views
        tvKeyName.text = "Key Name: ${handover.key_name}"
        tvTakenBy.text = "Taken By: ${handover.taken_by}"
        tvPNumber.text = "P Number: ${handover.p_number}"
        tvTakenTime.text = "Taken Time: ${handover.taken_time}"
        tvHandoverTime.text = "Handover Time: ${handover.handover_time}"

        // Set up the close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

}
