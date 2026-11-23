package com.ambiback.app.ui.device

//import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
//import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ambiback.app.Device
import com.ambiback.app.R

class DeviceFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceFragment()
    }

    private lateinit var viewModel: DeviceViewModel
    var currentDevice: Device? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.device_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DeviceViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
