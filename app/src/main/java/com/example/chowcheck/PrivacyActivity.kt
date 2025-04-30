package com.example.chowcheck

import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import android.app.Activity

class PrivacyActivity : Activity() {

    private lateinit var btnBack: ImageView
    private lateinit var switchDataCollect: Switch
    private lateinit var spinnerVisibility: Spinner
    private lateinit var spinnerProfile: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        btnBack = findViewById(R.id.btnBack)
        switchDataCollect = findViewById(R.id.switchDataCollect)
        spinnerVisibility = findViewById(R.id.spinnerVisibilty)
        spinnerProfile = findViewById(R.id.spinnerProfile)

        btnBack.setOnClickListener {
            finish()
        }

        val visibilityOptions = arrayOf("Everyone", "Friends", "Private")
        val visibilityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, visibilityOptions)
        visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVisibility.adapter = visibilityAdapter

        val diaryOptions = arrayOf("Public", "Friends", "Private")
        val diaryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, diaryOptions)
        diaryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProfile.adapter = diaryAdapter

        switchDataCollect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Data collection enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Data collection disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
