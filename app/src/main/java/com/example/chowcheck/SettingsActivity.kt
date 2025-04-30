package com.example.chowcheck

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.Context // Import Context for SharedPreferences
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import com.example.chowcheck.frag.ProfileFragment // Import ProfileFragment constants


class SettingsActivity : Activity() {

    private lateinit var listView: ListView
    private lateinit var buttonGoBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        listView = findViewById(R.id.settingsListView)
        buttonGoBack = findViewById(R.id.btnBack)

        // Remove "Profile" from this list
        val settingsOptions = listOf(
            // "Profile", // Removed
            "Notifications",
            "Privacy",
            "Security",
            "About Developers",
            "Logout"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, settingsOptions)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (position) {
                // Case 0 is now Notifications
                0 -> startActivity(Intent(this, NotificationActivity::class.java))
                1 -> startActivity(Intent(this, PrivacyActivity::class.java))
                2 -> startActivity(Intent(this, SecurityActivity::class.java))
                3 -> startActivity(Intent(this, DeveloperActivity::class.java))
                4 -> showLogoutDialog() // Logout is now position 4
            }
        }

        buttonGoBack.setOnClickListener {
            // Simply finish this activity, the standard back press will handle Fragment backstack if needed
            finish()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Clear logged-in user status from SharedPreferences
                val editor = getSharedPreferences(ProfileFragment.USER_DATA_PREFS, MODE_PRIVATE).edit()
                editor.remove(ProfileFragment.KEY_LOGGED_IN_USER)
                // Optionally remove other user-specific data if needed
                editor.apply()

                // Navigate to LoginActivity and clear the task stack
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity() // Finish this activity and all parent activities
            }
            .setNegativeButton("No", null)
            .show()
    }
}