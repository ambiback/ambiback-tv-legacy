package com.ambiback.app.ui

//import android.support.v7.widget.RecyclerView


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ambiback.app.Device
import com.ambiback.app.DeviceAmbiBack
import com.ambiback.app.R
import com.ambiback.app.globals.Static
import com.ambiback.app.ui.DevicesListFragment.OnListFragmentInteractionListener
import kotlinx.android.synthetic.main.fragment_device_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

/**
 * [RecyclerView.Adapter] that can display a [Device] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class DeviceRecyclerViewAdapter(
    private val mValues: List<Device>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder>() {

    // Static Properties
    companion object {
        private val TAG = DeviceRecyclerViewAdapter::class.qualifiedName
    }

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Device
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_device_item, parent, false)

        //Set on click listener for each item view
        view.setOnClickListener(View.OnClickListener {
// TODO handle background change
        })

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mDeviceName.text = item.DeviceName
        holder.mDeviceIP.text = item.IPAddress

        if (item is DeviceAmbiBack) {
            var itemAmbiBack: DeviceAmbiBack = item
            if (itemAmbiBack.AmbiBackDeviceType.internaltype == "CAM") {
                holder.mDeviceButton.setImageResource(R.drawable.icon_ambiback_controller_cam)
                holder.mDeviceSwitchFeature.text = "Enable Camera"
                // Enable / Disable for CAM Mode
                if (itemAmbiBack.LastLedMode == 3)
                    holder.mDeviceSwitchFeature.isChecked = true
                else
                    holder.mDeviceSwitchFeature.isChecked = false
            } else if (itemAmbiBack.AmbiBackDeviceType.internaltype == "CONTROLLER") {
                holder.mDeviceButton.setImageResource(R.drawable.icon_ambiback_controller)
                holder.mDeviceSwitchFeature.text = "Enable Screen mirroring"
                if (Static.gNewFeatures == false || Static.gbMirrorSupportEnabled == false) {
                    // Hide new Features
                    holder.mDeviceSwitchFeature.visibility = View.GONE
                }

                holder.mDeviceSwitchFeature.isChecked = item.UseDevice
            } else if (itemAmbiBack.AmbiBackDeviceType.internaltype == "BRIDGE") {
                holder.mDeviceButton.setImageResource(R.drawable.icon_ambiback_controller_bridge)
                    holder.mDeviceSwitchFeature.text = "Enable Hue"
                // Enable / Disable for CAM Mode
                if (itemAmbiBack.LastLedMode == 1)
                    holder.mDeviceSwitchFeature.isChecked = true
                else
                    holder.mDeviceSwitchFeature.isChecked = false
            } else {
                Log.i(TAG, "onBindViewHolder: " + itemAmbiBack.AmbiBackDeviceType.internalname +", " + itemAmbiBack.DeviceName)
                holder.mDeviceSwitchFeature.visibility = View.GONE
            }
        }

        if (item.IsOnline) {
            holder.mFragmentDeviceItemLayout.setBackgroundResource(R.drawable.layout_green)
//            "@drawable/layout_green"
        }

        Log.i(TAG, "onCreateViewHolder: item = " + item.DeviceID )
        // Bind events
        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
        holder.mDeviceButton.setOnClickListener {
            mListener?.onListFragmentInteraction(item)
        }
        holder.mDeviceSwitchFeature.setOnCheckedChangeListener {
                buttonView,
                isChecked -> handleSwitchFeature(isChecked, item)
        }
    }

    override fun getItemCount(): Int = mValues.size

    fun handleSwitchFeature(isChecked: Boolean, myDevice: Device) {
        Log.i(TAG, "handleSwitchFeature: feature checked(" + isChecked + "): " + myDevice.DeviceID )
        if (myDevice is DeviceAmbiBack) {
            var deviceAmbiBack: DeviceAmbiBack = myDevice
            if (deviceAmbiBack.AmbiBackDeviceType.internaltype == "CAM") {
                GlobalScope.launch {
                    var szUrl = "http://" + deviceAmbiBack.IPAddress + "/setcolormode?mode=" + if(isChecked) "3" else "0"
                    try {
                        // Get JSON String from Controller
                        var url: URL = URL(szUrl)
                        var szResultsFromController = url.readText()
                        Log.i(TAG, deviceAmbiBack.DeviceID + ": handleSwitchFeature: (" + szUrl + ") RESULT " + szResultsFromController)
                    }
                    catch (ex: Exception) {
                        Log.e(TAG, deviceAmbiBack.DeviceID + ": handleSwitchFeature: (" + szUrl + ") FAILED " + ex.message )
                    }
                }
            } else if (deviceAmbiBack.AmbiBackDeviceType.internaltype == "CONTROLLER") {
                Log.i(TAG, "handleSwitchFeature: CONTROLLER item checked(" + isChecked + "): " + myDevice.DeviceID )
                myDevice.UseDevice = isChecked
                myDevice.SaveConfig()
            } else if (deviceAmbiBack.AmbiBackDeviceType.internaltype == "BRIDGE") {
                GlobalScope.launch {
                    var szUrl = "http://" + deviceAmbiBack.IPAddress + "/sethuesupport?value=" + if(isChecked) "1" else "0"
                    try {
                        // Get JSON String from Controller
                        var url: URL = URL(szUrl)
                        var szResultsFromController = url.readText()
                        Log.i(TAG, deviceAmbiBack.DeviceID + ": handleSwitchFeature: (" + szUrl + ") RESULT " + szResultsFromController)
                    }
                    catch (ex: Exception) {
                        Log.e(TAG, deviceAmbiBack.DeviceID + ": handleSwitchFeature: (" + szUrl + ") FAILED " + ex.message )
                    }
                }
            } else {
                Log.e(TAG, deviceAmbiBack.DeviceID + ": handleSwitchFeature: UNKNOWN DEVICE" )
            }
        }
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mDeviceButton: ImageButton = mView.item_Button
        val mDeviceName: TextView = mView.item_devicename
        val mDeviceIP: TextView = mView.item_deviceip
        val mDeviceSwitchFeature: Switch = mView.item_switchFeature

        val mFragmentDeviceItemLayout: LinearLayout = mView.item_Layout

        override fun toString(): String {
            return super.toString() + " '" + mDeviceName.text + "'"
        }
    }
}
