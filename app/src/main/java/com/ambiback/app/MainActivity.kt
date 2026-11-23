package com.ambiback.app

/*
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
*/
// import android.widget.Toolbar
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ambiback.app.globals.Static
import com.ambiback.app.ui.DevicesListFragment
import com.ambiback.app.ui.device.DeviceFragment
import com.ambiback.app.ui.device.DeviceFragmentAmbiBack
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_ambiback_mirror.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        DevicesListFragment.OnListFragmentInteractionListener
{
    // Private properties
    private lateinit var mMediaProjectionManager: MediaProjectionManager

    // Static Properties
    companion object {
        private val TAG = MainActivity::class.qualifiedName
        private val REQUEST_MEDIA_PROJECTION = 1
        var mActivityInitDone = false
        var mAndroidInitDone = false

        // Static fragments
        val fragmentMain: Fragment? = null // = MainFragment.newInstance()
        val fragmentDeviceList: Fragment = DevicesListFragment.newInstance(1)

        fun UpdateImage() {
            if (Capture.mBitmap != null)
                Static.gMainActivity?.UpdateImageReal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "ENTER onCreate" )

        // Top Most Init Code
        this.enableStrictMode()
        Static.gMainContext = this
        Static.gMainActivity = this
        CaptureService.mStaticMainActivity = this

        // Control Init Stuff
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set Action floating button
        // TODO decide about action button!
        val fab: FloatingActionButton = findViewById(R.id.fab)
        if (true) /* !Static.gNewFeatures)*/ {
            fab.hide()
        } else {
            fab.setOnClickListener { view ->
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }

        // Init Drawer
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        if (!mActivityInitDone){
            mActivityInitDone = true
        }

        // Init Application here
        if (Static.gbLoaded == false) {
            Static.InitApplication()
        }

        // Load/Replace primary fragment if Backstack is 0
        if (supportFragmentManager.backStackEntryCount == 0) {
            this.updateFragmentDeviceList()
        }
        // ---------------------------
/*
        // Init CaptureActivity
        if (Static.gCaptureActivity == null) {
            // Create CaptureActivity activity
            val newIntent = Intent(this@MainActivity, CaptureActivity::class.java)
            // Start Background Activity
            startActivityForResult(newIntent, REQUEST_MEDIA_PROJECTION)
        }
*/
        // Load Global Settings
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        Static.gbMirrorSupportEnabled = sharedPrefs.getBoolean("enablemirrorsupport", Static.gbMirrorSupportEnabled)
        Static.gbMirrorEnabled = sharedPrefs.getBoolean("enablemirror", Static.gbMirrorEnabled)

        // Autostart Ambibacklight
        if (Static.gbMirrorSupportEnabled &&
            Static.gbMirrorEnabled == true ) {
            handleSwitchAmbiBack(Static.gbMirrorEnabled)
        }

        // Perform initial device discovery
        Timer().schedule(1000){
            Static.DiscoverDevices()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        Log.i(TAG, "onPrepareOptionsMenu: ENTER" )
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Check for fragments on stack and show them first
            if (supportFragmentManager.backStackEntryCount > 0) {
                Log.i(TAG, "onBackPressed: popping backstack " + supportFragmentManager.backStackEntryCount )
                supportFragmentManager.popBackStackImmediate()
            } else {
                Log.i(TAG, "onBackPressed: nothing on backstack, calling super" + supportFragmentManager.backStackEntryCount)
                super.onBackPressed()
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i(TAG, "onCreateOptionsMenu: ENTER" )
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        val switchAmbiBackMenuItem = menu!!.findItem(R.id.app_bar_ambilight)
        val switchAmbiBackRelativeLayout = switchAmbiBackMenuItem.actionView.findViewById(R.id.app_bar_ambilight) as RelativeLayout
        val switchAmbiBack = switchAmbiBackRelativeLayout.getChildAt(0) as Switch

        // Hide if gNewFeatures is false!
        if (Static.gNewFeatures == false || Static.gbMirrorSupportEnabled == false) {
            switchAmbiBackMenuItem.setVisible(false);
        }

        // Bind events
        switchAmbiBack.setOnCheckedChangeListener {
                buttonView,
                isChecked -> handleSwitchAmbiBack(isChecked)
        }

        // Toggle Switch on if enabled
        if (Static.gbMirrorEnabled == true ) {
            switchAmbiBack.setChecked(Static.gbMirrorEnabled )
        }

        // Add dynamic devices to navbar
        val navView: NavigationView = findViewById(R.id.nav_view)
        val devMenu = navView!!.menu!!.findItem(R.id.nav_devices).subMenu
        for ((deviceKey, deviceValue) in Static.gDeviceMap) {
            if (deviceValue.MenuID != -1) {
                try {
                    var existingMenuItem: MenuItem = devMenu!!.getItem(deviceValue.MenuID)
                    if (existingMenuItem == null)
                        deviceValue.MenuID = -1
                } catch (ex: Exception) {
                    Log.e(TAG, "onCreateOptionsMenu caught Exception ${ex.message}", ex)
                    deviceValue.MenuID = -1
                }
            }

            // deviceValue.MenuID = -1 // Make sure not to reuse old menuid
            AddDeviceToNaView(deviceValue)
        }

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        Log.i(TAG, "onOptionsItemSelected: item selected " + item.itemId )
        // Handle settings clicke here
        return when (item.itemId) {
            R.id.action_settings  -> {
                // Create CaptureActivity activity
                val newIntent = Intent(this@MainActivity, SettingsActivity::class.java)
                // Start  Activity
                startActivity(newIntent)

                // ReLoad Global Settings
                val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                Static.gbMirrorSupportEnabled = sharedPrefs.getBoolean("enablemirrorsupport", Static.gbMirrorSupportEnabled)
                Static.gbMirrorEnabled = sharedPrefs.getBoolean("enablemirror", Static.gbMirrorEnabled)

                /*
                // Hide if gNewFeatures is false!
                if (Static.gNewFeatures == false || Static.gbMirrorSupportEnabled == false) {
                    val menu: Menu = findViewById(R.menu.main)
                    val switchAmbiBackMenuItem = menu!!.findItem(R.id.app_bar_ambilight)
                    switchAmbiBackMenuItem.setVisible(false);
                }
                */

                return true;
            }
            R.id.action_about  -> {
                // Create CaptureActivity activity
                val newIntent = Intent(this@MainActivity, aboutActivity::class.java)
                // Start  Activity
                startActivity(newIntent)

                return true;
            }

            R.id.action_discover -> {
                // Trigger Devices Scan
                Static.DiscoverDevices()

                return true;
            }
            R.id.action_cleanup -> {
                // Trigger Devices Scan
                Static.CleanupDevices()

                // Don't forget to update DeviceList
                updateCurrentFragmentDeviceList()

                return true;
            }
            R.id.action_cleanup_all -> {
                // Trigger Devices Scan
                Static.CleanupAllDevices()

                // Don't forget to update DeviceList
                updateCurrentFragmentDeviceList()

                return true;
            }


            else -> super.onOptionsItemSelected(item)
        }
/*
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }*/
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragment: Fragment? = null
        var fragmentID: String = "";
        var bAddToBackstack: Boolean = false;

        // Handle navigation view item clicks here.
        if (item.itemId == R.id.nav_home) {
            // Show main home activity
            fragment = MainActivity.fragmentDeviceList
            fragmentID = "MainActivity.fragmentDeviceList"
//        } else if (item.itemId == R.id.nav_tools) {
            // Show whatever activity
// TODO RELAY FRAME
        } else {
            // Check for dynamic devices
            for ((deviceKey, deviceValue) in Static.gDeviceMap) {
                // check for device item clicked
                if (item.itemId == deviceValue.MenuID) {
                    // Create a new fragment and specify the fragment to show based on nav item clicked
                    if (deviceValue.DeviceType.toString() == DeviceType.AmbiBackController.toString()) {
                        fragment = DeviceFragmentAmbiBack.newInstance()
                        fragment.currentDevice = Static.gDeviceMap[deviceKey]
                        fragmentID = deviceKey
                        bAddToBackstack = true
                    } else {
                        // Use default Fragment - should not happen at all
                        fragment = DeviceFragment.newInstance()
                    }
                    break
                }
            }
        }

        // Set action bar title
        this.title = item.getTitle();
        // Highlight the selected item has been done by NavigationView
        item.setChecked(true)

        // Replace fragment if one was found!
        if (fragment != null) {
            if (bAddToBackstack) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main_frame, fragment, fragmentID)
                    .addToBackStack(fragmentID)
                    .commit();
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main_frame, fragment, fragmentID)
                    .commit();
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult: getMediaProjection with $resultCode")
                //TODO                Toast.makeText(this, "onActivityResult: REQUEST_MEDIA_PROJECTION Success", Toast.LENGTH_SHORT).show()
                MainActivity.mAndroidInitDone = true

                // Create Service Intent for Capture Service
                val captureIntent = Intent(this, CaptureService::class.java)
                captureIntent.setAction(CaptureService.ACTION_ENABLE_CAPTURE)
                captureIntent.putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                captureIntent.putExtras(data)

                // Start Service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(captureIntent);
                } else {
                    this.startService(captureIntent);
                }

                Static.gbMirrorEnabled = true
                Static.SaveConfig()

                // Service is started
                Static.mServiceStarted = true
            } else {
//                MainActivity.mMediaProjection = null
                Toast.makeText(this, "Projection failed", Toast.LENGTH_SHORT).show()
                // TODO disable APP
            }
        }
    }
    override fun onListFragmentInteraction(item: Device?) {
        Log.i(TAG, "onListFragmentInteraction: item " + item!!.DeviceID )
        if (item != null) {
            handleFragmentDeviceConfig(item)
        }
    }

    fun RemoveDeviceFromNaView(removeDevice: Device) {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val devMenu = navView.menu!!.findItem(R.id.nav_devices).subMenu
        try {

            // Check if MENU Entry exists!
            if (removeDevice.MenuID != -1) {
                try {
                    devMenu!!.removeItem(removeDevice.MenuID)
                    Log.i(TAG, "RemoveDeviceFromNaView: item " + removeDevice.DeviceID + " removed from NavView" )
                } catch (ex: Exception) {
                    Log.e(
                        TAG,
                        "RemoveDeviceFromNaView caught Exception while removing ${removeDevice.MenuID}: ${ex.message} - WHAT TODO NOW?",
                        ex
                    )
                }
            } else {
                Log.e(
                    TAG,
                    "RemoveDeviceFromNaView ${removeDevice.DeviceID} does not have a MenuID!"
                )
            }
        } catch (ex: Exception) {
            Log.e(TAG, "RemoveDeviceFromNaView Exception ${ex.message}", ex)
        }

        // Don't forget to update DeviceList
        updateCurrentFragmentDeviceList()
    }

    fun AddDeviceToNaView(newDevice: Device) {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val devMenu = navView.menu!!.findItem(R.id.nav_devices).subMenu
        try {
            // Check if MENU Entry exists!
            if (newDevice.MenuID != -1) {
                try {
                    var existingMenuItem: MenuItem = devMenu!!.getItem(newDevice.MenuID)
                } catch (ex: Exception) {
                    Log.e(
                        TAG,
                        "AddDeviceToNaView caught Exception with ${newDevice.MenuID}: ${ex.message} - reset back to -1!",
                        ex
                    )
                    newDevice.MenuID = -1
                }
            }

            if (newDevice.MenuID != -1) {
                Log.i(TAG, "AddDeviceToNaView: Existing Menuitem for Device " + newDevice.DeviceID + " MenuID(" + newDevice.MenuID + ")")
                var existingMenuItem: MenuItem =
                    devMenu!!.getItem(newDevice.MenuID) //  .add( Menu. NONE, newItemID, Menu.NONE, newDevice.DeviceName )
                val switchMenuItem =
                    existingMenuItem!!.actionView.findViewById(R.id.deviceswitch) as Switch
                // Update properties on menu
                existingMenuItem!!.setTitle(newDevice.DeviceName) //    ="@layout/switch_item"
                existingMenuItem!!.setChecked(newDevice.UseDevice)
                switchMenuItem.setChecked(newDevice.UseDevice)
            } else {
                val newItemID = View.generateViewId()
                Log.i(TAG, "AddDeviceToNaView: Add NEW Menuitem for Device " + newDevice.DeviceID + " MenuID(" + newItemID + ")")
                var newMenuItem: MenuItem =
                    devMenu!!.add(Menu.NONE, newItemID, Menu.NONE, newDevice.DeviceName)

                // Set MenuID for click handling
                newDevice.MenuID = newItemID
                // Set properties
                newMenuItem.setIcon(R.drawable.icon_ambiback_controller)
                newMenuItem.setTitle(newDevice.DeviceName) //    ="@layout/switch_item"
                newMenuItem.setCheckable(true)
                newMenuItem.setChecked(newDevice.UseDevice)
                newMenuItem.setActionView(R.layout.switch_item)

                // Bind events
                newMenuItem.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
                    override fun onMenuItemClick(item: MenuItem): Boolean {
                        Log.i(
                            TAG,
                            "onMenuItemClick: item checked(" + item.isChecked() + "): " + item.title + "-" + newDevice.DeviceID
                        )

                        if (handleFragmentDeviceConfig(newDevice))
                            drawer_layout?.closeDrawers() // Close menu

                        return true
                    }
                })

                val switchMenuItem =
                    newMenuItem.actionView.findViewById(R.id.deviceswitch) as Switch
                switchMenuItem.setOnCheckedChangeListener { buttonView,
                                                            isChecked ->
                    handleSwitchDevice(isChecked, newDevice)
                }
                switchMenuItem.setChecked(newDevice.UseDevice)

                if (Static.gNewFeatures == false || Static.gbMirrorSupportEnabled == false) {
                    switchMenuItem.visibility = View.INVISIBLE;
                }
            }

            // add NavigationItemSelectedListener to check the navigation clicks
            navView.setNavigationItemSelectedListener(this);
        } catch (ex: Exception) {
            Log.e(TAG, "AddDeviceToNaView Exception ${ex.message}", ex)
        }

        // Don't forget to update DeviceList
        updateCurrentFragmentDeviceList()
