package com.microjet.airqi2.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.microjet.airqi2.*
import com.microjet.airqi2.BlueTooth.BLECallingTranslate
import com.microjet.airqi2.CustomAPI.Utils
import com.microjet.airqi2.Definition.BroadcastActions
import com.microjet.airqi2.Definition.BroadcastIntents
import com.microjet.airqi2.Definition.Colors
import kotlinx.android.synthetic.main.frg_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainFragment : Fragment(), View.OnTouchListener {

   /* enum class DetectionData(val range1: Long, val range2: Long, val maxValue: Float) {
        TVOC(220, 660, 1000f),
        CO2(700, 1000, 5000f),
        Temp(18, 25, 50f),
        Humi(45, 65, 100f),
        PM25(15, 35, 500f),
        PM10(54, 125, 600f)
    }
*/
    enum class DetectionData( val rangeArray:LongArray,val angleArray:FloatArray ) {
        TVOC(longArrayOf(0,220, 660,2200,5500,20000,60000), floatArrayOf(70f,85f,60f,25f,20f,10f)),
        CO2(longArrayOf(0,1500,60000),floatArrayOf(135f,135f)),
        Temp(longArrayOf(0,28,35,50),floatArrayOf(80f,110f,80f)),//以-10度為基準
        Humi(longArrayOf(0,45,65,100),floatArrayOf(80f,110f,80f)),
        PM25(longArrayOf(0,15,35,54,150,250,500),floatArrayOf(70f,85f,60f,25f,20f,10f)),
        PM10(longArrayOf(0,54,125,254,354,424,604),floatArrayOf(70f,85f,60f,25f,20f,10f))
    }

    private var mContext: Context? = null

    private var tvocDataFloat = 0f
    private var tempDataFloat = 0f
    private var humiDataFloat = 0f
    private var co2DataFloat = 0f
    private var pm25DataFloat = 0f
    private var pm10DataFloat = 0f
    private var preHeat = "0"
    //20180207
    private var isPumpOn = false


    private var dataForState = DetectionData.TVOC
  //  private var dataForState2 = DetectionData2.TVOC

    private var connState = false

    private var errorTime = 0

    private var scrollindex = 0
    private var scrollViewsArr = ArrayList<View>()


    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        mContext = this.context!!.applicationContext
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(mContext!!).registerReceiver(myBroadcastReceiver,
                makeMainFragmentUpdateIntentFilter())
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frg_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // 設定底部數值面板的標題文字下標
        tvBtmPMTitle.text = Utils.setTextSubscript("PM2.5", 2)
        tvBtmPM10Title?.text = Utils.setTextSubscript("PM10", 2)
        tvBtmCO2Title.text = Utils.setTextSubscript("eCO2", 3)

        show_TVOC?.setOnTouchListener(this)
        show_eCO2?.setOnTouchListener(this)
        show_Temp?.setOnTouchListener(this)
        show_RH?.setOnTouchListener(this)
        show_PM?.setOnTouchListener(this)
        show_PM10?.setOnTouchListener(this)
        imgLight?.setOnTouchListener(this)

        // 初始化inCircleTitle文字大小
        fixInCircleTextSize()
        //下滑更多
        slideMoreAnimation()
        //跑馬燈
        scrollingMission()
        setCircleBarInitialValue()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                v!!.parent.requestDisallowInterceptTouchEvent(true)

                val beforeState = dataForState

                when (v.id) {
                    R.id.show_TVOC -> {
                        dataForState = DetectionData.TVOC
                        inCircleBar.setColor(Colors.tvocCO2Colors, Colors.tvocCO2Angles)
                        setCircleBarValue(dataForState)
                    }
                    R.id.show_eCO2 -> {
                        dataForState = DetectionData.CO2
                        inCircleBar.setColor(Colors.eCO2Color, Colors.eco2Angles)
                        setCircleBarValue(dataForState)
                    }
                    R.id.show_Temp -> {
                        dataForState = DetectionData.Temp
                        inCircleBar.setColor(Colors.tempColors, Colors.tempAngles)
                        setCircleBarValue(dataForState)
                    }
                    R.id.show_RH -> {
                        dataForState = DetectionData.Humi
                        inCircleBar.setColor(Colors.humiColors, Colors.humiAngles)
                        setCircleBarValue(dataForState)
                    }
                    R.id.show_PM -> {
                        dataForState = DetectionData.PM25
                        inCircleBar.setColor(Colors.tvocCO2Colors, Colors.tvocCO2Angles)
                        setCircleBarValue(dataForState)
                    }
                    R.id.show_PM10 -> {
                        dataForState = DetectionData.PM10
                        inCircleBar.setColor(Colors.tvocCO2Colors, Colors.tvocCO2Angles)
                        setCircleBarValue(dataForState)
                    }
                    R.id.imgLight -> {
                        if (dataForState == DetectionData.TVOC || dataForState == DetectionData.CO2) {
                            //sendPumpCommand(BroadcastActions.INTENT_KEY_PUMP_ON)
                            MainActivity.mUartService?.setPumpOnOff(true)
                        } else if (dataForState == DetectionData.PM25 || dataForState == DetectionData.PM10) {
                            //sendPumpCommand(BroadcastActions.INTENT_KEY_PM25_FAN_ON)
                            MainActivity.mUartService?.setFanOn()
                        }
                        v.isPressed = true
                    }
                }
                pumpOnStatus(beforeState, dataForState)
                checkUIState()
            }
            MotionEvent.ACTION_UP -> {
                if (v!!.id == R.id.imgLight) {
                    if (dataForState == DetectionData.TVOC || dataForState == DetectionData.CO2) {
                        /*sendPumpCommand(BroadcastActions.INTENT_KEY_PUMP_OFF)
                    } else if (dataForState == DetectionData.PM25 || dataForState == DetectionData.PM10) {
                        sendPumpCommand(BroadcastActions.INTENT_KEY_PM25_FAN_OFF)*/

                        MainActivity.mUartService?.setPumpOnOff(false)
                    }
                    v.isPressed = false
                }
            }
        }
        return true
    }


    /*private fun sendPumpCommand(command: String) {
        val intent: Intent? = Intent(command)
        //intent!!.putExtra("status", command)
        mContext!!.sendBroadcast(intent)
    }*/

    /*private fun actionToSring(action: Int): String {
        when (action) {
            MotionEvent.ACTION_DOWN -> return "Down"
            MotionEvent.ACTION_MOVE -> return "Move"
            MotionEvent.ACTION_POINTER_DOWN -> return "Pointer down"
            MotionEvent.ACTION_UP -> return "UP"
            MotionEvent.ACTION_POINTER_UP -> return "Pointer up"
            MotionEvent.ACTION_OUTSIDE -> return "Outside"
            MotionEvent.ACTION_CANCEL -> return "Cancel"
        }
        return ""
    }*/


    override fun onResume() {
        super.onResume()

        EventBus.getDefault().register(this)
        checkUIState()
    }

    override fun onPause() {
        super.onPause()

        EventBus.getDefault().unregister(this)
        checkUIState()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            LocalBroadcastManager.getInstance(mContext!!).unregisterReceiver(myBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e("MainFrg", "onDestroy() called...")
    }


    private fun fixInCircleTextSize() {
        val dm = resources.displayMetrics
        val dpi = dm.densityDpi
        Log.i("DPI", "目前解析度為: $dpi")
        when (dpi) {
        /*DisplayMetrics.DENSITY_HIGH -> {   // HDPI
            //inCircleTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            inCircleState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }*/
            in DisplayMetrics.DENSITY_420..DisplayMetrics.DENSITY_XXHIGH -> {   // XXHDPI
                //inCircleTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                //inCircleState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                tvNotify.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                tvLastDetectTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            }
            in DisplayMetrics.DENSITY_560..DisplayMetrics.DENSITY_XXXHIGH -> {
                //inCircleTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                //inCircleState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                tvNotify.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                tvLastDetectTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            }
        }
    }

    private fun textSpannable(temp: String) {
        //val dm = resources.displayMetrics
        val textSpan = SpannableStringBuilder(temp)
        /*val text1Size = when (dm.densityDpi) {
            DisplayMetrics.DENSITY_DEFAULT -> convertSpToPx(50f)   // MDPI
            DisplayMetrics.DENSITY_HIGH -> convertSpToPx(30f)   // HDPI
            in DisplayMetrics.DENSITY_420..DisplayMetrics.DENSITY_XXHIGH -> convertSpToPx(34f)   // XXHDPI
            in DisplayMetrics.DENSITY_560..DisplayMetrics.DENSITY_XXXHIGH -> convertSpToPx(36f)   // XXXHDPI
            else -> convertSpToPx(30f)
        }
        val text2Size = when (dm.densityDpi) {
            DisplayMetrics.DENSITY_DEFAULT -> convertSpToPx(30f)   // MDPI
            DisplayMetrics.DENSITY_HIGH -> convertSpToPx(18f)   // HDPI
            in DisplayMetrics.DENSITY_420..DisplayMetrics.DENSITY_XXHIGH -> convertSpToPx(20f)   // XXHDPI
            in DisplayMetrics.DENSITY_560..DisplayMetrics.DENSITY_XXXHIGH -> convertSpToPx(22f)   // XXXHDPI
            else -> convertSpToPx(18f)
        }*/

        textSpan.setSpan(RelativeSizeSpan(0.5f), temp.indexOf(" ") + 1, temp.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        //textSpan.setSpan(AbsoluteSizeSpan(text1Size),temp.indexOf(" ") - 1, temp.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        inCircleValue.text = textSpan
    }

    /*private fun setThresholdValue(dataForState: DetectionData) {
        //tvRange1.text = dataForState.range1.toString()
        //tvRange2.text = dataForState.range2.toString()
        tvRange1.visibility = View.INVISIBLE
        tvRange2.visibility = View.INVISIBLE
    }*/
/*
    private fun setBarMaxValue(state: DetectionData) {
        inCircleBar.setThreadholdValue(floatArrayOf(state.range1.toFloat(), state.range2.toFloat()))
        inCircleBar.setMaxValues(state.maxValue)
    }*/
    private fun setCircleBarValue(state: DetectionData) {
        inCircleBar.setAllCondition(state.rangeArray,state.angleArray)
    }
    @SuppressLint("SetTextI18n")
    private fun setBtmCurrentValue() {
        val finalTempVal = Utils.convertTemperature(mContext!!, tempDataFloat)

        tvBtmTVOCValue?.text = Utils.setTextSubscript("${tvocDataFloat.toInt()} ppb")
        //if (MyApplication.isPM25 == "000000000000") {
        if (pm25DataFloat == 65535f) {
            tvBtmPM25Value?.text = "Not Support"
            show_PM?.isEnabled = false
        } else {
            tvBtmPM25Value?.text = Utils.setTextSubscript("${pm25DataFloat.toInt()} μg/m³")
            show_PM?.isEnabled = true
        }
        tvBtmCO2Value?.text = Utils.setTextSubscript("${co2DataFloat.toInt()} ppm")
        //tvBtmTEMPValue?.text = Utils.setTextSubscript("${tempDataFloat} ℃")
        tvBtmTEMPValue?.text = Utils.setTextSubscript(finalTempVal)
        tvBtmHUMIValue?.text = Utils.setTextSubscript("${humiDataFloat.toInt()} %")
        tvBtmPM10Value?.text = Utils.setTextSubscript("${pm10DataFloat.toInt()} μg/m³")
    }

    @SuppressLint("SetTextI18n")
    private fun tvocStatusTextShow(currentValue: Float) {
        when (currentValue) {
            in 0..219 -> {
                tvNotify?.text = getString(R.string.text_message_air_good)
                inCircleState.text = getString(R.string.text_label_status_good)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_01)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_green)
            }
            in 220..659 -> {
                tvNotify?.text = getString(R.string.text_message_air_mid)
                inCircleState.text = getString(R.string.text_label_status_mid)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Moderate))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Moderate))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_02)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_orange)
            }
            in 660..2199 -> {
                tvNotify?.text = getString(R.string.text_message_air_Medium_Orange)
                inCircleState.text = getString(R.string.text_label_status_medium_Orange)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Orange))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Orange))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_03)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_orange)
            }
            in 2200..5499 -> {
                tvNotify?.text = getString(R.string.text_message_air_bad)
                inCircleState.text = getString(R.string.text_label_status_bad)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_04)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
            in 5500..19999 -> {
                tvNotify?.text = getString(R.string.text_message_air_Serious_Purple)
                inCircleState.text = getString(R.string.text_label_status_Serious_Purple)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Purple))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Purple))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_05)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
            else -> {
                tvNotify?.text = getString(R.string.text_message_air_Extreme_Dark_Purple)
                inCircleState.text = getString(R.string.text_label_status_Extreme_Dark_Purple)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Test_Unhealthy))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Test_Unhealthy))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_06)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
        }
    }

    private fun eco2StatusTextShow(currentValue: Float) {
        when (currentValue) {
            in 0..1499 -> {
                tvNotify?.text = getString(R.string.new_ECO2_main_page_Description_Green)
                inCircleState.text = getString(R.string.new_ECO2_main_page_Circle_Green)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_01)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_green)
            }
            else -> {
                tvNotify?.text = getString(R.string.new_ECO2_main_page_Description_Red)
                inCircleState.text = getString(R.string.new_ECO2_main_page_Circle_Red)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_04)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
        }
    }

    private fun tempStatusTextShow(currentValue: Float) {
        when (currentValue) {
            in 18..24 -> {
                tvNotify?.text = getString(R.string.text_message_temperature)
                inCircleState.text = getString(R.string.text_label_temp_Green)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                //20180202
                imgLight.setImageResource(R.drawable.face_icon_temp_02)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.bg_temp_green)
            }
            in 25..200 -> {
                tvNotify?.text = getString(R.string.text_message_temperature)
                inCircleState.text = getString(R.string.text_label_temp_Red)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                //20180202
                imgLight.setImageResource(R.drawable.face_icon_temp_03)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.bg_temp_red)
            }
            else -> {
                tvNotify?.text = getString(R.string.text_message_temperature)
                inCircleState.text = getString(R.string.text_label_temp_Blue)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.progressBarMiddleBlue))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.progressBarMiddleBlue))
                //20180202
                imgLight.setImageResource(R.drawable.face_icon_temp_01)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.bg_temp_blue)
            }
        }
    }

    private fun humiStatusTextShow(currentValue: Float) {
        when (currentValue) {
            in 0..44 -> {
                tvNotify?.text = getString(R.string.text_message_humidity)
                inCircleState.text = getString(R.string.text_label_Humi_Blue)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.progressBarMiddleBlue))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.progressBarMiddleBlue))
                //20180202
                imgLight.setImageResource(R.drawable.face_icon_hmi_01)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.bg_rh_blue)
            }
            in 45..65 -> {
                tvNotify?.text = getString(R.string.text_message_humidity)
                inCircleState.text = getString(R.string.text_label_Humi_Green)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                //20180202
                imgLight.setImageResource(R.drawable.face_icon_hmi_02)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.bg_rh_green)
            }
            else -> {

                tvNotify?.text = getString(R.string.text_message_humidity)
                inCircleState.text = getString(R.string.text_label_Humi_Red)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                //20180202
                imgLight.setImageResource(R.drawable.face_icon_hmi_03)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.bg_rh_red)
            }
        }
    }

    private fun pm25StatusTextShow(currentValue: Float) {
        when (currentValue) {
            in 0..15 -> {
                tvNotify?.text = getString(R.string.message_pm25_Green)
                inCircleState.text = getString(R.string.label_pm25_Green)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_01)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_green)
            }

            in 16..34 -> {
                tvNotify?.text = getString(R.string.message_pm25_Yellow)
                inCircleState.text = getString(R.string.label_pm25_Yellow)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Moderate))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Moderate))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_02)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_orange)
            }
            in 35..54 -> {
                tvNotify?.text = getString(R.string.message_pm25_Orange)
                inCircleState.text = getString(R.string.label_pm25_Orange)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Orange))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Orange))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_03)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_orange)
            }
            in 55..150 -> {
                tvNotify?.text = getString(R.string.message_pm25_Red)
                inCircleState.text = getString(R.string.label_pm25_Red)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_04)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
            in 151..250 -> {
                tvNotify?.text = getString(R.string.message_pm25_Purple)
                inCircleState.text = getString(R.string.label_pm25_Purple)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Purple))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Purple))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_05)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
            else -> {
                tvNotify?.text = getString(R.string.message_pm25_Brown)
                inCircleState.text = getString(R.string.label_pm25_Brown)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Test_Unhealthy))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Test_Unhealthy))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_06)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
        }
    }

    private fun pm10StatusTextShow(currentValue: Float) {
        when (currentValue) {
            in 0..53 -> {
                tvNotify?.text = getString(R.string.message_pm10_Green)
                inCircleState.text = getString(R.string.label_pm10_Green)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Good))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_01)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_green)
            }

            in 54..124 -> {
                tvNotify?.text = getString(R.string.message_pm10_Yellow)
                inCircleState.text = getString(R.string.label_pm10_Yellow)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Moderate))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Moderate))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_02)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_orange)
            }
            in 125..253 -> {
                tvNotify?.text = getString(R.string.message_pm10_Orange)
                inCircleState.text = getString(R.string.label_pm10_Orange)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Orange))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Orange))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_03)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_orange)
            }
            in 254..353 -> {
                tvNotify?.text = getString(R.string.message_pm10_Red)
                inCircleState.text = getString(R.string.label_pm10_Red)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Bad))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_04)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
            in 354..423 -> {
                tvNotify?.text = getString(R.string.message_pm10_Purple)
                inCircleState.text = getString(R.string.label_pm10_Purple)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Purple))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Main_textResult_Purple))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_05)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
            else -> {
                tvNotify?.text = getString(R.string.message_pm10_Brown)
                inCircleState.text = getString(R.string.label_pm10_Brown)
                inCircleValue.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Test_Unhealthy))
                inCircleState.setTextColor(
                        ContextCompat.getColor(mContext!!, R.color.Test_Unhealthy))
                //20180131
                imgLight.setImageResource(R.drawable.face_icon_06)
                //20180301
                PrimaryBackground.setBackgroundResource(R.drawable.app_bg_cloud_red)
            }
        }
    }

    private fun makeMainFragmentUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BroadcastActions.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BroadcastActions.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BroadcastActions.ACTION_GET_NEW_DATA)
        intentFilter.addAction(BroadcastActions.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    @SuppressLint("SimpleDateFormat")
    @Synchronized
    private fun checkUIState() {
        if (connState) {
            if (preHeat == "255") {
                setNewsPanelShow(true)

                when (dataForState) {
                    DetectionData.TVOC -> {
                        inCircleTitle.text = getString(R.string.text_label_tvoc_detect)


                        inCircleBar.inputCurrentValue(tvocDataFloat)

                        tvocStatusTextShow(tvocDataFloat)
                        val temp = tvocDataFloat.toInt().toString() + " ppb"
                        textSpannable(temp)
                    }
                    DetectionData.CO2 -> {
                        inCircleTitle.text = getString(R.string.text_label_eco2_detect)

                        inCircleBar.inputCurrentValue(co2DataFloat)
                        eco2StatusTextShow(co2DataFloat)
                        val temp = co2DataFloat.toInt().toString() + " ppm"
                        textSpannable(temp)
                    }
                    DetectionData.Temp -> {
                        inCircleTitle.text = getString(R.string.text_label_temp_detect)
                        inCircleBar.inputCurrentValue(tempDataFloat+10f)
                        tempStatusTextShow(tempDataFloat)
                        val temp = Utils.convertTemperature(mContext!!, tempDataFloat)// tempDataFloat.toString() + " °C"
                        textSpannable(temp)
                    }

                    DetectionData.Humi -> {
                        inCircleTitle.text = getString(R.string.text_label_humi_detect)
                        inCircleBar.inputCurrentValue(humiDataFloat)
                        humiStatusTextShow(humiDataFloat)
                        val temp = humiDataFloat.toInt().toString() + " %"
                        textSpannable(temp)
                    }
                    DetectionData.PM25 -> {
                        inCircleTitle.text = getString(R.string.text_label_pm25_detect)
                        inCircleBar.inputCurrentValue(pm25DataFloat)
                        pm25StatusTextShow(pm25DataFloat)
                        val temp = pm25DataFloat.toInt().toString() + " μg/m³"
                        textSpannable(temp)
                    }
                    DetectionData.PM10 -> {
                        inCircleTitle.text = getString(R.string.text_label_pm10_detect)
                        inCircleBar.inputCurrentValue(pm10DataFloat)
                        pm10StatusTextShow(pm10DataFloat)
                        val temp = pm10DataFloat.toInt().toString() + " μg/m³"
                        textSpannable(temp)
                    }
                }
                setBtmCurrentValue()
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                val date = Date()
                tvLastDetectTime.text = dateFormat.format(date).toString()
            } else {
                setNewsPanelShow(false)
            }
        } else {
            inCircleValue?.text = " "
            inCircleState?.text = " "
            tvBtmTVOCValue?.text = "---"
            tvBtmPM25Value?.text = "---"
            tvBtmCO2Value?.text = "---"
            tvBtmTEMPValue?.text = "---"/*currentValue[0] + " ℃"*/
            tvBtmHUMIValue?.text = "---"/*currentValue[1] + " %"*/
            tvBtmPM10Value?.text = "---"
            tvNotify?.text = " "
            tvLastDetectTime?.text = " "
            inCircleBar?.inputCurrentValue(0f)
           // inCircleBar?.setCurrentValues(0f)
            //imgLight?.setImageResource(R.drawable.app_android_icon_light)
            setNewsPanelShow(true)
        }
        dashBoardChange(connState, dataForState)
    }

    private val myBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BroadcastActions.ACTION_GATT_DISCONNECTED -> {
                    connState = false
                }

                BroadcastActions.ACTION_GATT_CONNECTED -> {
                    connState = true
                }

                BroadcastActions.ACTION_DATA_AVAILABLE -> {
                    dataAvailable(intent)
                }
            }
            checkUIState()
        }
    }

    //20180207
    private fun pumpOnStatus(beforeState: DetectionData, dataState: DetectionData) {
        if (beforeState != dataState) {
            if (isPumpOn) {
                val intent: Intent? = Intent(BroadcastIntents.PRIMARY)
                intent!!.putExtra("status", BroadcastActions.INTENT_KEY_PUMP_OFF)
                mContext!!.sendBroadcast(intent)
                //20180202
                imgLight.isPressed = false
                //20180207
                isPumpOn = false
            }
        }
    }

    private fun dataAvailable(intent: Intent) {
        val txValue = intent.getByteArrayExtra(BroadcastActions.ACTION_EXTRA_DATA)
        if (errorTime >= 3) {
            errorTime = 0
        }
        if (!checkCheckSum(txValue)) {
            errorTime += 1
        } else {
            when (txValue[2]) {
                0xB0.toByte() -> {

                }
                0xC0.toByte() -> {
                    val hashMap = BLECallingTranslate.getAllSensorC0KeyValue(txValue)
                    tempDataFloat = hashMap[TvocNoseData.C0TEMP]!!.toFloat()
                    humiDataFloat = hashMap[TvocNoseData.C0HUMI]!!.toFloat()
                    tvocDataFloat = hashMap[TvocNoseData.C0TVOC]!!.toFloat()
                    co2DataFloat = hashMap[TvocNoseData.C0ECO2]!!.toFloat()
                    pm25DataFloat = hashMap[TvocNoseData.C0PM25]!!.toFloat()
                    preHeat = (hashMap[TvocNoseData.C0PREH]!!)
                }
                0xD0.toByte() -> {
                    val hashMap = BLECallingTranslate.getAllSensorD0KeyValue(txValue)
                    pm10DataFloat = hashMap[TvocNoseData.D0PM10]!!.toFloat()
                    Log.d("0xD0", hashMap.toString())
                }
            }
        }
    }


    private fun checkCheckSum(input: ByteArray): Boolean {
        var checkSum = 0x00
        val max = 0xFF.toByte()
        for (i in 0 until input.size) {
            checkSum += input[i]
        }
        val checkSumByte = checkSum.toByte()
        return checkSumByte == max

    }

    private fun slideMoreAnimation() {
        val animShake = AnimationUtils.loadAnimation(mContext, R.anim.textview_shake)
        slideMore?.startAnimation(animShake)
        Handler().postDelayed({ slideMore?.visibility = View.GONE }, 6000)
    }

    private fun scrollingMission() {
        Handler().postDelayed({
            if (TvocNoseData.scrollingList.isNotEmpty()) {
                setViewSingleLine()
            } else {
                scrollindex++
                if (scrollindex < 10) {
                    scrollingMission()
                }
            }
        }, 6000)
    }



    @SuppressLint("SetTextI18n")
    private fun setViewSingleLine() {
        scrollViewsArr.clear()//记得加这句话，不然可能会产生重影现象
        Log.e("scrollingList", TvocNoseData.scrollingList.toString())
        for (i in 0 until TvocNoseData.scrollingList.size) {
            //设置滚动的单个布局
            val vScrollivs = LayoutInflater.from(mContext).inflate(R.layout.item_view_single, null)
            //初始化布局的控件
            val tv1 = vScrollivs.findViewById<TextView>(R.id.tvScrollContent)
            /**
             * 設置監聽
             */

            tv1.setOnClickListener {
                //val url = Uri.parse(TvocNoseData.scrollingList[i]["url"].toString())
                //val intent = Intent(Intent.ACTION_VIEW, url)
                //startActivity(intent)
                val url = TvocNoseData.scrollingList[i]["url"].toString()
                val intent: Intent? = Intent(context, ExperienceWebview::class.java)
                intent!!.putExtra("URL", url)
                startActivity(intent)
            }

            //进行对控件赋值
            tv1?.text = "${TvocNoseData.scrollingList[i]["title"]}..."
            Log.e("scrollingList[title]", TvocNoseData.scrollingList[i]["title"].toString())
            //添加到循环滚动数组里面去
            scrollViewsArr.add(vScrollivs)
        }
        upview1?.setViews(scrollViewsArr)
    }

    private fun setNewsPanelShow(enable: Boolean) {
        if(enable) {
            upview1?.visibility = View.VISIBLE
        } else {
            upview1?.visibility = View.INVISIBLE
        }
    }

    @Subscribe
    fun onEvent(bleEvent: BleEvent) {
        /* 處理事件 */
        Log.d("AirAction", bleEvent.message)
        when (bleEvent.message) {
            "new Topic get" -> {
                view?.post({ setViewSingleLine() })
            }
        }
    }

    private fun setCircleBarInitialValue(){
        when (dataForState){//可依初始值不同給定不同的參數
            DetectionData.TVOC->{inCircleBar.setColor(Colors.tvocCO2Colors, Colors.tvocCO2Angles)}
            DetectionData.CO2->{inCircleBar.setColor(Colors.eCO2Color, Colors.eco2Angles)}
            DetectionData.Temp->{inCircleBar.setColor(Colors.tempColors, Colors.tempAngles)}
            DetectionData.Humi->{inCircleBar.setColor(Colors.humiColors, Colors.humiAngles)}
            DetectionData.PM25->{inCircleBar.setColor(Colors.tvocCO2Colors, Colors.tvocCO2Angles)}
            DetectionData.PM10->{inCircleBar.setColor(Colors.tvocCO2Colors, Colors.tvocCO2Angles)}
        }
        setCircleBarValue(dataForState)
    }

    private fun dashBoardChange(connstate: Boolean, data: DetectionData) {
        for (i in 0 until valPanel.childCount) {
            for (y in 0 until llayoutbtmline1.childCount) {
                llayoutbtmline1.getChildAt(y).setBackgroundColor(Color.TRANSPARENT)
            }
            for (y in 0 until llayoutbtmline2.childCount) {
                llayoutbtmline2.getChildAt(y).setBackgroundColor(Color.TRANSPARENT)
            }
        }

        if (connstate) {
            when (data.ordinal) {
                0 -> {
                    show_TVOC?.setBackgroundResource(R.color.grayColor_60A2)
                }
                1 -> {
                    show_eCO2?.setBackgroundResource(R.color.grayColor_60A2)
                }
                2 -> {
                    show_Temp?.setBackgroundResource(R.color.grayColor_60A2)
                }
                3 -> {
                    show_RH?.setBackgroundResource(R.color.grayColor_60A2)
                }
                4 -> {
                    show_PM?.setBackgroundResource(R.color.grayColor_60A2)
                }
                5 -> {
                    show_PM10?.setBackgroundResource(R.color.grayColor_60A2)
                }
            }
        }
    }

}
