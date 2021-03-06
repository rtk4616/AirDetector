package com.microjet.airqi2.Account.AccountTask

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import com.microjet.airqi2.Account.AccountObject
import com.microjet.airqi2.BleEvent
import com.microjet.airqi2.TvocNoseData
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Created by B00190 on 2018/6/22.
 */
class AccountLoginTask(gettedActivity: Activity) : AsyncTask<String, Int, String>() {
    private val TAG: String = "AccountLoginTask"
    private val useActivity: Activity = gettedActivity

    override fun onPreExecute() {
        super.onPreExecute()
        //等候dialog視窗開啟
        AccountObject.openWatiDialog(useActivity)
    }


    //主要背景執行
    override fun doInBackground(vararg params: String?): String? {
        val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        val mediaType = MediaType.parse("application/x-www-form-urlencoded")
        val userInfo = "email=" + params[0] + "&password=" + params[1]
        val body = RequestBody.create(mediaType, userInfo)
        val request = Request.Builder()
                .url("https://mjairql.com/api/v1/login")
                .post(body)
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val res: String = response.body()!!.string().toString()
                Log.e(TAG, res)
                val returnResult = JSONObject(res).getJSONObject("success")
                TvocNoseData.cloudToken = returnResult.getString("token").toString()
                TvocNoseData.cloudName = returnResult.getJSONObject("userData").getString("name").toString()
                TvocNoseData.cloudEmail = returnResult.getJSONObject("userData").getString("email").toString()
                TvocNoseData.cloudDeviceArr = returnResult.getJSONArray("deviceList").toString()
                return "successNetwork"
            } else {
                return "ResponseError"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "ReconnectNetwork"
        }
    }

    override fun onPostExecute(result: String?) {
        Log.e(TAG, result)
        if(result != null){
            AccountObject.accountLoginStrResult =result
            EventBus.getDefault().post(BleEvent("loginTaskResult"))
            Log.e(TAG, result)
        }
    }


}