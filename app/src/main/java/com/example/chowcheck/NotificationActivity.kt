package com.example.chowcheck

import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import android.app.Activity

class NotificationActivity : Activity() {

    private lateinit var switchMealReminder: Switch
    private lateinit var switchWaterReminder: Switch
    private lateinit var switchProgressReminder: Switch
    private lateinit var switchGoalReminder: Switch
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        switchMealReminder = findViewById(R.id.switchMealReminder)
        switchWaterReminder = findViewById(R.id.switchWaterReminder)
        switchProgressReminder = findViewById(R.id.switchProgressReminder)
        switchGoalReminder = findViewById(R.id.switchGoalReminder)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }
        switchMealReminder.isChecked = true
        switchWaterReminder.isChecked = false
        switchProgressReminder.isChecked = true
        switchGoalReminder.isChecked = false

        switchMealReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Meal Reminder Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Meal Reminder Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        switchWaterReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Water Reminder Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Water Reminder Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        switchProgressReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Progress Reminder Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Progress Reminder Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        switchGoalReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Goal Reminder Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Goal Reminder Disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
