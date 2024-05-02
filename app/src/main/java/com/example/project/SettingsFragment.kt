package com.example.project

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate


class SettingsFragment : Fragment() {

    private lateinit var settingsSpinner: Spinner
    private var myView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Check if the view is already inflated
        if (myView == null) {
            myView = inflater.inflate(R.layout.settingsfragment, container, false)

            settingsSpinner = myView!!.findViewById(R.id.settingsSpinner)
            val adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.DarkMode,
                android.R.layout.simple_spinner_item
            )
            settingsSpinner.adapter = adapter

            settingsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (view == null) {
                        return
                    }
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    when (selectedItem) {
                        "Light" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }

                        "Dark" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }

                        "System Default" -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                    applyThemeBasedOnNightMode()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }


        }
        return myView
    }


    private fun applyThemeBasedOnNightMode() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                (activity as AppCompatActivity).delegate.applyDayNight()
            }
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                (activity as AppCompatActivity).delegate.applyDayNight()
            }
            android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                (activity as AppCompatActivity).delegate.applyDayNight()
            }
        }
    }

}