package com.ambiback.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.StrictMode
import android.util.Log
import java.time.LocalDateTime
import kotlin.experimental.and

enum class DeviceType(val id: Int, val internalname: String) {
    UnknownDevice(0, "UnknownDevice"),
    AmbiBackController(5, "AmbiBack_Controller"),
    PhilipsHueColorLight(6, "PhilipsHueColorLight")
}

enum class LedSetups(val id: Int) {
    Right_Cclock_bottom_right_up ( 0x01),
    Top_Cclock_top_right_left ( 0x02),
    Left_Cclock_top_left_down ( 0x03),
    Bottom_Cclock_bottom_left_right ( 0x04),
    Right_Clock_top_right_down ( 0x05),
    Top_Clock_top_left_right ( 0x06),
    Left_Clock_bottom_left_up ( 0x07),
    Bottom_Clock_bottom_right_left (0x08)
}

enum class FlowDirection(val id: Int) {
    LeftToRight  (  0),
    TopDown   ( 1),
    RightToLeft  (  2),
    BottomUp  (  3)
}

// Basic Class for Ambient devices
abstract class Device {
    companion object Static {
        private val TAG = Device::class.qualifiedName
        val BASENAME: String = "com.ambiback.app.device."
    }

    // Global Device properties
    var DeviceID: String = ""
    var InternalDeviceID: String = ""
    var InternalNetworkMAC: String = ""
    var DeviceName: String = ""
    var DeviceVersion: String = ""
    var DeviceType: DeviceType = com.ambiback.app.DeviceType.UnknownDevice
    var IPAddress: String = ""

    // Configfile for Device
    var ConfigFile: String = Device.BASENAME

    // GUI helpers
    var MenuID: Int = -1

    // --- Pixel Calc helpers
    // Helper Array to store the screen areas to scan color values for each pixel!
    var rectScanPixels: Array<Rect?> = arrayOfNulls<Rect?>(1)

    // Helper Attay to store calculated pixels
    var iOutPixels: IntArray? = null

    var timeTakenms: Long = 0
    // ---
    // TODO: Make configureable through GUI / Web Interface

    // Static properties not configureable
    val MinFrameDelay: Int = 10
    val MaxFrameDelay: Int = 1000
    val StepFrameDelay: Int = 10
    val StepFramesPerSecond: Int = 2

    // Should device be used?
    var UseDevice: Boolean= false
    // Configurarion loaded from controller?
    var IsRemoteConfigLoaded: Boolean= false
    // Is Device online?
    var IsOnline: Boolean= false

    // Advanced Controller Properties
    var FrameLength: UByte = 10u    // Should  not be configured by APP
    var FrameDelay: Int = 60

    // Advanced App Client Properties
    var FramesPerSecond: Int = 10
    var MaxFramesPerSecond: UByte = 30u
    var InnerSizePercent: UByte = 15u
    var BorderThreshold: UInt = 1024u // Properly configureable, if color bytes reach that with border detection everything is considered fine
    var ColorBoostPercent: UByte = 20u;
    var ColorBoostPercentMin: UByte = 20u;

    // Color Control variables
    var ColorRedPercent: UByte  = 100u;
    var ColorGreenPercent: UByte  = 100u;
    var ColorBluePercent: UByte  = 100u;
    var ValueGainPercent: Int = 100
    var MaxBrightnessPercent: UByte = 0u;
    var MinimumLuminosityPercent: UByte = 0u;

    // Framerate control
    var mFrameTimeMs : Int = 1000 / FramesPerSecond
    var mLastSecond : Long = 0
    var mLastMilliSec : Long  = 0
    var mFramesPerSec : Int = 0


    init {
        this.DeviceType = com.ambiback.app.DeviceType.UnknownDevice
        this.DeviceName = "Unknown device"
        // this.setDeviceID()
        this.initClass()
    }

    abstract fun setDeviceID()
    abstract fun setRealDeviceID(overrideDeviceID : String)
    abstract fun initClass()
    abstract fun prepareClass()
    abstract fun LoadConfig()
    abstract fun SaveConfig()

    // Public Pixel Processing functions
    fun CalcCorners(PixelTotal: Int) {
        this.iOutPixels = IntArray(PixelTotal)
        Log.v(TAG, "CalcCorners: " + iOutPixels )
    }

    fun InitFrameRateVars() {
        // Framerate Control helpers
        mFrameTimeMs = 1000 / FramesPerSecond
        mLastSecond = System.currentTimeMillis() / 1000
        mLastMilliSec = System.currentTimeMillis() % 1000
        mFramesPerSec = 0
    }


