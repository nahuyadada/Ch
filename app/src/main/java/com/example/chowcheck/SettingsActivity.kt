package com.example.chowcheck

// Remove: import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences // Added for clarity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast // Added for clarity
// Import AppCompatActivity and AppCompat AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog // Use AppCompat AlertDialog
// Import ProfileFragment constants (already present)
import com.example.chowcheck.frag.ProfileFragment

// Change inheritance here
class SettingsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var buttonGoBack: ImageView // Consider using Toolbar with setSupportActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings) // Ensure this layout exists and is compatible

        listView = findViewById(R.id.settingsListView)
        buttonGoBack = findViewById(R.id.btnBack) // Ensure R.id.btnBack exists

        // Consider adding a Toolbar and using setSupportActionBar(toolbar)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // toolbar.setNavigationOnClickListener { finish() }
        // If using Toolbar, you might remove the standalone buttonGoBack ImageView

        val settingsOptions = listOf(
            "Notifications",
            "Security",
            "About Developers",
            "Logout"
        )

        // Use 'this' or 'baseContext' which are valid for AppCompatActivity
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, settingsOptions)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(this, NotificationActivity::class.java))
                1 -> startActivity(Intent(this, SecurityActivity::class.java))
                2 -> startActivity(Intent(this, DeveloperActivity::class.java))
                3 -> showLogoutDialog()
                // Add error handling for unexpected positions if needed
            }
        }

        // If not using a Toolbar with NavigationOnClickListener, keep this:
        buttonGoBack.setOnClickListener {
            finish()
        }
    }

    private fun showLogoutDialog() {
        // Use the imported AppCompat AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Clear logged-in user status from SharedPreferences
                // Ensure MODE_PRIVATE is used correctly (Context.MODE_PRIVATE)
                val editor = getSharedPreferences(ProfileFragment.USER_DATA_PREFS, Context.MODE_PRIVATE).edit()
                editor.remove(ProfileFragment.KEY_LOGGED_IN_USER)
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