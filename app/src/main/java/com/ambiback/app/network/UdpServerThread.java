package com.ambiback.app.network;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.util.Log;
import com.ambiback.app.Device;
import com.ambiback.app.DeviceAmbiBack;
import com.ambiback.app.DeviceType;
import com.ambiback.app.MainActivity;
import com.ambiback.app.globals.Static;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.Enumeration;

import kotlinx.coroutines.GlobalScope;

public class UdpServerThread extends Thread{
    static public String AMBIBACK_BROADCAST_DISCOVER = "HELLO AMBIBACK DEVICES";

    private String TAG = "UdpServerThread";
    int serverPort;
    boolean running;
    DatagramSocket socket = null;
    InetAddress localIPAddress = null;
    InetAddress inetBroadcast = null;

    public UdpServerThread(int serverPort) {
        super();
        this.serverPort = serverPort;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    @Override
    public void run() {

        running = true;

        WifiManager wifiMgr = (WifiManager)  Static.gMainContext.getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock mMulticastLock = wifiMgr.createMulticastLock("ambiback-udp");

        try {
//            socket = new DatagramSocket(serverPort);
            // Keep a socket open to listen to all the UDP trafic that is destined for this port
            localIPAddress = this.getLocalIpAddress();
            inetBroadcast = this.getBroadcastAddress(wifiMgr);

            // Needed to receive Multicasts
            mMulticastLock.acquire();

            // Create local socket!
            socket = new DatagramSocket(serverPort, InetAddress.getByName("0.0.0.0")); //inetBroadcast); // , InetAddress.getByName("192.168.0.255"));
            socket.setBroadcast(true);
            Log.i(TAG, "UDP Server is running on " + socket.getLocalAddress() + " / " + socket.getInetAddress() + " / " + localIPAddress + " port " + socket.getBroadcast());

            // Receive Loop here
            while(running){
                byte[] recvBuf = new byte[4096];

                // receive request
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);//this code block the program flow

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                Log.i(TAG, "Received Broadcast from: " + address + ":" + port + "\n");

                if (address.toString().equals(localIPAddress.toString())) {
                    Log.i(TAG, "Own Broadcast, discard..." );
                } else {
                    String szBroadcast = new String(recvBuf,0, packet.getLength());
                    Log.v(TAG, "Data: " + szBroadcast);

                    // TODO: Process received data!
                    try {
                        JSONObject jsonObj = new JSONObject(szBroadcast);
                        // Get Data from JSON string
                        String szDeviceType = jsonObj.getString("DeviceType").trim();
                        String szDeviceVersion = jsonObj.getString("DeviceVersion").trim();
                        String szInternalDeviceID = jsonObj.getString("DeviceID").trim();
                        String szInternalNetworkMAC = jsonObj.getString("DeviceMAC").trim();
                        String szDeviceName = jsonObj.getString("DeviceName").trim();
                        String szDeviceIP = jsonObj.getString("DeviceIP").trim();
                        int iLastLedMode = jsonObj.getInt("LedStatus");

                        // Create new Device obj and fill/init
                        DeviceAmbiBack newDevice = new DeviceAmbiBack();
                        newDevice.setInternalDeviceID(szInternalDeviceID);
                        newDevice.setInternalNetworkMAC(szInternalNetworkMAC);

                        // "AmbiBack_Controller"
                        newDevice.setDeviceType (DeviceType.AmbiBackController);
                        newDevice.setAmbiBackDeviceType ( newDevice.getAmbiBackDeviceTypes(szDeviceType) );
                        newDevice.setLastLedMode ( iLastLedMode );
                        newDevice.setDeviceName (szDeviceName);
                        newDevice.setDeviceVersion(szDeviceVersion);
                        newDevice.setIPAddress(szDeviceIP);

                        // Set Device ID and try to load configuration
                        newDevice.setDeviceID();

                        // Set Device Online
                        newDevice.setIsOnline(true);

                        // Load local config first
                        newDevice.LoadConfig();

                        // Make sure to overwrite certain properties from CACHE!
                        newDevice.setDeviceName (szDeviceName);
                        newDevice.setDeviceVersion(szDeviceVersion);
                        newDevice.setIPAddress(szDeviceIP);

                        // Add device in all cases, load settings in background
                        Static.AddDevice(newDevice, true);

                        // Try loading config from controller
                        newDevice.LoadConfigFromController();
                    } catch (Exception e) {
                        Log.e(TAG, "run json parse failed: " + e.getMessage());
                    }
                }
            }

            Log.i(TAG, "UDP Server ended");

        } catch (SocketException e) {
            Log.e(TAG, "run SocketException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "run IOException: " + e.getMessage());
        } finally {
            if(socket != null){
                socket.close();
                Log.i(TAG, "socket.close()");
            }

            mMulticastLock.release();
        }
    }

    public void sendBroadcast(String messageStr) {
        if (inetBroadcast == null)
            return;
        try {
            //Open a random port to send the package
            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.setBroadcast(true);
            sendSocket.setReuseAddress(true);

            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetBroadcast, serverPort); // InetAddress.getByName("255.255.255.255")
            sendSocket.send(sendPacket);
            Log.i(TAG, "Broadcast packet sent to: " + inetBroadcast.getHostAddress());
        } catch (IOException e) {
            Log.e(TAG, "sendBroadcast Exception: " + e.getMessage());
        }
    }


    private InetAddress getBroadcastAddress(WifiManager wifi) throws IOException {
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }

    private InetAddress getLocalIpAddress() {
        InetAddress ip = null;
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress;
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "getLocalIpAddress Exception: " + e.getMessage());
        }
        return ip;
    }
}