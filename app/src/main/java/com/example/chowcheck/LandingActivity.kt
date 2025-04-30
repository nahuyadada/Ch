package com.example.chowcheck

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.chowcheck.frag.DiaryFragment
import com.example.chowcheck.frag.FoodLogFragment
import com.example.chowcheck.frag.ProfileFragment
import com.example.chowcheck.frag.ResultsTabFragment // *** ADD THIS IMPORT ***
import com.google.android.material.bottomnavigation.BottomNavigationView

class LandingActivity : AppCompatActivity() {

    private lateinit var bottomNavView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        bottomNavView = findViewById(R.id.bottomNav)

        val fragmentToLoad = intent.getStringExtra("LOAD_FRAGMENT")

        if (savedInstanceState == null && fragmentToLoad == null) {
            bottomNavView.selectedItemId = R.id.navigation_diary
            replaceFragment(DiaryFragment())
        } else if (fragmentToLoad == "PROFILE") {
            bottomNavView.selectedItemId = R.id.navigation_profile
            // The listener below should handle loading, but uncomment if needed:
            // replaceFragment(ProfileFragment())
        }
        // Add other 'else if' blocks here if needed


        // --- Listener Setup ---
        bottomNavView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.navigation_diary -> DiaryFragment()
                R.id.navigation_food_log -> FoodLogFragment()
                R.id.navigation_profile -> ProfileFragment()
                // *** ADD THIS CASE to load ResultsTabFragment ***
                R.id.navigation_results -> ResultsTabFragment() as Fragment// Load the fragment directly
                // ---------------------------------------------
                else -> DiaryFragment() // Default case
            }
            replaceFragment(selectedFragment)
            true
        }

        bottomNavView.setOnItemReselectedListener { item ->
            // Optional: Handle reselection if needed (e.g., scroll to top)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            // Optional: Add .addToBackStack(null) if you want fragment back navigation
            .commit()
    }
}