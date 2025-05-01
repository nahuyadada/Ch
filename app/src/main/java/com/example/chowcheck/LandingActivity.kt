package com.example.chowcheck

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class LandingActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottomNav)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment

        navController = navHostFragment.navController

        // Standard setup - this connects the BottomNavigationView items
        // to the top-level destinations in your nav graph based on their IDs.
        bottomNavView.setupWithNavController(navController)

        // --- REMOVED the custom setOnItemSelectedListener ---
        // The ViewModel approach should now handle the date state synchronization
        // when navigating between Diary and FoodLog via the bottom bar.

        // Optional: Handle reselection if needed (e.g., tapping Diary when already on Diary)
        /*
        bottomNavView.setOnItemReselectedListener { item ->
             // If you want tapping the current tab to do something specific, like scroll to top
             // or reset the date to today via the ViewModel:
             // if (item.itemId == R.id.diaryFragment) {
             //    val dateViewModel: DateViewModel by viewModels() // Or however you get the VM instance here
             //    dateViewModel.resetToToday()
             // }
             // Or pop the back stack for that tab:
             // navController.popBackStack(item.itemId, inclusive = false)
        }
        */
    }

    // Keep this if you might add an ActionBar later
    /*
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    */
}
