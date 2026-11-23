package com.ambiback.app.ui

/*
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
 */

// import com.ambiback.app.dummy.DummyContent
// import com.ambiback.app.dummy.DummyContent.DummyItem
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ambiback.app.Device
import com.ambiback.app.R
import com.ambiback.app.globals.Static
import kotlinx.android.synthetic.main.fragment_device_item_list.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DevicesListFragment.OnListFragmentInteractionListener] interface.
 */
class DevicesListFragment : Fragment() {
    // Static Properties
    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            DevicesListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }

        private val TAG = DevicesListFragment::class.qualifiedName
    }

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null

    private var mRecyclerView: RecyclerView ?= null
    private var mSwipeLayout: SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "ENTER onCreate" )

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }

        refreshView?.setOnRefreshListener {
            this.UpdateDeviceRecycler()
            refreshView.isRefreshing = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_device_item_list, container, false)
        if (view is RecyclerView) {
            mRecyclerView = view;
        } else {
            mRecyclerView = view.findViewById(R.id.devicelistrecycleview);
            mSwipeLayout = view.findViewById(R.id.refreshView);
        }
        Log.i(TAG, "ENTER onCreateView" )

        // Set the adapter
        if (mRecyclerView is RecyclerView ) {
            Log.i(TAG, "onCreateView is RecyclerView" )
            mRecyclerView!!.layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
            }
            this.UpdateDeviceRecycler()
        } else {
            Log.i(TAG, "onCreateView is NOT RecyclerView" )
        }

        mSwipeLayout!!.setOnRefreshListener {
            // Trigger Devices Scan
            Static.DiscoverDevices()
            this.UpdateDeviceRecycler()
            refreshView.isRefreshing = false
        }

        val switchAmbiBack = view.findViewById(R.id.item_switchMirror) as Switch
        // Hide if gNewFeatures is false!
        if (Static.gNewFeatures == false || Static.gbMirrorSupportEnabled == false) {
            switchAmbiBack?.visibility = View.GONE;
        } else {
            // Bind events
            switchAmbiBack?.setOnCheckedChangeListener {
                    buttonView,
                    isChecked -> Static.gMainActivity?.handleSwitchAmbiBack(isChecked)
            }
        }

        // Toggle Switch on if enabled
        if (Static.gbMirrorEnabled == true ) {
            switchAmbiBack?.setChecked(Static.gbMirrorEnabled )
        }

        // Hide if gNewFeatures is false!
        if (Static.gNewFeatures == false || Static.gbMirrorSupportEnabled == false) {
            val ambiBackMirrorLayout = view.findViewById(R.id.fragment_ambiback_mirror) as LinearLayout
            ambiBackMirrorLayout?.visibility = View.GONE
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    public fun UpdateDeviceRecycler() {
        try {
            mRecyclerView?.adapter = DeviceRecyclerViewAdapter(Static.gDeviceList, listener)
        }
        catch (ex: Exception) {
            Log.e(TAG, "UpdateDeviceRecycler: failed with exception " + ex.message )
        }
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: Device?)
    }

}
