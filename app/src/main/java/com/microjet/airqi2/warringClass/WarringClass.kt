package com.microjet.airqi2.warringClass

import android.content.Context
import android.util.Log
import com.microjet.airqi2.Definition.SavePreferences
import com.microjet.airqi2.R

/**
 * Created by B00055 on 2018/5/21.
 */
class WarringClass (context:Context)
{
    //要有通知，警告聲，
    private val mContext=context
    private val mPreference= mContext.getSharedPreferences(SavePreferences.SETTING_KEY, 0)
    private var tvocSound=WarringSound(context, R.raw.tvoc_over)
    private var pm25Sound=WarringSound(context, R.raw.pm25_over)
    private var WNotification=WarringNotification(context)
    private var tvocVibrator=WarringVibrator(context,mPreference.getInt(SavePreferences.SETTING_TVOC_NOTIFY_VALUE, 660))
    private var pm25Vibrator=WarringVibrator(context,mPreference.getInt(SavePreferences.SETTING_PM25_NOTIFY_VALUE, 16))

    private var allowNotify = false
    private var allowVibrator = false
    private var allowSound = false
    private var tvocAlertValue = 660
    private var pm25AlertValue = 16
    private var tvocArrayAlertValue=ArrayList<Int>()
    private var pm25ArrayAlertValue=ArrayList<Int>()
    init {
        tvocArrayAlertValue.add(0)
        tvocArrayAlertValue.add(219)
        tvocArrayAlertValue.add(659)
        tvocArrayAlertValue.add(2199)
        tvocArrayAlertValue.add(5499)
        tvocArrayAlertValue.add(19999)
        tvocArrayAlertValue.add(60000)
        tvocVibrator.setArrayPoint(tvocArrayAlertValue)
        pm25ArrayAlertValue.add(0)
        pm25ArrayAlertValue.add(14)
        pm25ArrayAlertValue.add(34)
        pm25ArrayAlertValue.add(54)
        pm25ArrayAlertValue.add(149)
        pm25ArrayAlertValue.add(249)
        pm25ArrayAlertValue.add(500)
        pm25Vibrator.setArrayPoint(pm25ArrayAlertValue)

       //WVibrator=WarringVibrator(context)
       //WNotification=WarringNotification(context)
    }
    fun judgeValue(tvocValue: Int, pm25Value: Int){
        allowNotify = mPreference.getBoolean(SavePreferences.SETTING_ALLOW_NOTIFY, false)
        allowVibrator = mPreference.getBoolean(SavePreferences.SETTING_ALLOW_VIBERATION,false)
        allowSound = mPreference.getBoolean(SavePreferences.SETTING_ALLOW_SOUND,false)
        tvocAlertValue = mPreference.getInt(SavePreferences.SETTING_TVOC_NOTIFY_VALUE, 660)
        pm25AlertValue = mPreference.getInt(SavePreferences.SETTING_PM25_NOTIFY_VALUE, 16)
        tvocVibrator.warringValue = tvocAlertValue
        pm25Vibrator.warringValue = pm25AlertValue
        tvocSound.warringValue = tvocAlertValue
        pm25Sound.warringValue = pm25AlertValue
        when (allowNotify){
            true->{
                checkVibrator(tvocValue,pm25Value)
                checkSound(tvocValue,pm25Value)
            }
            false->{
                Log.v(this.javaClass.simpleName,"allowNotify:$allowNotify")
            }
        }
    }
    private fun checkVibrator(tvocValue: Int, pm25Value: Int){
        when (allowVibrator){
            true->{
                tvocVibrator.sendVibrator(tvocValue)
                pm25Vibrator.sendVibrator(pm25Value)
            }
            false->{
                Log.v(this.javaClass.simpleName,"allowVibrator:$allowVibrator")
            }
        }
    }
    private fun checkSound(tvocValue: Int, pm25Value: Int){
        when (allowSound){
            true->{
                pm25Sound.soundPlay(pm25Value)
                tvocSound.soundPlay(tvocValue)
            }
            false->{
                Log.v(this.javaClass.simpleName,"allowSound:$allowSound")
            }
        }
    }
}