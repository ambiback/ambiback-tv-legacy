package com.ambiback.app

import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
// import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ambiback.app.globals.Static

class CaptureActivity : AppCompatActivity() {
    // Private properties
    private val REQUEST_MEDIA_PROJECTION = 1
    private lateinit var mMediaProjectionManager: MediaProjectionManager

    // Static Properties
    companion object {
        private val TAG = CaptureActivity::class.qualifiedName
        var mMediaProjectionInitDone = false
        var mInitDone = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        // Set reference of this activity
        Static.gCaptureActivity = this

        // Trigger MediaProjection
        startMediaProjection()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "onActivityResult")
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                mInitDone = true
                val captureIntent = Intent(this, CaptureService::class.java)
                captureIntent.setAction(CaptureService.ACTION_ENABLE_CAPTURE)
                captureIntent.putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                captureIntent.putExtras(data)

                Log.i(TAG, "onActivityResult: getMediaProjection SUCCESS")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(captureIntent);
                } else {
                    this.startService(captureIntent);
                }
                Static.gbMirrorEnabled = true
                Static.SaveConfig()

                // Service is started
                Static.mServiceStarted = true

                /*
                */

                // Close ourselfes
                Log.i(TAG, "onActivityResult: FINISH")
                finish();
            } else {
                Log.i(TAG, "onActivityResult: getMediaProjection FAILED")
                Toast.makeText(this, "Error, Projection failed", Toast.LENGTH_SHORT).show()
// TODO disable APP
            }
        }
    }

    fun startMediaProjection(){
        Log.i(TAG, "startMediaProjection")
        if (mMediaProjectionInitDone == false) {
            // Start Init Code
            mMediaProjectionManager = getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        } else {
            Log.i(TAG, "mMediaProjectionInitDone already done")
/*
            // Get CaptureIntent Service
            val captureIntent = Intent(this, CaptureService::class.java)

            if (isChecked) {
                // Start or stop your Service
                captureIntent.setAction(CaptureService.ACTION_DISABLE_CAPTURE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(captureIntent);
                } else {
                    this.startService(captureIntent);
                }
                Static.mServiceStarted = true
                Static.gbAmbibackEnabled = true
                Static.SaveConfig()
            } else {
                // The switch is disabled
                //                text_view.text = "Switch off"
                // Stop Capture Intent
                captureIntent.setAction(CaptureService.ACTION_ENABLE_CAPTURE)
                this.stopService(captureIntent);
                Static.mServiceStarted = false
                Static.gbAmbibackEnabled = false
                Static.SaveConfig()
            }
            */
        }
    }



}
