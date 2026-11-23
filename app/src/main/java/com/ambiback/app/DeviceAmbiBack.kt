package com.ambiback.app

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.ambiback.app.globals.CRC
import com.ambiback.app.globals.Static
import com.ambiback.app.globals.Static.Companion.gMainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket
import java.net.URL
import kotlin.system.measureTimeMillis

enum class AmbiBackDeviceTypes(val id: Int, val internaltype: String, val internalname: String, val displayname: String) {
    unknown(-1, "Unknown", "UNKNOWN", "Unknown AmbiBack Controller"),
    esp32bridge(0, "BRIDGE", "AmbiBack_Bridge_Controller", "AmbiBack Bridge"),
    esp32naked(1,"CONTROLLER", "AmbiBack_Pc_Controller", "AmbiBack LED Controller"),
    camwroover(2, "CAM","AmbiBack_Cam_wr_Controller", "AmbiBack Cam Controller"),
    camttgocam(20, "CAM","AmbiBack_Cam_tt_Controller", "AmbiBack Cam Controller"),
    camttgocammic(21, "CAM","AmbiBack_Cam_tm_Controller", "AmbiBack Cam Controller"),
    camttgocamjournal(22, "CAM","AmbiBack_Cam_jr_Controller", "AmbiBack Cam Controller"),
    camttgocamplus(23, "CAM","AmbiBack_Cam_pl_Controller", "AmbiBack Cam Controller"),
    camm5cam(30, "CAM","AmbiBack_Cam_m5_Controller", "AmbiBack Cam Controller"),
    camm5xcam(31, "CAM","AmbiBack_Cam_5x_Controller", "AmbiBack Cam Controller"),
    m5atomlite(32, "CONTROLLER","AmbiBack_Pc_M5AL_Controller", "AmbiBack LED Controller"),
    m5atommatrix(33, "CONTROLLER","AmbiBack_Pc_M5AX_Controller", "AmbiBack LED Controller"),
    camm5timerx(34, "CAM","AmbiBack_Cam_5tx_Controller", "AmbiBack Cam Controller"),
}

class DeviceAmbiBack() : Device() {
    companion object Static {
        private val TAG = DeviceAmbiBack::class.qualifiedName
        private val TCP_HEAD_LEN = 8

        var client = OkHttpClient()
    }

    var AmbiBackDeviceType : AmbiBackDeviceTypes = AmbiBackDeviceTypes.unknown
    var LastLedMode : Int = 0;

    var TcpPort: Int = 1337
    var m_socket: Socket? = null
    var _pixelVert: UByte = 22u //8
    var _pixelHori: UByte = 40u //16
    var _pixelTotalPort1: UByte = (_pixelVert + _pixelVert + _pixelHori + _pixelHori).toUByte() // (this._pixelHori * 2 + this._pixelVert * 2);
    var _flexSetupPort1: UByte = LedSetups.Left_Clock_bottom_left_up.id.toUByte()

    init {
        Log.i(DeviceAmbiBack.TAG, "init")
        // Set defaults
        this.DeviceType = com.ambiback.app.DeviceType.AmbiBackController
        this.DeviceName = "Unknown Controller"
        // this.setDeviceID()
        this.initClass()

        this.IPAddress = "127.0.0.1"
    }

    fun getAmbiBackDeviceTypes(szInternalName: String) : AmbiBackDeviceTypes {
        enumValues<AmbiBackDeviceTypes>().forEach {
            if (it.internalname == szInternalName)
                return it;
        }
        return AmbiBackDeviceTypes.unknown
    }

    override fun setDeviceID() {
        this.setRealDeviceID("")
    }

    override fun setRealDeviceID(overrideDeviceID: String)
    {
        if (overrideDeviceID.length > 0) {
            // USe ID directly
            DeviceID = overrideDeviceID
        } else {
            /* Generate Device.kt ID from these values */
            DeviceID = this.DeviceType.internalname + "_"
            DeviceID += if (this.InternalDeviceID.length > 0) { this.InternalDeviceID } else { this.IPAddress}
            DeviceID += if (this.InternalNetworkMAC.length > 0) { this.InternalNetworkMAC } else { "" }
        }

        Log.i(TAG, "setRealDeviceID: " + DeviceID)

        /* Update config file name  */
        ConfigFile = Device.BASENAME + this.DeviceID + ".cfg"
    }


