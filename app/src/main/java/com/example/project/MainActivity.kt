package com.example.project

import android.nfc.Tag
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.project.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var fragmentManager: FragmentManager
    private lateinit var fragmentTransaction: FragmentTransaction
    private lateinit var binding: ActivityMainBinding

    // Store the tag for each fragment
    private val upscaleFragmentTag = "UpscaleFragment"
    private val colouriseFragmentTag = "ColouriseFragment"
    private val settingsFragmentTag = "SettingsFragment"

    // Add variable to store the current fragment Tag
    private var currentFragmentTag = upscaleFragmentTag






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore the current fragment
        if (savedInstanceState != null) {
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag", upscaleFragmentTag)
        } else {
            goToFragment(currentFragmentTag)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.upscale -> {
                    goToFragment(currentFragmentTag)
                    true
                }
                R.id.colourise -> {
                    goToFragment(colouriseFragmentTag)
                    true
                }
                R.id.settings -> {
                    goToFragment(settingsFragmentTag)
                    true
                }
                else -> false
            }
        }
    }

    private fun goToFragment(tag: String) {
        fragmentManager = supportFragmentManager
        fragmentTransaction = fragmentManager.beginTransaction()

        // Find the fragment by tag
        var fragment = fragmentManager.findFragmentByTag(tag)

        // If the fragment is null, create a new instance
        if (fragment == null) {
            fragment = when (tag) {
                upscaleFragmentTag -> UpscaleFragment()
                colouriseFragmentTag -> ColouriseFragment()
                settingsFragmentTag -> SettingsFragment()
                else -> UpscaleFragment()
            }
        }

        fragmentTransaction.replace(R.id.fragmentContainer, fragment, tag)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentFragmentTag", currentFragmentTag)
    }
}