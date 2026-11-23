package com.ambiback.app.ui.device

// import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
//import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_NORMAL
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ambiback.app.Device
import com.ambiback.app.R
import kotlinx.android.synthetic.main.device_fragment_ambiback.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class DeviceFragmentAmbiBack : Fragment() {

    // Static Properties
    companion object {
        private val TAG =  DeviceFragmentAmbiBack::class.qualifiedName
        fun newInstance() = DeviceFragmentAmbiBack()
    }

    private lateinit var viewModel: DeviceViewModel
    private var mRecyclerView: RecyclerView ?= null
    private var mSwipeLayout: SwipeRefreshLayout? = null
    var currentDevice: Device? = null
    var reloadTry: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "$this: onCreate()")
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        var view = inflater.inflate(R.layout.device_fragment_ambiback, container, false)
        if (view is RecyclerView) {
            mRecyclerView = view;
        } else {
            mRecyclerView = view.findViewById(R.id.devicelistrecycleview);
            // mSwipeLayout = view.findViewById(R.id.refreshViewAmbiback);
        }

        /* DISABLED FOR NOW
        mSwipeLayout!!.setOnRefreshListener {
            // Try reload
            webview_ambiback?.loadUrl ("http://" + currentDevice?.IPAddress)
            refreshViewAmbiback.isRefreshing = false
        }
        */

        return view; // inflater.inflate(R.layout.device_fragment_ambiback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // val myWebView: WebView = view.findViewById(R.id.your_webView)

        webview_ambiback.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            Log.i(TAG, "setOnKeyListener: OnKeyListener ${keyCode}")
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action === MotionEvent.ACTION_UP && webview_ambiback.canGoBack()
            ) {
                Log.i(TAG, "setOnKeyListener: OnKeyListener TRUE ")
                webview_ambiback.goBack()
                return@OnKeyListener true
            }
            Log.i(TAG, "setOnKeyListener: OnKeyListener FALSE ")
            false
        }) // until here

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DeviceViewModel::class.java)
        // TODO: Use the ViewModel

        // Toast.makeText(Static.gMainActivity, "DeviceFragmentAmbiBack opened for  ${currentDevice?.DeviceName}", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "onActivityCreated: opened for ${currentDevice?.DeviceName} ")

        webview_ambiback.settings.javaScriptEnabled = true
        webview_ambiback.settings.allowContentAccess = true
        webview_ambiback.settings.allowFileAccess = true
        webview_ambiback.settings.allowUniversalAccessFromFileURLs = true
        webview_ambiback.settings.cacheMode = LOAD_NORMAL; //LOAD_DEFAULT

        webview_ambiback.webViewClient = object : WebViewClient () {
            override fun onLoadResource (view: WebView?, url: String?) {
                Log.v(TAG, "webview.onLoadResource: URL LOAD ${url}")
            }
            override fun onReceivedError (view: WebView?, request: WebResourceRequest?, error: WebResourceError) {
                Log.e(TAG, "webview.onReceivedError: URL FAIL ${request?.url} with error ${error.errorCode} + ${error.description}  try ${reloadTry} ")
                if ( (request?.url.toString().contains("!#") || request?.url.toString().endsWith("/")) && error.errorCode == -6 /*ERR_CONNECTION_REFUSED*/ && reloadTry.compareTo(5) < 0) {
                    Thread.sleep(1000)
                    view?.loadUrl (request?.url.toString())
                    reloadTry.inc();
                }
            }
            override fun onPageFinished (view: WebView?, url: String?) {
                Log.i(TAG, "webview.onPageFinished: URL LOAD ${url}")
                reloadTry = 0
            }
        }
        // Initial try
        webview_ambiback?.loadUrl ("http://" + currentDevice?.IPAddress)
    }




}