    override fun initClass() {
        // Set other defaults
        this.FramesPerSecond = 10;
        this.MaxFramesPerSecond = 30u;
        this.FrameDelay = 60;
    }

    override fun prepareClass() {
        // Init Screen Scan Areas
        this.rectScanPixels = arrayOfNulls<Rect?>(this._pixelTotalPort1.toInt())
        this.CalcRectangles(0, 0, CaptureService.SCREENWIDTH, CaptureService.SCREENHEIGHT, 0, this.rectScanPixels);
        this.CalcCorners(this._pixelTotalPort1.toInt());
    }

    override fun LoadConfig() {
        // Tries to load configuration from shared prefs
        val devicePref = gMainActivity?.getSharedPreferences(
            this.ConfigFile, Context.MODE_PRIVATE)
        Log.i(TAG, "LoadConfig: Loading " + this.ConfigFile + " for " + this.DeviceName)

        // Global Device properties
        // this.DeviceID = devicePref!!.getString("DeviceID", "")
        this.InternalDeviceID = devicePref!!.getString("InternalDeviceID", this.InternalDeviceID)
        this.InternalNetworkMAC = devicePref!!.getString("InternalNetworkMAC", this.InternalNetworkMAC)
        this.DeviceName = devicePref!!.getString("DeviceName", this.DeviceName)
        this.DeviceVersion = devicePref!!.getString("DeviceVersion", this.DeviceVersion)
        this.IPAddress = devicePref!!.getString("IPAddress", this.IPAddress )
        if (AmbiBackDeviceType.id < 0 ) {
            // Only load if not set already
            this.AmbiBackDeviceType = this.getAmbiBackDeviceTypes( devicePref!!.getString("AmbiBackDeviceType", this.AmbiBackDeviceType.internalname ))
        }

        // Read Stored Config values
        this.UseDevice = devicePref!!.getBoolean("UseDevice", false)
        this._pixelVert =  devicePref!!.getInt("_pixelVert", 8).toUByte()
        this._pixelHori =  devicePref!!.getInt("_pixelHori", 16).toUByte()
        this._flexSetupPort1 =  devicePref!!.getInt("_flexSetupPort1", LedSetups.Left_Clock_bottom_left_up.id).toUByte()
        this.CalcTotalPixels()

        this.FrameLength = devicePref!!.getInt("FrameLength", 10).toUByte()
        this.FrameDelay = devicePref!!.getInt("FrameDelay", 60)
        this.FramesPerSecond = devicePref!!.getInt("FramesPerSecond", 10)
        this.InnerSizePercent = devicePref!!.getInt("InnerSizePercent", 10).toUByte()
        this.ColorBoostPercent = devicePref!!.getInt("ColorBoostPercent", 20).toUByte()
        this.ColorBoostPercentMin = devicePref!!.getInt("ColorBoostPercentMin", 20).toUByte()

        // Read device properties
        this.ColorRedPercent = devicePref!!.getInt("ColorRedPercent", 100).toUByte()
        this.ColorGreenPercent= devicePref!!.getInt("ColorGreenPercent", 100).toUByte()
        this.ColorBluePercent= devicePref!!.getInt("ColorBluePercent", 100).toUByte()
        this.ValueGainPercent= devicePref!!.getInt("MaxBrightnessPercent", 100)
        this.MaxBrightnessPercent= devicePref!!.getInt("MaxBrightnessPercent", 0).toUByte()
        this.MinimumLuminosityPercent= devicePref!!.getInt("MinimumLuminosityPercent", 0).toUByte()

        Log.i(TAG, "LoadConfig: UseDevice " + this.UseDevice + " for " + this.DeviceName)
    }