    fun CheckAndSleepFrameDelay() {
        val lCurrentSecond = System.currentTimeMillis() / 1000
        val lMillSecondsDone = (System.currentTimeMillis() % 1000) - mLastMilliSec

        if (lCurrentSecond === mLastSecond) {
            // Check if we are in FrameRate

            if (lMillSecondsDone < mFrameTimeMs) {
                Thread.sleep(mFrameTimeMs.minus(lMillSecondsDone));
                Log.v( TAG,"CheckAndSleepFrameDelay Sleep: ${FramesPerSecond} / ${mFrameTimeMs}/" + mFrameTimeMs.minus(lMillSecondsDone) + " ms")
            } else {
                Log.v( TAG,"CheckAndSleepFrameDelay NoSleep: ${FramesPerSecond} / ${mFrameTimeMs}/" + lMillSecondsDone + " ms")
            }

            // Safe last millsec now
            mLastMilliSec = (System.currentTimeMillis() % 1000)

            // Increment Counter
            mFramesPerSec++
        }else
        {
            Log.i(Device.TAG, "CheckFrameRateDelay: Framerate send: $mFramesPerSec - Target: $DeviceName")

            // Reset variables
            mLastSecond = System.currentTimeMillis() / 1000;
            mLastMilliSec = System.currentTimeMillis() % 1000;
            mFramesPerSec = 0;

            // Update Frame Sleeptime
            mFrameTimeMs = 1000 / FramesPerSecond;
        }
    }

    fun CalcRightRectangles(
        PixelVert: UByte,
        PixelHori: UByte,
        iLeftOffset: Int,
        iTopOffset: Int,
        iScanWidth: Int,
        iScanHeight: Int,
        iIntoImagePx: Int,
        myDirection: FlowDirection,
        rectPixels: Array<Rect?>,
        iOffset: Int
    ): Int {
        var iNewOffset = iOffset

        val iPixelWidth =
            if (iIntoImagePx == 0) iScanWidth * this.InnerSizePercent.toInt() / 100 else iIntoImagePx   /* Use % width */
        val iPixelHeight = iScanHeight / PixelVert.toInt()
        val iLeft = iLeftOffset + iScanWidth - iPixelWidth
        var iTop = 0

        if (myDirection === FlowDirection.TopDown) {
            // TopDown!
            for (iLoop in 0 until PixelVert.toInt()) {
                iTop = iTopOffset + iLoop * iPixelHeight
                rectPixels[iNewOffset] = Rect(iLeft, iTop, iLeft+iPixelWidth, iTop+iPixelHeight)
                iNewOffset++
            }
        } else {
            // BottomUp!
            for (iLoop in PixelVert.toInt() - 1 downTo 0) {
                iTop = iTopOffset + iLoop * iPixelHeight
                rectPixels[iNewOffset] = Rect(iLeft, iTop, iLeft+iPixelWidth, iTop+iPixelHeight)
                iNewOffset++
            }
        }

        /* return new array offset*/
        return iNewOffset
    }

    fun CalcTopRectangles(
        PixelVert: UByte,
        PixelHori: UByte,
        iLeftOffset: Int,
        iTopOffset: Int,
        iScanWidth: Int,
        iScanHeight: Int,
        iIntoImagePx: Int,
        myDirection: FlowDirection,
        rectPixels: Array<Rect?>,
        iOffset: Int
    ): Int {
        var iNewOffset = iOffset

        val iPixelWidth = iScanWidth / PixelHori.toInt()
        val iPixelHeight =
            if (iIntoImagePx == 0) iScanHeight * this.InnerSizePercent.toInt() / 100 else iIntoImagePx /* Use % width */
        var iLeft = 0

        if (myDirection === FlowDirection.RightToLeft) {
            // RightToLeft!
            for (iLoop in PixelHori.toInt() - 1 downTo 0) {
                iLeft = iLeftOffset + iLoop * iPixelWidth
                rectPixels[iNewOffset] = Rect(iLeft, iTopOffset, iLeft+iPixelWidth, iTopOffset+iPixelHeight)
                iNewOffset++
            }
        } else {
            // LeftToRight!
            for (iLoop in 0 until PixelHori.toInt()) {
                iLeft = iLeftOffset + iLoop * iPixelWidth
                rectPixels[iNewOffset] = Rect(iLeft, iTopOffset, iLeft+iPixelWidth, iTopOffset+iPixelHeight)
                iNewOffset++
            }
        }

        /* return new array offset*/
        return iNewOffset
    }

