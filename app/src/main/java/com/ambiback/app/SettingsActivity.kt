package com.ambiback.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.settings_activity.*

class SettingsActivity : AppCompatActivity() {
    // Static Properties
    companion object {
        private val TAG = SettingsActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

/*        buttonAboutNew.setOnClickListener {
            Log.i(TAG, "buttonAbout: click")
            // Create CaptureActivity activity
            val newIntent = Intent(this@SettingsActivity, aboutActivity::class.java)
            // Start  Activity
            startActivity(newIntent)
        }*/

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.ambiback_preferences, rootKey)
        }
    }
}