    override fun SaveConfig() {
        // Tries to save configuration into shared prefs
        val sharedPref = gMainActivity?.getSharedPreferences(
            this.ConfigFile, Context.MODE_PRIVATE)
        Log.i(TAG, "SaveConfig: Saving " + this.ConfigFile  + " for " + this.DeviceName)

        // Save config values
        val prefEditor = sharedPref!!.edit()

        // Global Device properties
        prefEditor.putString("InternalDeviceID", this.InternalDeviceID)
        prefEditor.putString("InternalNetworkMAC", this.InternalNetworkMAC)
        prefEditor.putString("DeviceName", this.DeviceName)
        prefEditor.putString("DeviceVersion", this.DeviceVersion)
        prefEditor.putString("IPAddress", this.IPAddress)
        prefEditor.putString("AmbiBackDeviceType", this.AmbiBackDeviceType.internalname )

        // Save APP Config values
        prefEditor.putBoolean("UseDevice", this.UseDevice)
        prefEditor.putInt("_pixelVert", this._pixelVert.toInt())
        prefEditor.putInt("_pixelHori", this._pixelHori.toInt())
        prefEditor.putInt("_pixelTotalPort1", this._pixelTotalPort1.toInt())
        prefEditor.putInt("_flexSetupPort1", this._flexSetupPort1.toInt())

        prefEditor.putInt("FrameLength", this.FrameLength.toInt())
        prefEditor.putInt("FrameDelay", this.FrameDelay.toInt())
        prefEditor.putInt("FramesPerSecond", this.FramesPerSecond.toInt())
        prefEditor.putInt("InnerSizePercent", this.InnerSizePercent.toInt())
        prefEditor.putInt("ColorBoostPercent", this.ColorBoostPercent.toInt())
        prefEditor.putInt("ColorBoostPercentMin", this.ColorBoostPercentMin.toInt())

        // Save device properties
        prefEditor.putInt("ColorRedPercent", this.ColorRedPercent.toInt())
        prefEditor.putInt("ColorGreenPercent", this.ColorGreenPercent.toInt())
        prefEditor.putInt("ColorBluePercent", this.ColorBluePercent.toInt())
        prefEditor.putInt("ValueGainPercent", this.ValueGainPercent.toInt())
        prefEditor.putInt("MaxBrightnessPercent", this.MaxBrightnessPercent.toInt())
        prefEditor.putInt("MinimumLuminosityPercent", this.MinimumLuminosityPercent.toInt())

        // Finally Save
        prefEditor.apply()
    }

    fun LoadConfigFromController() {
        Log.i(TAG, "LoadConfigFromController: ENTER " + this.IPAddress)
        var myDevice: DeviceAmbiBack = this

        GlobalScope.launch {
            var szUrl = "http://" + myDevice.IPAddress + "/getparamatersasjson"

            try {
                // Get JSON String from Controller
                var url: URL = URL(szUrl)
                var szConfigFromController = url.readText()

                if (myDevice.InitConfigFromJson(szConfigFromController)) {
                    Log.i(TAG, myDevice.DeviceID + ": LoadConfigFromController: (" + szUrl + ") SUCCESS" )
                    myDevice.SaveConfig()
                    myDevice.IsRemoteConfigLoaded = true // Successfully processed
                    myDevice.IsOnline = true
                    gMainActivity?.runOnUiThread(
                        Runnable {
                            // Code to run in GUI thread
                            gMainActivity?.updateCurrentFragmentDeviceList()
                        }
                    );
                } else {
                    Log.e(TAG, myDevice.DeviceID + ": LoadConfigFromController: (" + szUrl + ") JSON PARSE FAILED")
                    myDevice.IsRemoteConfigLoaded = false // Successfully processed
                }
            }
            catch (ex: Exception) {
                Log.e(TAG, myDevice.DeviceID + ": LoadConfigFromController: (" + szUrl + ") FAILED " + ex.message )
            }
        }
    }

