package com.ambiback.app

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
//import android.support.v4.app.ActivityCompat.startActivityForResult
//import android.support.v4.content.ContextCompat.getSystemService
import android.util.Log
import com.ambiback.app.globals.Static

class RestarterBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = RestarterBroadcastReceiver::class.qualifiedName
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive: Service was stopped: Started?=" + Static.mServiceStarted)

        if (true) { // Static.mServiceStarted) {
            Log.i(TAG, "onReceive: Service was running before, restart it!")

    //            handleSwitchAmbiBack(true /*Static.gbAmbibackEnabled*/)

            // Get CaptureIntent Service
            // Get Result code from ExtraData
            var resultCode = intent.getIntExtra(CaptureService.EXTRA_RESULT_CODE, -1);
            Log.i(TAG, "onReceive: EXTRA_RESULT_CODE = $resultCode")

            return

            // context
            val captureIntent = Intent( context, CaptureService::class.java)
            captureIntent.putExtra(CaptureService.EXTRA_RESULT_CODE, -1)
            captureIntent.setAction(CaptureService.ACTION_DISABLE_CAPTURE)
            context.startService(captureIntent);
        }
    }
}
