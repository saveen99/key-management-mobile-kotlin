package com.example.myapplication

import Handover
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "KeyManagement.db"
        private const val DATABASE_VERSION = 1

        // Table names
        private const val TABLE_ADMIN = "Admin"
        private const val TABLE_USER = "User"
        private const val TABLE_HANDOVER = "Handover"

        // Admin Table columns
        private const val ADMIN_USERNAME = "username"
        private const val ADMIN_PASSWORD = "password"

        // User Table columns
        private const val USER_KEY_NAME = "key_name"
        private const val USER_TAKEN_BY = "taken_by"
        private const val USER_P_NUMBER = "p_number"
        private const val USER_TAKEN_TIME = "taken_time"

        // Handover Table columns
        private const val HANDOVER_KEY_NAME = "key_name"
        private const val HANDOVER_TAKEN_BY = "taken_by"
        private const val HANDOVER_P_NUMBER = "p_number"
        private const val HANDOVER_TAKEN_TIME = "taken_time"
        private const val HANDOVER_TIME = "handover_time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Admin table
        val createAdminTable = ("CREATE TABLE $TABLE_ADMIN ($ADMIN_USERNAME TEXT PRIMARY KEY, $ADMIN_PASSWORD TEXT)")
        db.execSQL(createAdminTable)

        // Create User table
        val createUserTable = ("CREATE TABLE $TABLE_USER ($USER_KEY_NAME TEXT, $USER_TAKEN_BY TEXT, $USER_P_NUMBER TEXT, $USER_TAKEN_TIME TEXT)")
        db.execSQL(createUserTable)

        // Create Handover table
        val createHandoverTable = ("CREATE TABLE $TABLE_HANDOVER ($HANDOVER_KEY_NAME TEXT, $HANDOVER_TAKEN_BY TEXT, $HANDOVER_P_NUMBER TEXT, $HANDOVER_TAKEN_TIME TEXT, $HANDOVER_TIME TEXT)")
        db.execSQL(createHandoverTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ADMIN")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HANDOVER")
        onCreate(db)
    }

    // Add methods for CRUD operations

    fun addKey(user: User): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        // Correct property names from the User class
        contentValues.put(USER_KEY_NAME, user.key_name)
        contentValues.put(USER_TAKEN_BY, user.taken_by)
        contentValues.put(USER_P_NUMBER, user.p_number)
        contentValues.put(USER_TAKEN_TIME, user.taken_time)

        val currentTime = SimpleDateFormat("yyyy-MM-dd | HH:mm:ss", Locale.getDefault()).format(Date())
        contentValues.put(USER_TAKEN_TIME, currentTime)

        return db.insert(TABLE_USER, null, contentValues)
    }

    // Method to get all keys from the User table
    @SuppressLint("Range")
    fun getAllKeys(): List<String> {
        val keysList = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT $USER_KEY_NAME FROM $TABLE_USER", null)
        if (cursor.moveToFirst()) {
            do {
                val keyName = cursor.getString(cursor.getColumnIndex(USER_KEY_NAME))
                keysList.add(keyName)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return keysList
    }

    // Method to get details of a specific key
    @SuppressLint("Range")
    fun getUser(keyName: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USER WHERE $USER_KEY_NAME = ?", arrayOf(keyName))
        if (cursor.moveToFirst()) {
            val user = User(
                key_name = cursor.getString(cursor.getColumnIndex(USER_KEY_NAME)),
                taken_by = cursor.getString(cursor.getColumnIndex("taken_by")),
                p_number = cursor.getString(cursor.getColumnIndex("p_number")),
                taken_time = cursor.getString(cursor.getColumnIndex("taken_time"))
            )
            cursor.close()
            return user
        }
        cursor.close()
        return null
    }

    // Method to update the user details
    fun updateUser(user: User) {
        val db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put(USER_KEY_NAME, user.key_name)
        contentValues.put("taken_by", user.taken_by)
        contentValues.put("p_number", user.p_number)
        contentValues.put("taken_time", user.taken_time)
        db.update(TABLE_USER, contentValues, "$USER_KEY_NAME = ?", arrayOf(user.key_name))
    }

    // Method to add handover record
    fun addHandover(handover: Handover): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        // Add handover details to ContentValues
        contentValues.put(HANDOVER_KEY_NAME, handover.key_name)
        contentValues.put(HANDOVER_TAKEN_BY, handover.taken_by)
        contentValues.put(HANDOVER_P_NUMBER, handover.p_number)
        contentValues.put(HANDOVER_TAKEN_TIME, handover.taken_time)

        // Assign handover time, using the current time if it's null
        val handoverTime = handover.handover_time ?: SimpleDateFormat("yyyy-MM-dd | HH:mm:ss", Locale.getDefault()).format(Date())
        contentValues.put(HANDOVER_TIME, handoverTime)

        // Insert the handover record into the Handover table
        val result = db.insert(TABLE_HANDOVER, null, contentValues)

        // If insertion is successful, delete the corresponding entry from the User table
        if (result != -1L) {
            db.delete(TABLE_USER, "$USER_KEY_NAME = ?", arrayOf(handover.key_name))
        }

        return result
    }

    // Method to get all handover records
    @SuppressLint("Range")
    fun getAllHandoverRecords(): List<Handover> {
        val handoverList = mutableListOf<Handover>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_HANDOVER", null)
        if (cursor.moveToFirst()) {
            do {
                val handover = Handover(
                    key_name = cursor.getString(cursor.getColumnIndex(HANDOVER_KEY_NAME)),
                    taken_by = cursor.getString(cursor.getColumnIndex(HANDOVER_TAKEN_BY)),
                    p_number = cursor.getString(cursor.getColumnIndex(HANDOVER_P_NUMBER)),
                    taken_time = cursor.getString(cursor.getColumnIndex(HANDOVER_TAKEN_TIME)),
                    handover_time = cursor.getString(cursor.getColumnIndex(HANDOVER_TIME))
                )
                handoverList.add(handover)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return handoverList
    }

    fun exportHandoverRecordsToExcel(): Boolean {
        val handoverRecords = getAllHandoverRecords()
        if (handoverRecords.isEmpty()) {
            return false
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Handover Records")
        val headerRow = sheet.createRow(0)

        // Add header columns
        headerRow.createCell(0).setCellValue("Key Name")
        headerRow.createCell(1).setCellValue("Taken By")
        headerRow.createCell(2).setCellValue("P Number")
        headerRow.createCell(3).setCellValue("Taken Time")
        headerRow.createCell(4).setCellValue("Handover Time")

        // Populate the Excel sheet with data
        for ((index, handover) in handoverRecords.withIndex()) {
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(handover.key_name)
            row.createCell(1).setCellValue(handover.taken_by)
            row.createCell(2).setCellValue(handover.p_number)
            row.createCell(3).setCellValue(handover.taken_time)
            row.createCell(4).setCellValue(handover.handover_time)
        }

        // Save the workbook to a file
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/KeyManagement")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "HandoverRecords_${System.currentTimeMillis()}.xlsx")
        try {
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                workbook.close()
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun clearHandoverTable(): Boolean {
        val db = this.writableDatabase
        return try {
            db.execSQL("DELETE FROM $TABLE_HANDOVER")
            true // Return true if cleared successfully
        } catch (e: Exception) {
            e.printStackTrace()
            false // Return false if there was an error
        }
    }

    // Method to delete a user by key name
    fun deleteUser(keyName: String): Boolean {
        val db = this.writableDatabase
        return try {
            db.delete(TABLE_USER, "$USER_KEY_NAME = ?", arrayOf(keyName)) > 0 // Returns true if at least one row was deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false // Return false if there was an error
        }
    }




}
