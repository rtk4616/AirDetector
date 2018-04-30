package com.microjet.airqi2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.SeekBar
import com.microjet.airqi2.Definition.BroadcastActions
import com.microjet.airqi2.Definition.BroadcastIntents
import com.microjet.airqi2.Definition.SavePreferences
import kotlinx.android.synthetic.main.activity_setting.*
import com.warkiz.widget.IndicatorSeekBar



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
    private var swRunInBgVal: Boolean = false
    private var swTotalNotifyVal: Boolean = false
    //20180130
    private var batSoundVal: Boolean = false
    private var swLedPowerVal: Boolean = true
    //20180227
    private var swCloudVal: Boolean = true

    private var seekBarVal: Int = 660

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        uiSetListener()

        mPreference = getSharedPreferences(SavePreferences.SETTING_KEY, 0)

        initActionBar()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        readPreferences()   // 當Activity onResume時載入設定值

        text_local_uuid.text = MyApplication.getPsuedoUniqueID()
        text_device_ver.text = resources.getString(R.string.text_label_device_version) + MyApplication.getDeviceVersion()
    }

    private fun readPreferences() {
        swAllowNotifyVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_NOTIFY, false)
        swMessageVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_MESSAGE, false)
        swViberateVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_VIBERATION, false)
        swSoundVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_SOUND, false)
        swRunInBgVal = mPreference!!.getBoolean(SavePreferences.SETTING_ALLOW_RUN_IN_BG, false)
        swTotalNotifyVal = mPreference!!.getBoolean(SavePreferences.SETTING_TOTAL_POLLUTION_NOTIFY, false)
        //20180206
        batSoundVal = mPreference!!.getBoolean(SavePreferences.SETTING_BATTERY_SOUND, false)

        swLedPowerVal = mPreference!!.getBoolean(SavePreferences.SETTING_LED_SWITCH, true)

        //20180227
        swCloudVal = mPreference!!.getBoolean(SavePreferences.SETTING_CLOUD_FUN, true)

        seekBarVal = mPreference!!.getInt(SavePreferences.SETTING_NOTIFY_VALUE, 660)


        swAllowNotify.isChecked = swAllowNotifyVal

        if (swAllowNotify.isChecked) {
            cgMessage.visibility = View.VISIBLE
            cgVibration.visibility = View.VISIBLE
            cgSound.visibility = View.VISIBLE
            cgLowBatt.visibility = View.VISIBLE
        } else {
            cgMessage.visibility = View.GONE
            cgVibration.visibility = View.GONE
            cgSound.visibility = View.GONE
            cgLowBatt.visibility = View.GONE
        }

        swMessage.isChecked = swMessageVal
        swVibrate.isChecked = swViberateVal
        swSound.isChecked = swSoundVal
        //20180130
        //swPump.isChecked = swPumpVal
        //20180206
        batSound.isChecked = batSoundVal

        ledPower.isChecked = swLedPowerVal

        swCloudFunc.isChecked = swCloudVal

        seekBar.setProgress(seekBarVal.toFloat())
    }

    private fun uiSetListener() {

        swAllowNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cgMessage.visibility = View.VISIBLE
                cgVibration.visibility = View.VISIBLE
                cgSound.visibility = View.VISIBLE
                seekBar.visibility = View.VISIBLE
                cgLowBatt.visibility = View.VISIBLE
            } else {
                cgMessage.visibility = View.GONE
                cgVibration.visibility = View.GONE
                cgSound.visibility = View.GONE
                seekBar.visibility = View.GONE
                cgLowBatt.visibility = View.GONE

                mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_MESSAGE,
                        isChecked).apply()

                mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_VIBERATION,
                        isChecked).apply()

                mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_SOUND,
                        isChecked).apply()

                mPreference!!.edit().putBoolean(SavePreferences.SETTING_BATTERY_SOUND,
                        isChecked).apply()
            }

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_NOTIFY,
                    isChecked).apply()
        }

        swMessage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                text_msg_stat.text = getString(R.string.text_setting_on)
                val mainintent = Intent(BroadcastIntents.PRIMARY)
                mainintent.putExtra("status", "message")
                sendBroadcast(mainintent)

                Log.d("message", "messageSETTING")
            } else {
                text_msg_stat.text = getString(R.string.text_setting_off)
            }

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_MESSAGE,
                    isChecked).apply()
        }

        swVibrate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                text_vibrate_stat.text = getString(R.string.text_setting_on)
            } else {
                text_vibrate_stat.text = getString(R.string.text_setting_off)
            }

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_VIBERATION,
                    isChecked).apply()
        }

        swSound.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                text_sound_stat.text = getString(R.string.text_setting_on)
            } else {
                text_sound_stat.text = getString(R.string.text_setting_off)
            }

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_ALLOW_SOUND,
                    isChecked).apply()
        }

        seekBar.setOnSeekChangeListener(object : IndicatorSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: IndicatorSeekBar, progress: Int, progressFloat: Float, fromUserTouch: Boolean) {
                mPreference!!.edit().putInt(SavePreferences.SETTING_NOTIFY_VALUE,
                        progress).apply()
            }

            override fun onSectionChanged(seekBar: IndicatorSeekBar, thumbPosOnTick: Int, textBelowTick: String, fromUserTouch: Boolean) {
                //這個回調僅在分段系列`discrete`的SeekBar生效，當為連續系列`continuous`則不回調。
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar, thumbPosOnTick: Int) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {}
        })


        //20180206
        batSound.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                text_bat_stat!!.text = getString(R.string.text_setting_on)
            } else {
                text_bat_stat!!.text = getString(R.string.text_setting_off)
            }
            mPreference!!.edit().putBoolean(SavePreferences.SETTING_BATTERY_SOUND,
                    isChecked).apply()
        }

        ledPower.setOnCheckedChangeListener { _, isChecked ->
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

            val intent: Intent? = Intent(BroadcastIntents.PRIMARY)

            if (isChecked) {
                text_clouud_stat!!.text = getString(R.string.text_setting_on)
                intent!!.putExtra("status", BroadcastActions.INTENT_KEY_CLOUD_ON)
            } else {
                text_clouud_stat.text = getString(R.string.text_setting_off)
                intent!!.putExtra("status", BroadcastActions.INTENT_KEY_CLOUD_OFF)
            }

            sendBroadcast(intent)

            mPreference!!.edit().putBoolean(SavePreferences.SETTING_CLOUD_FUN,
                    isChecked).apply()

        }
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
}