    fun CalcLeftRectangles(
        PixelVert: UByte,
        PixelHori: UByte,
        iLeftOffset: Int,
        iTopOffset: Int,
        iScanWidth: Int,
        iScanHeight: Int,
        iIntoImagePx: Int,
        myDirection: FlowDirection,
        rectPixels: Array<Rect?>,
        iOffset: Int
    ): Int {
        var iNewOffset = iOffset

        val iPixelWidth =
            if (iIntoImagePx == 0) iScanWidth * this.InnerSizePercent.toInt() / 100 else iIntoImagePx /* Use % width! */
        val iPixelHeight = iScanHeight / PixelVert.toInt()
        var iTop = 0

        if (myDirection === FlowDirection.TopDown) {
            // TopDown!
            for (iLoop in 0 until PixelVert.toInt()) {
                iTop = iTopOffset + iLoop * iPixelHeight
                rectPixels[iNewOffset] = Rect(iLeftOffset, iTop, iLeftOffset+iPixelWidth, iTop+iPixelHeight)
                iNewOffset++
            }
        } else {
            // BottomUp!
            for (iLoop in PixelVert.toInt() - 1 downTo 0) {
                iTop = iTopOffset + iLoop * iPixelHeight
                rectPixels[iNewOffset] = Rect(iLeftOffset, iTop, iLeftOffset+iPixelWidth, iTop+iPixelHeight)
                iNewOffset++
            }
        }

        /* return new array offset*/
        return iNewOffset
    }

    fun CalcBottomRectangles(
        PixelVert: UByte,
        PixelHori: UByte,
        iLeftOffset: Int,
        iTopOffset: Int,
        iScanWidth: Int,
        iScanHeight: Int,
        iIntoImagePx: Int,
        myDirection: FlowDirection,
        rectPixels: Array<Rect?>,
        iOffset: Int
    ): Int {
        var iNewOffset = iOffset

        val iPixelWidth = iScanWidth / PixelHori.toInt()
        val iPixelHeight =
            if (iIntoImagePx == 0) iScanHeight * this.InnerSizePercent.toInt() / 100 else iIntoImagePx /* Use % width */
        var iLeft = 0
        val iTop = iTopOffset + iScanHeight - iPixelHeight

        if (myDirection === FlowDirection.RightToLeft) {
            // RightToLeft!
            for (iLoop in PixelHori.toInt() - 1 downTo 0) {
                iLeft = iLeftOffset + iLoop * iPixelWidth
                rectPixels[iNewOffset] = Rect(iLeft, iTop, iLeft+iPixelWidth, iTop+iPixelHeight)
                iNewOffset++
            }
        } else {
            // LeftToRight!
            for (iLoop in 0 until PixelHori.toInt()) {
                iLeft = iLeftOffset + iLoop * iPixelWidth
                rectPixels[iNewOffset] = Rect(iLeft, iTop, iLeft+iPixelWidth, iTop+iPixelHeight)
                iNewOffset++
            }
        }

        /* return new array offset*/
        return iNewOffset
    }

