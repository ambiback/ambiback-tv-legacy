package com.ambiback.app.globals

import android.content.Context
import android.util.Log
import com.ambiback.app.CaptureActivity
import com.ambiback.app.Device
import com.ambiback.app.DeviceAmbiBack
import com.ambiback.app.MainActivity
import com.ambiback.app.network.UdpServerThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class Static {

    // Static objects and functions
    companion object {
        private val TAG = Static::class.qualifiedName

        // Public Static variables
        var gNewFeatures = true

        // Public Global vars
        var mServiceStarted = false

        var gConfigFile: String = "com.ambiback.app.configfile"
        var gApplicationTitle = "AmbiBack TV Client"

        var gDeviceMap = mutableMapOf<String, Device>()
        var gDeviceList: MutableList<Device> = ArrayList()

        // Predefined defaults
        var gUdpPortNumberAmbiBack = 1337

        // Global Object helpers
        var gUdpServer: UdpServerThread? = null

        // Options
        var gbLoaded = false
        var gbMirrorSupportEnabled: Boolean = false
        var gbMirrorEnabled: Boolean = false

        // Global GUI Helpers
        var gCaptureActivity: CaptureActivity? = null
        var gMainActivity: MainActivity? = null
        lateinit var gMainContext: Context // Helper variable to set the MainContext

        // var gMainContext: Context? = null

        fun InitApplication() {
            // Load Config
            Static.InitConfig()

            // Start UDP Listener
            Static.StartUdpListener()

            // Loading done
            Static.gbLoaded = true
        }

        fun InitConfig() {
            Log.i(TAG, "InitConfig: ENTER")
            val sharedPref = gMainActivity?.getSharedPreferences(
                gConfigFile, Context.MODE_PRIVATE)

            // Read Config values
            Static.gbMirrorEnabled = sharedPref!!.getBoolean("gbAmbibackEnabled", false)

            // TODO INIT DEVICE LIST
            // gDeviceList
            val deviceids = sharedPref!!.getStringSet ("gaDevicesList", mutableSetOf<String>())
            val deviceips = sharedPref!!.getStringSet ("gaDevicesListIPs", mutableSetOf<String>())
            Log.v(TAG, "InitConfig: Store device IDs: " + deviceids.toString() )
            Log.v(TAG, "InitConfig: Store device IPs: " + deviceips.toString() )

            var iDx = 0
            for(szdeviceid in deviceids){
               Log.v(TAG, "InitConfig: restore device ID {szdeviceid}" )

                // Create new Device obj and fill/init
                val newDevice = DeviceAmbiBack()
                try{
                    newDevice.IPAddress = deviceips.elementAt(iDx)
                    Log.v(TAG, "InitConfig: restore device ID ${szdeviceid} with IP  ${newDevice.IPAddress}" )
                } catch (ex: Exception) {
                    Log.e(TAG, "InitConfig: IP Address for ${szdeviceid} unknown with err: " + ex.message )
                }

                // Set Device ID directly!
                newDevice.setRealDeviceID(szdeviceid)

                // Read Cached config first
                newDevice.LoadConfig()
                Log.v(TAG, "InitConfig: restored device ID: " + szdeviceid + " DONE -> " + newDevice.DeviceName)

                // Add device on successfull load
                AddDevice(newDevice, false)

                // Try loading config from controller
                newDevice.LoadConfigFromController()

                iDx++
            }
        }

        fun SaveConfig() {
            if (gMainActivity == null) {
                Log.i(TAG, "SaveConfig: SKIP - gMainActivity == NULL")
            } else {
                Log.i(TAG, "SaveConfig: ENTER")
                val sharedPref = gMainActivity?.getSharedPreferences(
                    gConfigFile, Context.MODE_PRIVATE)
                // Save config values
                with (sharedPref!!.edit()) {
                    Static.gbMirrorEnabled?.let { putBoolean("gbAmbibackEnabled", it) }

                    // SAVE Device IDs/IPs
                    val deviceids = mutableSetOf<String>()
                    val deviceips = mutableSetOf<String>()
                    for ((deviceKey, deviceValue) in Static.gDeviceMap) {
                        deviceids.add(deviceValue.DeviceID)
                        deviceips.add(deviceValue.IPAddress)
                        // deviceips[deviceValue.InternalDeviceID] = deviceValue.IPAddress
                    }

                    Log.v(TAG, "SaveConfig: Store device IDs: " + deviceids.toString() )
                    Log.v(TAG, "SaveConfig: Store device IPs: " + deviceips.toString() )
                    putStringSet("gaDevicesList", deviceids)
                    putStringSet("gaDevicesListIPs", deviceips)
//                    Static.gDeviceMap?.let { putStringSet("gaDevicesList", deviceids) }
//                    Static.gDeviceMap?.let { putStringSet("gaDevicesListIPs", deviceips) }

                    // Save changes
                    commit()
                }
            }
        }

        fun StartUdpListener() {
            if(gUdpServer == null){
                gUdpServer = UdpServerThread(Static.gUdpPortNumberAmbiBack);
            }
            gUdpServer?.start()
        }

        fun StopUdpListener() {
            if(gUdpServer != null){
                gUdpServer?.setRunning(false)
                gUdpServer = null;
            }
        }

        fun DiscoverDevices() {
            GlobalScope.launch {
                gUdpServer?.sendBroadcast(UdpServerThread.AMBIBACK_BROADCAST_DISCOVER)
            }
        }

        fun CleanupDevices() {
            Log.i(Static.TAG, "CleanupDevices: check ${Static.gDeviceMap.count()} devices")

            val cleanupdevices = mutableSetOf<Device>()
            for ((deviceKey, deviceValue) in Static.gDeviceMap) {
                if (deviceValue.IsOnline == false){
                    cleanupdevices.add(deviceValue)
                }
            }

            if (cleanupdevices.count() > 0) {
                cleanupdevices.forEach { deviceValue ->
                    Log.i(Static.TAG, "CleanupDevices: Remove Device ${deviceValue.DeviceID}")
                    Static.RemoveDevice(deviceValue);
                    Static.gDeviceList.remove(deviceValue)

                    // Update Entry in Main Drawer
                    Static.gMainActivity?.runOnUiThread(
                        Runnable {
                            // Code to run in GUI thread
                            Static.gMainActivity!!.RemoveDeviceFromNaView(deviceValue)
                            Log.i(Static.TAG, "CleanupDevices: after runOnUiThread for id: ${deviceValue.DeviceID}")
                        }
                    );
                }
            }

            // Sort list by DeviceName
            Static.gDeviceList.sortBy { device -> device.DeviceName }

            // Save Config as well!
            Static.SaveConfig()
        }

        fun CleanupAllDevices() {
            Log.i(Static.TAG, "CleanupAllDevices: check ${Static.gDeviceMap.count()} devices")

            val cleanupdevices = mutableSetOf<Device>()
            for ((deviceKey, deviceValue) in Static.gDeviceMap) {
                cleanupdevices.add(deviceValue)
            }

            if (cleanupdevices.count() > 0) {
                cleanupdevices.forEach { deviceValue ->
                    Log.i(Static.TAG, "CleanupAllDevices: Remove Device ${deviceValue.DeviceID}")
                    Static.RemoveDevice(deviceValue)
                    Static.gDeviceList.remove(deviceValue)

                    // Update Entry in Main Drawer
                    Static.gMainActivity?.runOnUiThread(
                        Runnable {
                            // Code to run in GUI thread
                            Static.gMainActivity!!.RemoveDeviceFromNaView(deviceValue)
                            Log.i(Static.TAG, "CleanupAllDevices: after runOnUiThread for id: ${deviceValue.DeviceID}")
                        }
                    );
                }
            }

            // Sort list by DeviceName
            Static.gDeviceList.sortBy { device -> device.DeviceName }

            // Save Config as well!
            Static.SaveConfig()
        }

        @JvmStatic fun AddDevice(newDevice: Device, autoSave: Boolean = true) {
            if (gDeviceMap.containsKey(newDevice.DeviceID)) {
                Log.i(Static.TAG, "AddDevice: UPDATE device to list with id: ${newDevice.DeviceID}")
                // Copy MenuID
                newDevice.MenuID = gDeviceMap[newDevice.DeviceID]!!.MenuID
                // remove old obj from list
                gDeviceList.remove(gDeviceMap[newDevice.DeviceID])
                // Update element in List
                gDeviceMap[newDevice.DeviceID] = newDevice
                // TODO UPDATE LIST AS WELL
                // gDeviceList.set(x, newDevice)
            } else {
                Log.i(Static.TAG, "AddDevice: ADD NEW device to list with id: ${newDevice.DeviceID}")
                // Add new element to list
                gDeviceMap.put(newDevice.DeviceID, newDevice)
            }
            // Always add to list
            gDeviceList.add(newDevice)

            // Sort list by DeviceName
            Static.gDeviceList.sortBy { device -> device.DeviceName }

            if (autoSave) {
                // Save Config as well!
                Static.SaveConfig()
            }

            // Set / Update Entry in Main Drawer
            Log.i(Static.TAG, "AddDevice: before runOnUiThread for id: ${newDevice.DeviceID}")
            Static.gMainActivity?.runOnUiThread(
                Runnable {
                    // Code to run in GUI thread
                    Static.gMainActivity!!.AddDeviceToNaView(newDevice)
                    Log.i(Static.TAG, "AddDevice: after runOnUiThread for id: ${newDevice.DeviceID}")
                }
            );
        }

        @JvmStatic fun RemoveDevice(delDevice: Device ) {
            if (gDeviceMap.containsKey(delDevice.DeviceID)) {
                Log.i(Static.TAG, "RemoveDevice: remove device with id: ${delDevice.DeviceID} from list")
                // Update element in List
                gDeviceMap.remove(delDevice.DeviceID)
            } else {
                // Element does not exist
                Log.w(Static.TAG, "RemoveDevice: element not in list with id: ${delDevice.DeviceID}")
            }
        }

   }

}