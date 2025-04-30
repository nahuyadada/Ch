package com.example.chowcheck

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.LinearLayout // Keep this import
import android.widget.TextView // Add this if you need the TextViews later
import android.widget.Toast

class SecurityActivity : Activity() {

    private lateinit var btnBack: ImageView
    private lateinit var switch2FA: Switch
    // *** Change variable types to LinearLayout and rename ***
    private lateinit var layoutChangePass: LinearLayout
    private lateinit var layoutLinkedAcc: LinearLayout
    // Optional: If you still need the TextViews themselves, declare them
    // private lateinit var textChangePassHeader: TextView
    // private lateinit var textLinkedAccHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        try {
            switch2FA = findViewById(R.id.switch2FA)
            // *** Find the LinearLayouts by their new IDs ***
            layoutChangePass = findViewById(R.id.layoutChangePassword)
            layoutLinkedAcc = findViewById(R.id.layoutLinkedAcc)

            // Set listeners on the LinearLayouts
            layoutChangePass.setOnClickListener {
                Toast.makeText(this, "Navigating to Change Password screen.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ChangePasswordActivity::class.java)
                startActivity(intent)
            }

            layoutLinkedAcc.setOnClickListener {
                Toast.makeText(this, "Navigating to Manage Linked Accounts.", Toast.LENGTH_SHORT).show()
                // val intent = Intent(this, ManageLinkedAccountsActivity::class.java)
                // startActivity(intent)
            }

            // Switch listener remains the same
            switch2FA.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Toast.makeText(this, "2FA Enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "2FA Disabled", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Consider more specific error handling if needed
            e.printStackTrace()
            Toast.makeText(this, "Error initializing security options: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}