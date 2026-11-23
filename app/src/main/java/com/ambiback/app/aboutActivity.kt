package com.ambiback.app

// import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_about.*

class aboutActivity : AppCompatActivity() {

    // Static Properties
    companion object {
        private val TAG = aboutActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        Log.i(TAG, "ENTER onCreate" )

        // Control Init Stuff
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Controls Init
        textViewVersion.text = "Version: " + BuildConfig.VERSION_NAME + "." + BuildConfig.VERSION_CODE

        // Controls Events
        textViewAbout.setOnClickListener() {
            Log.i(TAG, "textViewAbout: click")
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ambiback.de"))
            startActivity(browserIntent)
        }
    }

    // textViewAbout

}