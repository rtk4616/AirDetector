package com.microjet.airqi2

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.microjet.airqi2.Definition.BroadcastActions
import com.microjet.airqi2.Definition.BroadcastIntents
import com.microjet.airqi2.Definition.SavePreferences
import kotlinx.android.synthetic.main.activity_setting.*
import com.jaygoo.widget.RangeSeekBar
import com.microjet.airqi2.BlueTooth.DFU.DFUProcessClass
import com.microjet.airqi2.Definition.Colors
import com.microjet.airqi2.GestureLock.DefaultPatternCheckingActivity
import com.microjet.airqi2.GestureLock.DefaultPatternSettingActivity
import com.microjet.airqi2.URL.AirActionTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.DecimalFormat


/**
 * Created by B00174 on 2017/11/29.
 *
 */
class SettingActivity : AppCompatActivity() {

    private var mPreference: SharedPreferences? = null

    private var swAllowNotifyVal: Boolean = false
    private var swMessageVal: Boolean = false
    private var swViberateVal: Boolean = false
    private var swSoundVal: Boolean = false
    //20180130
    private var batSoundVal: Boolean = false
    private var swLedPowerVal: Boolean = true
    //20180227
    private var swCloudVal: Boolean = true

    private var tvocSeekBarVal: Int = 660
    private var pm25SeekBarVal: Int = 16

    private var isPrivacy: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        readPreferences()   // 載入設定值
        uiSetListener()
        initActionBar()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        text_local_uuid.text = MyApplication.getPsuedoUniqueID()
        text_device_ver.text = resources.getString(R.string.text_label_device_version) + MyApplication.getDeviceVersion()

        getPrivacySettings()

        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    private fun readPreferences() {
        mPreference = getSharedPreferences(SavePreferences.SETTING_KEY, 0)

        getNotificationSettings()
        getCloudSettings()
        getPrivacySettings()
        getDeviceLedSettings()
    }

