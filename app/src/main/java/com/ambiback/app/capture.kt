package com.ambiback.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.Log
import com.ambiback.app.globals.Static
import kotlin.system.measureTimeMillis

class Capture(val context: Context) : ImageReader.OnImageAvailableListener {

    companion object {
        private val TAG = Capture::class.qualifiedName
        var mBitmap: Bitmap? = null
        var mBuffer: ByteArray? = null
        var mBufWidth: Int = 0
        var mBufHeight: Int = 0
        var mBufPixelStride: Int = 0
        var mBufRowStride: Int = 0
        var mBufBlackLinesTop: Int = 0
        var mBufBlackLinesBottom: Int = 0

        var timeTakenms: Long = 0
        var mReader: ImageReader? = null
    }
    private var display: VirtualDisplay? = null
    private var onCaptureListener: ((Bitmap) -> Unit)? = null

    fun run(mediaProjection: MediaProjection, onCaptureListener: (Bitmap) -> Unit) {
        this.onCaptureListener = onCaptureListener
        if (display == null) {
            display = createDisplay(mediaProjection)
        }
    }

    private fun createDisplay(mediaProjection: MediaProjection): VirtualDisplay {
        context.resources.displayMetrics.run {
            // Hardcoded lower resolution for now
            widthPixels = CaptureService.SCREENWIDTH;
            heightPixels = CaptureService.SCREENHEIGHT;

            val maxImages = 2
            mReader = ImageReader.newInstance(
                widthPixels, heightPixels, PixelFormat.RGBA_8888, maxImages)
            mReader?.setOnImageAvailableListener(this@Capture, null)
            val display = mediaProjection.createVirtualDisplay(
                "Capture Display", widthPixels, heightPixels, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR + DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mReader?.surface, null, null)
            Log.d(TAG, "createVirtualDisplay")
            return display
        }
    }

    override fun onImageAvailable(reader: ImageReader) {
        if (display != null) {
            onCaptureListener?.invoke(captureImage(reader))
        }
    }

    private fun captureImage(reader: ImageReader): Bitmap {
        Log.d(TAG, "captureImage")
        var image: Image? = null
        timeTakenms = measureTimeMillis {
            image = reader.acquireLatestImage()
        }
        if (image != null) {
            // image
            Log.d(TAG, "captureImage: IMAGE CAPTURED")
            context.resources.displayMetrics.run {
                image!!.planes[0].run {

                    // Duplicate buffer for processing
                    Capture.mBuffer = ByteArray(buffer.capacity());
                    buffer.get( mBuffer )
                    buffer.rewind();      // Rewind buffer

                    // Force autodetect for next frame
                    mBufBlackLinesTop = -1
                    mBufBlackLinesBottom = -1

                    // Copy to bitmap for display
                    var bitmap = Bitmap.createBitmap(
                        rowStride / pixelStride,
                        heightPixels,
                        Bitmap.Config.ARGB_8888)
                    try {
// TODO: HANDLE Screen toggle
                        // Try to copy buffer
                        bitmap.copyPixelsFromBuffer(buffer)

                        // Reach this point? We set the member properties
                        mBufWidth = widthPixels
                        mBufHeight = heightPixels
                        mBufPixelStride = pixelStride
                        mBufRowStride = rowStride

                        // Set copy to mBitmap
                        Capture.mBitmap = bitmap
                        MainActivity?.UpdateImage()

                    } catch (ex: Exception) {
                        Log.e(Capture.TAG, "captureImage Exception ${ex.message}")

                        // Correct our resolution for next frame
                        widthPixels = 320;
                        heightPixels = 240;
                    }

                    // Close image
                    image!!.close()
                    return bitmap
                }
            }
        } else {
            Log.d(TAG, "captureImage: NULL IMAGE")
            Capture.mBitmap = null
            return Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        }
    }

    fun stop() {
        display?.release()
        display = null
        onCaptureListener = null
    }
}