    fun InitConfigFromJson(szJson: String): Boolean {
        try {
            Log.i(TAG, "InitConfigFromJson: " + this.IPAddress)
            val jsonObj = JSONObject( szJson)

            // Read Global Device properties
// TODO            this.DeviceType = DeviceType.AmbiBackController
            this.InternalDeviceID =jsonObj.getString("deviceid").trim({ it <= ' ' })
            this.InternalNetworkMAC = jsonObj.getString("devicemac")!!.trim({ it <= ' ' })
            this.AmbiBackDeviceType = this.getAmbiBackDeviceTypes( jsonObj.getString("devicetype")!!.trim({ it <= ' ' }) )
            this.DeviceName =jsonObj.getString("devicename").trim({ it <= ' ' })
            this.DeviceVersion =jsonObj.getString("deviceversion").trim({ it <= ' ' })
            if (!jsonObj.isNull("deviceip") )
                this.IPAddress = jsonObj.getString("deviceip").trim({ it <= ' ' })
            // Set Device ID again
            this.setDeviceID()

            // Load local config first
            this.LoadConfig()

            // Make sure to overwrite certain properties from CACHE!
            this.DeviceName =jsonObj.getString("devicename").trim({ it <= ' ' })
            this.DeviceVersion =jsonObj.getString("deviceversion").trim({ it <= ' ' })
            if (!jsonObj.isNull("deviceip") )
                this.IPAddress = jsonObj.getString("deviceip").trim({ it <= ' ' })

            // Read Config values
            this.FrameLength = jsonObj.getInt("framelength").toUByte()
            this.FrameDelay = jsonObj.getInt("framedelay")
            this.FramesPerSecond = (1000 / this.FrameDelay.toInt())

            // LED Properties
            this._pixelVert = jsonObj.getInt("vertical").toUByte()
            this._pixelHori = jsonObj.getInt("horizontal").toUByte()
            this._flexSetupPort1 = jsonObj.getInt("direction").toUByte()
            this.CalcTotalPixels()

            // Color Properties
            this.ColorBoostPercent = jsonObj.getInt("colorboost").toUByte()
            this.ColorBoostPercentMin = jsonObj.getInt("colorboostmin").toUByte()

            // Other properties not needed
            Log.i(TAG, this.DeviceID + ": InitConfigFromJson: SUCCESS " + this.IPAddress)

            // Successfully loaded
            return true;
        }
        catch (ex: Exception) {
            Log.e(TAG, this.DeviceID + ": InitConfigFromJson: FAILED " + ex.message )
        }

        // Failed to load
        return false;
    }
        // Private functions
    private fun CalcTotalPixels()
    {
        this._pixelTotalPort1 = ((this._pixelVert* 2u) + (this._pixelHori*2u)).toUByte();
        Log.i(TAG, this.DeviceID + ": CalcTotalPixels: ${this._pixelTotalPort1}, ${this._pixelVert} ${this._pixelHori} ")

        // Prepare Class objects again!
        this.prepareClass()
    }

    // Network Functions

    fun connect(){
        if (m_socket == null) {
            try {
                Log.i(DeviceAmbiBack.TAG, "connect try $IPAddress:$TcpPort")
                m_socket = Socket(IPAddress, TcpPort)
                Log.i(DeviceAmbiBack.TAG, "connect success $IPAddress:$TcpPort")
                m_socket?.sendBufferSize = 1024
                m_socket?.tcpNoDelay = true
                m_socket?.setPerformancePreferences(0, 2, 1)
                // Calc FrameRate Vars.
                this.InitFrameRateVars()
            } catch (ex: Exception) {
                Log.e(DeviceAmbiBack.TAG, "EXCEPTION while connect to $IPAddress with err ${ex.cause} ")
            }
            if (m_socket != null)
                Log.i(DeviceAmbiBack.TAG, "Connected to $IPAddress:$TcpPort")
            else
                Log.i(DeviceAmbiBack.TAG, "FAILED to connect to $IPAddress:$TcpPort")
        } else {
            Log.v(DeviceAmbiBack.TAG, "Already connected to $IPAddress:$TcpPort")
        }
    }

    fun disconnect() {
        if (m_socket != null)
            m_socket?.close()
        m_socket = null
    }

    fun processLedData() : Boolean {
        var bReturn: Boolean = false;
        if (Capture.mBitmap != null)
        {
            try {
                this.timeTakenms = measureTimeMillis {
                    // Scan Images by each Pixel for Colors
                    for (x in 0 until this._pixelTotalPort1.toInt()) {
                        iOutPixels?.set(x, this.getColorFromPixelRegion(
                            Capture.mBitmap!!,
                            this.rectScanPixels!![x])
                            )
                    }
                }

                bReturn = true
            } catch (ex: Exception) {
                Log.e(TAG, this.DeviceID + ": calcLedData Exception: " + ex.message)
            }
        }

        return bReturn
    }