    private fun uiSetListener() {

        swAllowNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cgMessage.visibility = View.VISIBLE
                cgVibration.visibility = View.VISIBLE
                cgSound.visibility = View.VISIBLE
                cgSeekbar.visibility = View.VISIBLE
                cgLowBatt.visibility = View.VISIBLE
            } else {
                cgMessage.visibility = View.GONE
                cgVibration.visibility = View.GONE
                cgSound.visibility = View.GONE
                cgSeekbar.visibility = View.GONE
                cgLowBatt.visibility = View.GONE
            }

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_NOTIFY,
                    isChecked).apply()
        }

        swMessage.setOnCheckedChangeListener { _, isChecked ->

            setSwitchLabelValue(text_msg_stat, isChecked)

            if (isChecked) {
                val mainintent = Intent(BroadcastIntents.PRIMARY)
                mainintent.putExtra("status", "message")
                sendBroadcast(mainintent)

                Log.d("message", "messageSETTING")
            }

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_MESSAGE,
                    isChecked).apply()
        }

        swVibrate.setOnCheckedChangeListener { _, isChecked ->

            setSwitchLabelValue(text_vibrate_stat, isChecked)

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_VIBERATION,
                    isChecked).apply()
        }

        swSound.setOnCheckedChangeListener { _, isChecked ->

            setSwitchLabelValue(text_sound_stat, isChecked)

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_SOUND,
                    isChecked).apply()
        }

        tvocSeekBar.setOnRangeChangedListener(object : RangeSeekBar.OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar, min: Float, max: Float, isFromUser: Boolean) {
                setSeekBarColor(view, min, true)

                setSeekBarValue(tvocSeekValue, min)

                mPreference!!.edit().putInt(SavePreferences.SETTING_TVOC_NOTIFY_VALUE, min.toInt()).apply()
                Log.e("SeekBar", "Min: $min")
            }

            override fun onStartTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {
                //do what you want!!
            }

            override fun onStopTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {
                //do what you want!!
            }
        })

        pm25SeekBar.setOnRangeChangedListener(object : RangeSeekBar.OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar, min: Float, max: Float, isFromUser: Boolean) {
                setSeekBarColor(view, min, false)

                setSeekBarValue(pm25SeekValue, min)

                mPreference!!.edit().putInt(SavePreferences.SETTING_PM25_NOTIFY_VALUE, min.toInt()).apply()
            }

            override fun onStartTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {
                //do what you want!!
            }

            override fun onStopTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {
                //do what you want!!
            }
        })


        //20180206
        batSound.setOnCheckedChangeListener { _, isChecked ->

            setSwitchLabelValue(text_bat_stat, isChecked)

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_BATTERY_SOUND,
                    isChecked).apply()
        }

        ledPower.setOnCheckedChangeListener { _, isChecked ->

            setSwitchLabelValue(text_led_stat, isChecked)

            val intent: Intent? = Intent(
                    if (isChecked) {
                        BroadcastActions.INTENT_KEY_LED_ON
                    } else {
                        BroadcastActions.INTENT_KEY_LED_OFF
                    }
            )

            sendBroadcast(intent)

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_LED_SWITCH,
                    isChecked).apply()
        }

        //20180227  CloudFun
        swCloudFunc.setOnCheckedChangeListener { _, isChecked ->

            setSwitchLabelValue(text_cloud_stat, isChecked)

            val intent: Intent? = Intent(BroadcastIntents.PRIMARY)

            if (isChecked) {
                intent!!.putExtra("status", BroadcastActions.INTENT_KEY_CLOUD_ON)
            } else {
                intent!!.putExtra("status", BroadcastActions.INTENT_KEY_CLOUD_OFF)
            }

            sendBroadcast(intent)

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_CLOUD_FUN,
                    isChecked).apply()

        }

        swAllowPrivacy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                DefaultPatternSettingActivity.startAction(this@SettingActivity)
            } else {
                DefaultPatternCheckingActivity.startAction(this@SettingActivity,
                        DefaultPatternCheckingActivity.START_ACTION_MODE_DISABLE)
            }
        }

        btnChangePassword.setOnClickListener {
            DefaultPatternCheckingActivity.startAction(this@SettingActivity,
                    DefaultPatternCheckingActivity.START_ACTION_MODE_CHANGE_PASSWOPRD)
        }

        btnCheckFW.setOnClickListener {
            if(MyApplication.getDeviceChargeStatus()) {
                val fwVer = MyApplication.getDeviceVersion()
                val fwSerial = MyApplication.getDeviceSerial()
                val fwType = MyApplication.getDeviceType()
                checkFwVersion("20$fwVer$fwSerial", "00$fwType")
            } else {
                showNotChargingDialog()
            }
        }

    }

    private fun setSeekBarColor(view: RangeSeekBar, min: Float, isTVOC: Boolean) {
        if (isTVOC) {
            view.setLineColor(R.color.colorSeekBarDefault, when (min) {
                in 0..219 -> Colors.tvocCO2Colors[0]
                in 220..659 -> Colors.tvocCO2Colors[1]
                in 660..2199 -> Colors.tvocCO2Colors[2]
                in 2200..5499 -> Colors.tvocCO2Colors[3]
                in 5500..20000 -> Colors.tvocCO2Colors[4]
                else -> Colors.tvocCO2Colors[5]
            })
        } else {
            view.setLineColor(R.color.colorSeekBarDefault, when (min) {
                in 0..15 -> Colors.tvocCO2Colors[0]
                in 16..34 -> Colors.tvocCO2Colors[1]
                in 35..54 -> Colors.tvocCO2Colors[2]
                in 55..150 -> Colors.tvocCO2Colors[3]
                in 151..250 -> Colors.tvocCO2Colors[4]
                else -> Colors.tvocCO2Colors[5]
            })
        }
    }

    private fun setSeekBarValue(view: TextView, min: Float) {
        val format = DecimalFormat("###")
        view.text = format.format(min).toString()
    }

    private fun setSwitchLabelValue(view: TextView, value: Boolean) {
        if (value) {
            view.text = getString(R.string.text_setting_on)
        } else {
            view.text = getString(R.string.text_setting_off)
        }
    }

    private fun getNotificationSettings() {
        swAllowNotifyVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_NOTIFY, false)
        swMessageVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_MESSAGE, false)
        swViberateVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_VIBERATION, false)
        swSoundVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_SOUND, false)
        batSoundVal = mPreference!!.getBoolean(SavePreferences.SETTING_BATTERY_SOUND, false)

        tvocSeekBarVal = mPreference!!.getInt(SavePreferences.SETTING_TVOC_NOTIFY_VALUE, 660)
        pm25SeekBarVal = mPreference!!.getInt(SavePreferences.SETTING_PM25_NOTIFY_VALUE, 16)

        swAllowNotify.isChecked = swAllowNotifyVal

        if (swAllowNotifyVal) {
            cgMessage.visibility = View.VISIBLE
            cgVibration.visibility = View.VISIBLE
            cgSound.visibility = View.VISIBLE
            cgSeekbar.visibility = View.VISIBLE
            cgLowBatt.visibility = View.VISIBLE
        } else {
            cgMessage.visibility = View.GONE
            cgVibration.visibility = View.GONE
            cgSound.visibility = View.GONE
            cgSeekbar.visibility = View.GONE
            cgLowBatt.visibility = View.GONE
        }

        swMessage.isChecked = swMessageVal
        swVibrate.isChecked = swViberateVal
        swSound.isChecked = swSoundVal

        batSound.isChecked = batSoundVal

        setSwitchLabelValue(text_msg_stat, swMessageVal)
        setSwitchLabelValue(text_vibrate_stat, swViberateVal)
        setSwitchLabelValue(text_sound_stat, swSoundVal)
        setSwitchLabelValue(text_bat_stat, batSoundVal)

        tvocSeekBar.setValue(tvocSeekBarVal.toFloat())
        pm25SeekBar.setValue(pm25SeekBarVal.toFloat())

        tvocSeekValue.text = tvocSeekBarVal.toString()
        pm25SeekValue.text = pm25SeekBarVal.toString()

        setSeekBarColor(tvocSeekBar, tvocSeekBarVal.toFloat(), true)
        setSeekBarColor(pm25SeekBar, pm25SeekBarVal.toFloat(), false)
    }

    private fun getPrivacySettings() {
        isPrivacy = mPreference!!.getBoolean(SavePreferences.SETTING_MAP_PRIVACY, false)

        swAllowPrivacy.isChecked = isPrivacy

        if (isPrivacy) {
            btnChangePassword.visibility = View.VISIBLE
        } else {
            btnChangePassword.visibility = View.GONE
        }
    }

    private fun getCloudSettings() {
        isPrivacy = mPreference!!.getBoolean(SavePreferences.SETTING_MAP_PRIVACY, false)

        swCloudVal = mPreference!!.getBoolean(SavePreferences.SETTING_CLOUD_FUN, true)
        swCloudFunc.isChecked = swCloudVal

        setSwitchLabelValue(text_cloud_stat, swCloudVal)
    }

    private fun getDeviceLedSettings() {
        swLedPowerVal = mPreference!!.getBoolean(SavePreferences.SETTING_LED_SWITCH, true)

        ledPower.isChecked = swLedPowerVal

        setSwitchLabelValue(text_led_stat, swLedPowerVal)
    }

    private fun initActionBar() {
        // 取得 actionBar
        val actionBar = supportActionBar
        // 設定顯示左上角的按鈕
        actionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home //對用戶按home icon的處理，本例只需關閉activity，就可返回上一activity，即主activity。
            -> {
                finish()
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe
    fun onEvent(bleEvent: BleEvent) {
        /* 處理事件 */
        Log.d("AirAction", bleEvent.message)
        when (bleEvent.message) {
            "version latest" -> {
                showFwLatestDialog()
            }
            "New FW Arrival " -> {
                showDownloadDialog(bleEvent.message!!)
            }
            "Download Success" -> {
                /*
                val intent = Intent()
                intent.putExtra("ADDRESS",show_Dev_address?.text.toString())
                intent.putExtra("DEVICE_NAME",show_Device_Name?.text.toString())
                intent.setClass(this, DFUActivity::class.java)
                startActivity(intent)*/
                val dfup = DFUProcessClass(this)
                val share = getSharedPreferences("MACADDRESS", Context.MODE_PRIVATE)
                val mDeviceAddress = share.getString("mac", "noValue")
                if (mDeviceAddress != "noValue") {
                    dfup.DFUAction("", mDeviceAddress)
                }
            }
            "dfu complete" -> {
                showDfuCompleteDialog()
            }
        }
    }

    private fun checkFwVersion(Version: String, DeviceType: String) {
        //if (batValue > 100) {
        val aat = AirActionTask(this@SettingActivity, Version, DeviceType)
        val myResponse = aat.execute("postFWVersion")
        Log.v("AirActionTask", "OVER")
        //}
    }

    private fun showDownloadDialog(msg: String) {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        //必須是android.app.AlertDialog.Builder 否則alertDialog.show()會報錯
        //Dialog.setTitle("提示")
        Dialog.setTitle(getString(R.string.remind))
        Dialog.setMessage("$msg\t請確定裝置與電源連接正常，手機儘量接近裝置，以利FW更新。")
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        //Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "确定")

        Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.Reject))//否
        { dialog, _ ->
            dialog.dismiss()
        }
        Dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Accept))//是
        { dialog, _ ->

            dialog.dismiss()

            val fwVer = ""

            val aat = AirActionTask(this@SettingActivity)
            aat.execute("downloadFWFile")
        }
        Dialog.show()
    }

    private fun showFwLatestDialog() {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        //必須是android.app.AlertDialog.Builder 否則alertDialog.show()會報錯
        //Dialog.setTitle("提示")
        Dialog.setTitle(getString(R.string.remind))
        Dialog.setMessage("您的Mobile Nose已是最新版本。")
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        Dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Accept))//是
        { dialog, _ ->

            dialog.dismiss()
        }
        Dialog.show()
    }

    private fun showNotChargingDialog() {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        //必須是android.app.AlertDialog.Builder 否則alertDialog.show()會報錯
        //Dialog.setTitle("提示")
        Dialog.setTitle(getString(R.string.remind))
        Dialog.setMessage("請將您的Mobile Nose插上充電線。")
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        Dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Accept))//是
        { dialog, _ ->

            dialog.dismiss()
        }
        Dialog.show()
    }

    private fun showDfuCompleteDialog() {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        //必須是android.app.AlertDialog.Builder 否則alertDialog.show()會報錯
        //Dialog.setTitle("提示")
        Dialog.setTitle(getString(R.string.remind))
        Dialog.setMessage("Mobile Nose已更新完成，請將您的Mobile Nose重新開機。\n按下Yes將返回到主畫面。")
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        Dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.Accept))//是
        { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        Dialog.show()
    }
}
