package com.ambiback.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
//import android.support.annotation.RequiresApi
import androidx.annotation.RequiresApi
// import android.support.v4.app.NotificationCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ambiback.app.globals.Static
import java.util.*

class CaptureService() : IntentService("com.ambiback.app.screencapture.service.") {
    companion object {
        private val TAG = CaptureService::class.qualifiedName
        private val REQUEST_MEDIA_PROJECTION = 1
        val ACTION_ENABLE_CAPTURE = "enable_capture"
        val ACTION_DISABLE_CAPTURE = "disable_capture"

        // Public Properties
        var mStaticMainActivity: MainActivity? = null
        var mMediaProjection: MediaProjection? = null

        // Static properties
        val SCREENWIDTH = 320;
        val SCREENHEIGHT = 240;

        // Store result code for mediaprojection!
        var resultCode = 0;

        private val BASE = "com.ambiback.app.screencapture.service."
        val EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE"
    }


//    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private var mMediaProjectionManager: MediaProjectionManager? = null
    private val REQUEST_MEDIA_PROJECTION = 1

    private var display: VirtualDisplay? = null
    private var onCaptureListener: ((Bitmap) -> Unit)? = null
    private val notificationId = Random().nextInt()

    private val capture = Capture(this)

    override fun onCreate() {
        Log.i(TAG, "Service onCreate")
    }
    // Main Service function
    override fun onHandleIntent(intent: Intent?) {
        Log.i(TAG, "onHandleIntent: ENTER")

        if (intent != null) {
            runBackgroundLoop(intent)
        }
        Log.i(TAG, "onHandleIntent: EXIT")
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ENTER")

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("Capture", "Capture Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foreground Service Kotlin Example")
            .setContentText("Test")
            .setSmallIcon(R.drawable.icon_ambiback_controller)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        if (intent != null) {
            runBackgroundLoop(intent)
        }

        Log.i(TAG, "onStartCommand: EXIT")
        return Service.START_STICKY
    }

    fun runBackgroundLoop(intent: Intent) {
        Log.i(TAG, "runBackgroundLoop: ENTER")

        /*
        if (Static.gCaptureActivity == null) {
            Log.i(TAG, "runBackgroundLoop: Create gCaptureActivity")
            // Create CaptureActivity activity
            val newIntent = Intent(baseContext, CaptureActivity::class.java)
            //            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // newIntent.putExtra(DeviceActivity.ITEM_DEVICE_KEY, deviceKey )
            application.startActivity(newIntent)
            // Start Background Activity
            // startActivityForResult( newIntent, REQUEST_MEDIA_PROJECTION)
        }*/

        // Start Init Code
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        val screenCaptureIntent = mMediaProjectionManager.createScreenCaptureIntent()
//         startActivityForResult(Static.gMainActivity!!, screenCaptureIntent, REQUEST_MEDIA_PROJECTION, screenCaptureIntent.extras )

        // Get Result code from ExtraData
        resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);

        // Create MediaProjection if necessary
        if (mMediaProjection == null){
            mMediaProjection = mMediaProjectionManager!!.getMediaProjection(resultCode, intent);
        }

        if (mMediaProjection != null) {
            Log.i(TAG, "runBackgroundLoop: SUCCESS $resultCode")

            // Trigger capture here
            this.enableCapture()
        } else {
            Log.i(TAG, "runBackgroundLoop: FAILED $resultCode")
            Thread.sleep(1000);
//                Toast.makeText(this, "getMediaProjection FAILED", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onDestroy() {
        Log.i(TAG, "CaptureService: onDestroy - Started?="+ Static.mServiceStarted )

//        super.onDestroy()
        disableCapture()

        val broadcastIntent = Intent(this, RestarterBroadcastReceiver::class.java)
        broadcastIntent.putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
        sendBroadcast(broadcastIntent)
    }

    private fun enableCapture() {
        if (CaptureService.mMediaProjection == null) {
            Log.i(TAG, "enableCapture: mMediaProjection == NULL")
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Log.i(TAG, "enableCapture: run onEnableCapture")
            onEnableCapture()
        }
    }

    private fun onEnableCapture() {
//        Toast.makeText(this, "Capture Service started", Toast.LENGTH_SHORT).show()

        CaptureService.mMediaProjection?.run {
            Log.i(TAG, "onEnableCapture: BEGIN Capture!")
            capture.run(this) {
                Log.v(TAG, "onEnableCapture: Image captured - Time Taken: " + Capture.timeTakenms + " ms")

                for ((deviceKey, deviceValue) in Static.gDeviceMap) {
                    val myDevice = deviceValue as DeviceAmbiBack
                    if (myDevice.UseDevice) {
                        // Check Connection each time
                        myDevice.connect()

                        // Get copy of image to device
                        if (myDevice.processLedData())
                            myDevice.sendLedData()
                        Log.v(TAG, "onEnableCapture: ProcessingTime (" + myDevice.DeviceID + "): " + Capture.timeTakenms + " ms")

                        // TODO - HANDLE SLEEP IN DEVICE processLedData!
                        // Handle Frame thread Delay
                        myDevice.CheckAndSleepFrameDelay();
                    }
                }

                // capture.stop()
                // save bitmap
            }

            Log.i(TAG, "onEnableCapture: END Capture!")

            // Disconnect all sessions
            for ((deviceKey, deviceValue) in Static.gDeviceMap) {
                val myDevice = deviceValue as DeviceAmbiBack
                myDevice.disconnect()
            }
        }
    }

    private fun disableCapture() {
        capture.stop()
//        CaptureService.mMediaProjection = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

}