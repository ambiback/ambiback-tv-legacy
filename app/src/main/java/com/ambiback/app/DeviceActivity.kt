package com.ambiback.app

//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import android.support.v4.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ambiback.app.globals.Static
import com.ambiback.app.ui.device.DeviceFragment
import com.ambiback.app.ui.device.DeviceFragmentAmbiBack

class DeviceActivity : AppCompatActivity() {
    companion object {
        val ITEM_DEVICE_KEY = "item_key"
        val ITEM_DEVICE_TYPE = "item_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_activity)
        if (savedInstanceState == null) {

            // Get Item Type
            val itemType: String = intent.getStringExtra(DeviceActivity.ITEM_DEVICE_TYPE)
            val itemKey: String = intent.getStringExtra(DeviceActivity.ITEM_DEVICE_KEY)

            // Create fragment based on type

            var fragment: Fragment? = null
            if (itemType == DeviceType.AmbiBackController.toString()) {
                fragment = DeviceFragmentAmbiBack.newInstance()
                fragment.currentDevice = Static.gDeviceMap[itemKey]
            } else {
                fragment = DeviceFragment.newInstance()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment /*DeviceFragment.newInstance()*/ )
                .commitNow()
        }
    }

}