    fun sendLedData() {
        val bPixelStart: UByte = 0x00u // StartIndex is 0x00 (where user configured his start!

        if (m_socket != null) {
            val networkReader = BufferedReader(InputStreamReader(m_socket?.getInputStream()))
            val networkDataOutput = DataOutputStream(m_socket?.getOutputStream())

            // Test data
            val byteCount = TCP_HEAD_LEN + (4 * _pixelTotalPort1.toInt())
            var payload: UByteArray = ubyteArrayOf(0x00u,0xFFu,0x13u,0x37u,this._pixelTotalPort1,bPixelStart,0x00u,0x00u)
            for (bPixelCurrent in 0..(this._pixelTotalPort1.toInt()-1)){
                payload = payload.plus(
                    ubyteArrayOf(
                        0x00u,
                        Color.red(iOutPixels?.get(bPixelCurrent)!!.toInt()).toUByte(),
                        Color.green(iOutPixels?.get(bPixelCurrent)!!.toInt()).toUByte(),
                        Color.blue(iOutPixels?.get(bPixelCurrent)!!.toInt()).toUByte())
                    )
            }

            // Set Checksum byte
            payload[6] = 0x01u
            payload[7] = CRC.crc_8(payload, byteCount, TCP_HEAD_LEN)

            TCP_HEAD_LEN

            try {
                // Send to controller
                networkDataOutput.write(payload.toByteArray())
                networkDataOutput.flush()

                Log.v(TAG, this.DeviceID + ": sendLedData success for ${this._pixelTotalPort1} Pixels,CRC: " + Integer.toHexString(payload[7].toInt()) )
            } catch (e: Exception) {
                Log.i(TAG, this.DeviceID + ": sendLedData failed with err ${e.message}")
                this.disconnect()
            }
        }
    }

    // Pixel Processing functions
    fun CalcRectangles(
        iLeftOffset: Int,
        iTopOffset: Int,
        iScanWidth: Int,
        iScanHeight: Int,
        iIntoImagePx: Int,
        rectPixels: Array<Rect?>
    ) {
        var iOffset = 0

        // CounterClockWise Bottom Right To Up
        if (this._flexSetupPort1.toInt() === LedSetups.Right_Cclock_bottom_right_up.id.toInt()) {
            iOffset = CalcRightRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.BottomUp,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcTopRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.RightToLeft,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcLeftRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.TopDown,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcBottomRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.LeftToRight,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Top_Cclock_top_right_left.id.toInt()) {
            iOffset = CalcTopRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.RightToLeft,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcLeftRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.TopDown,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcBottomRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.LeftToRight,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcRightRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.BottomUp,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Left_Cclock_top_left_down.id.toInt()) {
            iOffset = CalcLeftRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.TopDown,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcBottomRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.LeftToRight,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcRightRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.BottomUp,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcTopRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.RightToLeft,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Bottom_Cclock_bottom_left_right.id.toInt()) {
            iOffset = CalcBottomRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.LeftToRight,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcRightRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.BottomUp,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcTopRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.RightToLeft,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcLeftRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.TopDown,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Right_Clock_top_right_down.id.toInt()) {
            iOffset = CalcRightRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.TopDown,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcBottomRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.RightToLeft,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcLeftRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.BottomUp,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcTopRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.LeftToRight,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Top_Clock_top_left_right.id.toInt()) {
            iOffset = CalcTopRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.LeftToRight,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcRightRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.TopDown,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcBottomRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.RightToLeft,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcLeftRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.BottomUp,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Left_Clock_bottom_left_up.id.toInt()) {
// TODODODODOD FIX ALL OTHER CALCS! we need to calc based obn the BACK screen!
            iOffset = CalcLeftRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.BottomUp,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcTopRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.LeftToRight,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcRightRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.TopDown,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcBottomRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.RightToLeft,
                    rectPixels,
                    iOffset
                )
        } else if (this._flexSetupPort1.toInt() === LedSetups.Bottom_Clock_bottom_right_left.id.toInt()) {
            iOffset = CalcBottomRectangles(
                this._pixelVert,
                this._pixelHori,
                iLeftOffset,
                iTopOffset,
                iScanWidth,
                iScanHeight,
                iIntoImagePx,
                FlowDirection.RightToLeft,
                rectPixels,
                iOffset
            )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcLeftRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.BottomUp,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcTopRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.LeftToRight,
                    rectPixels,
                    iOffset
                )
            if (this._pixelTotalPort1.toInt() > iOffset)
                iOffset = CalcRightRectangles(
                    this._pixelVert,
                    this._pixelHori,
                    iLeftOffset,
                    iTopOffset,
                    iScanWidth,
                    iScanHeight,
                    iIntoImagePx,
                    FlowDirection.TopDown,
                    rectPixels,
                    iOffset
                )
        }
        // ClockWise Bottom Right to Left
        // ClockWise Bottom Left to Up
        // ClockWise Top Left to Right
        // ClockWise Top Right To Down
        // CounterClockWise Bottom Left to Right
        // CounterClockWise Top Left to Down
        // CounterClockWise Top Right to Left
    }


}