//        updateFragmentDeviceList()
    }
    fun handleFragmentDeviceConfig(newDevice:Device): Boolean {
        // Open WebPage Fragment
        var fragment: Fragment? = null
        var fragmentID: String = "";

        // Create a new fragment and specify the fragment to show based on nav item clicked
        if (newDevice.DeviceType.toString() == DeviceType.AmbiBackController.toString()) {
            fragment = DeviceFragmentAmbiBack.newInstance()
            fragment.currentDevice = newDevice
            fragmentID = newDevice.DeviceID
        } else {
            // Use default Fragment - should not happen at all
            fragment = DeviceFragment.newInstance()
        }

        // Replace fragment if one was found!
        if (fragment != null) {

            supportFragmentManager.beginTransaction()
                .replace(R.id.content_main_frame, fragment, fragmentID )
//                            .add(R.id.fragmentID, fragment, fragmentID)
                .addToBackStack(fragmentID)
                .commit()

            return true;
        } else
            return false;
    }

    fun updateCurrentFragmentDeviceList() {
        Log.i(TAG, "updateCurrentFragmentDeviceList: ENTER")
        // DonUpdate List inm Fragment
        val myFragmentDeviceList: DevicesListFragment = fragmentDeviceList as DevicesListFragment
        myFragmentDeviceList.UpdateDeviceRecycler()
    }

    fun updateFragmentDeviceList() {
        try {
            // add to main fragment but do not add to backstack!
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.content_main_frame,
                    MainActivity.fragmentDeviceList,
                    "MainActivity.fragmentDeviceList"
                )
                .commitAllowingStateLoss();
        } catch (ex: Exception) {
            Log.e(TAG, "updateFragmentDeviceList Exception ${ex.message}", ex)
        }
    }

    fun handleSwitchAmbiBack(isChecked: Boolean){
        Log.i(TAG, "handleSwitchAmbiBack: ENTER")

        if (Static.gNewFeatures == false) {
            Log.i(TAG, "handleSwitchAmbiBack: NEW FEATURES DISABLED!")
            return
        }

        if (MainActivity.mAndroidInitDone == false) {
            // Start Init Code
            mMediaProjectionManager = getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
            Toast.makeText(this, "Request MediaProjection Access", Toast.LENGTH_SHORT).show()
        } else {
            // Get CaptureIntent Service
            val captureIntent = Intent(this, CaptureService::class.java)

            // Start Service if enabled
            if (isChecked) {
                // Start or stop your Service
                captureIntent.setAction(CaptureService.ACTION_DISABLE_CAPTURE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(captureIntent);
                } else {
                    this.startService(captureIntent);
                }
                Static.mServiceStarted = true
                Static.gbMirrorEnabled = true
                Static.SaveConfig()

                // clickButtonTest(this)
                UpdateImageReal()
            } else {
                // The switch is disabled
                // Stop Capture Intent
                captureIntent.setAction(CaptureService.ACTION_ENABLE_CAPTURE)
                this.stopService(captureIntent);
                Static.mServiceStarted = false
                Static.gbMirrorEnabled = false
                Static.SaveConfig()
            }
        }
    }

    fun handleSwitchDevice(isChecked: Boolean, myDevice: Device) {
        Log.i(TAG, "switchMenuItem: feature checked(" + isChecked + "): " + myDevice.DeviceID )
        if (myDevice is DeviceAmbiBack) {
            var deviceAmbiBack: DeviceAmbiBack = myDevice
            if (deviceAmbiBack.AmbiBackDeviceType.internaltype == "CAM") {
                GlobalScope.launch {
                    var szUrl = "http://" + deviceAmbiBack.IPAddress + "/setcolormode?mode=" + if(isChecked) "3" else "0"
                    try {
                        // Get JSON String from Controller
                        var url: URL = URL(szUrl)
                        var szResultsFromController = url.readText()
                        Log.i(TAG, deviceAmbiBack.DeviceID + ": switchMenuItem: (" + szUrl + ") RESULT " + szResultsFromController)
                    }
                    catch (ex: Exception) {
                        Log.e(TAG, deviceAmbiBack.DeviceID + ": switchMenuItem: (" + szUrl + ") FAILED " + ex.message )
                    }
                }
            } else if (deviceAmbiBack.AmbiBackDeviceType.internaltype == "CONTROLLER") {
                Log.i(TAG, "switchMenuItem: CONTROLLER item checked(" + isChecked + "): " + myDevice.DeviceID )
                myDevice.UseDevice = isChecked
                myDevice.SaveConfig()
            } else if (deviceAmbiBack.AmbiBackDeviceType.internaltype == "BRIDGE") {
                GlobalScope.launch {
                    var szUrl = "http://" + deviceAmbiBack.IPAddress + "/sethuesupport?value=" + if(isChecked) "1" else "0"
                    try {
                        // Get JSON String from Controller
                        var url: URL = URL(szUrl)
                        var szResultsFromController = url.readText()
                        Log.i(TAG, deviceAmbiBack.DeviceID + ": switchMenuItem: (" + szUrl + ") RESULT " + szResultsFromController)
                    }
                    catch (ex: Exception) {
                        Log.e(TAG, deviceAmbiBack.DeviceID + ": switchMenuItem: (" + szUrl + ") FAILED " + ex.message )
                    }
                }
            } else {
                Log.e(TAG, deviceAmbiBack.DeviceID + ": switchMenuItem: UNKNOWN DEVICE" )
            }
        }
    }

    fun UpdateImageReal() {
        if (Capture.mBitmap != null) {
            MainActivity.fragmentDeviceList?.item_mirrorButton.setImageBitmap(Capture.mBitmap)
            // imageViewPreview.setImageBitmap(Capture.mBitmap)
        }
    }

    /* Workarround to allow network code on main thread temporary */
    fun enableStrictMode() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }


}