    fun getColorFromPixelRegion(myBitmap: Bitmap, myRect: Rect?) : Int // iLeft: Int, iTop: Int, iRight: Int, iBottom: Int) : Int
    {
        var returnColor: Int? = 0;

        var totalRed: UInt = 0u;
        var totalGreen: UInt = 0u;
        var totalBlue: UInt = 0u;
        var totalPixels: UInt = 0u;

        try {
            var iIndex = 0
            val iYEnd = Capture.mBufHeight.minus(1)
            var iTop = myRect!!.top
            var iBottom = myRect!!.bottom

            if (Capture.mBufBlackLinesTop == -1) {
                // Detect Upper black border properly
                var iMaxLineScan = Capture.mBufHeight * 0.20
                var totalBytesSum: UInt = 0u
                for (y in 0 until iMaxLineScan.toInt() ) {
                    for (x in 0 until Capture.mBufRowStride ) {
                        totalBytesSum += (Capture.mBuffer!!.get(iIndex).toUByte())
                        iIndex += 1;
                    }
                    if (totalBytesSum > BorderThreshold){
                        Capture.mBufBlackLinesTop = y
                        break
                    }
                }
                // No border found? Set to iMaxLineScan
                if (Capture.mBufBlackLinesTop == -1)
                    Capture.mBufBlackLinesTop = iMaxLineScan.toInt()

                // Reset Index
                iIndex = 0
            }
            if (Capture.mBufBlackLinesBottom == -1) {
                // Detect Lower black border properly
                var iMaxLineScan = Capture.mBufHeight * 0.80
                var totalBytesSum: UInt = 0u
                for (y in Capture.mBufHeight downTo iMaxLineScan.toInt() ) {
                    for (x in 0 until Capture.mBufRowStride ) {
                        totalBytesSum += (Capture.mBuffer!!.get(iIndex).toUByte())
                        iIndex += 1;
                    }
                    if (totalBytesSum > BorderThreshold){
                        Capture.mBufBlackLinesBottom = Capture.mBufHeight - y
                        break
                    }
                }
                // No border found? Set to iMaxLineScan
                if (Capture.mBufBlackLinesBottom == -1)
                    Capture.mBufBlackLinesBottom = Capture.mBufHeight - iMaxLineScan.toInt()

                // Reset Index
                iIndex = 0
            }

            // Border correction
            if (iTop == 0 && Capture.mBufBlackLinesTop > 0) {
                iTop += Capture.mBufBlackLinesTop
                iBottom += Capture.mBufBlackLinesTop
            }
            if (iBottom == Capture.mBufHeight && Capture.mBufBlackLinesBottom > 0) {
                iTop -= Capture.mBufBlackLinesBottom
                iBottom -= Capture.mBufBlackLinesBottom
            }

            val iHeight = iBottom - iTop
            val iWidth = myRect!!.right - myRect!!.left


            for (y in 0 until iYEnd) {
//                if (iIndex > Capture.mBuffer!!.count())
//                    Log.e(Device.TAG, "getColorFromPixelRegion Invalid Index")

                if (y >= iTop && y <= iBottom)
                {
                    // Skip until pixels we need
                    if (myRect!!.left>0)
                        iIndex += (myRect!!.left.minus(1) * 4);

                    for (x in myRect!!.left until myRect!!.right) {
//                        var bRed =  Capture.mBuffer!!.get(iIndex).toUByte()
                        totalRed += (Capture.mBuffer!!.get(iIndex).toUByte())
                        totalGreen += (Capture.mBuffer!!.get(iIndex.plus(1)).toUByte())
                        totalBlue += (Capture.mBuffer!!.get(iIndex.plus(2)).toUByte())
                        totalPixels += 1u
                        iIndex += 4;
                    }

                    /* Fast Forward Pixels */
                    iIndex += (Capture.mBufWidth - myRect!!.left - iWidth) * 4;
                }
                else
                {
                    /* Fast Forward PixelLine */
                    iIndex += (Capture.mBufWidth * 4);
                }
            }

        } catch (ex: Exception) {
            Log.e(Device.TAG, "getColorFromPixelRegion Exception ${ex.message}")
        }

        /* Calc avarage color */
        totalRed = if (totalRed > 0u) totalRed / totalPixels else 0u
        totalGreen = if (totalGreen > 0u) totalGreen / totalPixels else  0u
        totalBlue = if (totalBlue > 0u) totalBlue / totalPixels else 0u

        /* Check for Value Gain */
        /* DONE ON CONTROLLER NOW!
        if (this.ValueGainPercent > 100)
        {
            totalRed = (totalRed * this.ValueGainPercent.toUInt() ) / 100u;
            totalGreen = (totalGreen * this.ValueGainPercent.toUInt()) / 100u;
            totalBlue = (totalBlue * this.ValueGainPercent.toUInt()) / 100u;
        }
        /* Check for ColorBoost */
        if (this.ColorBoostPercent.toInt() > 0)
        {
            if (totalRed > totalGreen && totalRed > totalBlue)
            {
                totalRed = (totalRed * (100u + this.ColorBoostPercent.toUInt()) / 100u);
                totalGreen = (totalGreen * (100u - this.ColorBoostPercent.toUInt()) / 100u);
                totalBlue = (totalBlue * (100u - this.ColorBoostPercent.toUInt()) / 100u);
            }
            else if (totalGreen > totalRed && totalGreen > totalBlue)
            {
                totalGreen = (totalGreen * (100u + this.ColorBoostPercent.toUInt()) / 100u);
                totalRed = (totalRed * (100u - this.ColorBoostPercent.toUInt()) / 100u);
                totalBlue = (totalBlue * (100u - this.ColorBoostPercent.toUInt()) / 100u);
            }
            else if (totalBlue > totalGreen && totalBlue > totalRed)
            {
                totalBlue = (totalBlue * (100u + this.ColorBoostPercent.toUInt()) / 100u);
                totalGreen = (totalGreen * (100u - this.ColorBoostPercent.toUInt()) / 100u);
                totalRed = (totalRed * (100u - this.ColorBoostPercent.toUInt()) / 100u);
            }
        }
        */

        /* Correct values if they are above 255 */
        if (totalRed > 255u)
            totalRed = 255u;
        if (totalGreen > 255u)
            totalGreen = 255u;
        if (totalBlue > 255u)
            totalBlue = 255u;


        /* Create solid brush from avarage color */
        returnColor = Color.argb(255, totalRed.toInt(), totalGreen.toInt(), totalBlue.toInt())
        //  ()   new SolidBrush(System.Drawing.Color.FromArgb(255, iRed, iGreen, iBlue));

        return returnColor;
    }

}

