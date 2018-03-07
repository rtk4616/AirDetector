/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.microjet.airqi2.BlueTooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.bluetooth.le.ScanSettings.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.microjet.airqi2.R
import java.util.*

//選單按下去的後跳出的視窗及連線資料
class DeviceListActivity : Activity() {
    private var targetUUID = ParcelUuid(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))

    private var listBT: ListView? = null
    private var cancelButton: Button? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null

    private var mScanning: Boolean = false

    //private var listBluetoothDevice : MutableList<BluetoothDevice>
    private var mLeDeviceListAdapter : LeDeviceListAdapter? = null
    //ListAdapter mLeDeviceListAdapter;

    private var mHandler: Handler? = null

    private var scanProgress: ProgressBar? = null

    private var devScanningPanel: LinearLayout? = null

    // ListView 項目點選監聽器
    internal var scanResultOnItemClickListener: AdapterView.OnItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        val device = parent.getItemAtPosition(position) as BluetoothDevice

        val share = getSharedPreferences("MACADDRESS", MODE_PRIVATE)
        share.edit().clear().putString("mac", device.address).apply()
        share.edit().putString("name", device.name).apply()

        scanLeDevice(false)

//            val intent: Intent? = Intent(BroadcastIntents.PRIMARY)
//            intent!!.putExtra("status", BroadcastActions.ACTION_CONNECT_DEVICE)
//            //Use Bundle Save Address
//            val bundle: Bundle? = Bundle()
//            bundle!!.putString("mac", device.address.toString())
//            //put bundle into intent
//            intent!!.putExtras(bundle)
//            //intent!!.putExtra("mac", device.address)
//            sendBroadcast(intent)
        //
        /*
        val serviceIntent :Intent? = Intent(this, UartService::class.java)
        serviceIntent?.putExtra(BluetoothDevice.EXTRA_DEVICE, device.address)
        startService(serviceIntent)
        */
        //this@DeviceListActivity.finish()

        val backIntent = Intent()
        val backBundle = Bundle()
        backBundle.putString("MAC",device.address)
        backIntent.putExtras(backBundle)
        setResult(RESULT_OK,backIntent)
        finish()
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            addBluetoothDevice(result.device, result.rssi)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                addBluetoothDevice(result.device, result.rssi)
            }
        }

        /*override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }*/

        private fun addBluetoothDevice(device: BluetoothDevice, rssi: Int) {
            if (device.bondState == 10) {
                mLeDeviceListAdapter!!.addDevice(device, rssi)
                mLeDeviceListAdapter!!.notifyDataSetChanged()
            }
            //Log.v(TAG, "Found Device, Name: " + device.name + " RSSI: " + rssi)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.device_list)

        scanProgress = findViewById(R.id.scanProgress)
        scanProgress!!.bringToFront()

        devScanningPanel = findViewById(R.id.devScanningPanel)
        devScanningPanel!!.visibility = View.VISIBLE
        devScanningPanel!!.bringToFront()

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        //populateList()

        getBluetoothAdapterAndLeScanner()

        cancelButton = findViewById<View>(R.id.btn_cancel) as Button

        cancelButton!!.setOnClickListener {
            if (mScanning == false) {
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
            } else {
                finish()
            }
        }

        listBT = findViewById(R.id.new_devices)

        //listBluetoothDevice = ArrayList()
        mLeDeviceListAdapter = LeDeviceListAdapter()

        listBT!!.adapter = mLeDeviceListAdapter
        listBT!!.onItemClickListener = scanResultOnItemClickListener

        mHandler = Handler()
        registerReceiver(mBluetoothStateReceiver, makeBluetoothStateIntentFilter())
    }

    override fun onResume() {
        super.onResume()

        if (!mBluetoothAdapter!!.isEnabled) {
            //val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH)
            val intentOpenBluetoothSettings = Intent()
            intentOpenBluetoothSettings.action = android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            startActivity(intentOpenBluetoothSettings)
        } else {
            scanLeDevice(true)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBluetoothStateReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }

        getBluetoothAdapterAndLeScanner()

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner

        mScanning = false
    }

    /*
    to call startScan (ScanCallback callback),
    Requires BLUETOOTH_ADMIN permission.
    Must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get results.
     */
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            //listBluetoothDevice.clear()
            mLeDeviceListAdapter!!.notifyDataSetChanged()
            listBT!!.invalidateViews()

            cancelButton!!.text = resources.getText(android.R.string.cancel)

            devScanningPanel!!.visibility = View.VISIBLE
            devScanningPanel!!.bringToFront()
            scanProgress!!.visibility = View.VISIBLE
            listBT!!.isEnabled = false

            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mBluetoothLeScanner!!.stopScan(scanCallback)
                listBT!!.invalidateViews()

                cancelButton!!.text = resources.getText(R.string.scan)

                scanProgress!!.visibility = View.GONE

                devScanningPanel!!.visibility = View.INVISIBLE

                mScanning = false
                listBT!!.isEnabled = true


            }, SCAN_PERIOD)

            //mBluetoothLeScanner.startScan(scanCallback);

            //scan specified devices only with ScanFilter
            val scanFilter = ScanFilter.Builder()
                    .setServiceUuid(targetUUID).build()
            val scanFilters = ArrayList<ScanFilter>()
            scanFilters.add(scanFilter)

            val scanSettings = ScanSettings.Builder().setScanMode(SCAN_MODE_BALANCED).build()


            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, scanCallback)

            mScanning = true
        } else {
            mBluetoothLeScanner!!.stopScan(scanCallback)
            mScanning = false
            listBT!!.isEnabled = true
        }
    }

    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices : ArrayList<BluetoothDevice>
        private val mLeDevicesRssi : ArrayList<Int>
        private val mInflator : LayoutInflater

        init {
            mLeDevices = ArrayList()
            mLeDevicesRssi = ArrayList()
            mInflator = this@DeviceListActivity.layoutInflater
        }

        fun addDevice(device: BluetoothDevice, rssi: Int) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
                mLeDevicesRssi.add(rssi)
            }
        }

        /*BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }*/

        internal fun clear() {
            mLeDevices.clear()
            mLeDevicesRssi.clear()
            mLeDevicesRssi.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices.get(i)
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        @SuppressLint("SetTextI18n", "InflateParams")
        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View? {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.device_element, null)
                viewHolder = ViewHolder()
                viewHolder.deviceName = view.findViewById(R.id.name)
                viewHolder.deviceRssi = view.findViewById(R.id.rssi)
                viewHolder.deviceAddress = view.findViewById(R.id.address)
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            val device = mLeDevices[i]
            val rssi = mLeDevicesRssi[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.isNotEmpty()) {
                if(deviceName.contains("ADDWII_ASM_1124L")) {
                    viewHolder.deviceName!!.text = deviceName.substring(0, 16)
                } else {
                    viewHolder.deviceName!!.text = deviceName
                }
            } else {
                viewHolder.deviceName!!.text = "Null"
            }

            viewHolder.deviceRssi!!.text = "RSSI: " + rssi.toString()

            viewHolder.deviceAddress!!.text = device.address

            return view
        }
    }

    private class ViewHolder {
        internal var deviceName: TextView? = null
        internal var deviceRssi: TextView? = null
        internal var deviceAddress: TextView? = null
    }

    companion object {
        private val TAG = DeviceListActivity::class.java.simpleName

        private val RQS_ENABLE_BLUETOOTH = 1
        private val SCAN_PERIOD: Long = 5000
    }

    private fun makeBluetoothStateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        return intentFilter
    }

    private val mBluetoothStateReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            var stateStr = "BluetoothAdapter.STATE_OFF"

            when (state) {
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    stateStr = "BluetoothAdapter.STATE_TURNING_OFF"
                }
                BluetoothAdapter.STATE_OFF -> {
                    stateStr = "BluetoothAdapter.STATE_OFF"
                    //當手機藍牙關掉的時候將裝置清單關閉免得Crash
                    //因為有個handler會延後執行，需要移除裡面的內容才不會Crash
                    mHandler?.removeCallbacksAndMessages(null)
                    //兩種方法都可以收掉目前的DeviceList
                    finish()
                    onBackPressed()
                }
                BluetoothAdapter.STATE_ON -> {
                    stateStr = "BluetoothAdapter.STATE_ON"
                }
            }
            Log.v(TAG, "mBluetoothStateReceiver: " + stateStr)
        }
    }

}
