package com.example.chowcheck

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// --- Import NavController and helpers ---
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
// --- Keep this import ---
import com.google.android.material.bottomnavigation.BottomNavigationView

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottomNav)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment


        val navController: NavController = navHostFragment.navController

        bottomNavView.setupWithNavController(navController)

    }


}