package com.microjet.airqi2

import android.os.AsyncTask
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import io.realm.Realm
import io.realm.Sort
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient


/**
 * Created by B00175 on 2018/3/30.
 */
// 上傳非同步執行類別
class UploadTask: AsyncTask<String, Void, String>() {

    private val realm = Realm.getDefaultInstance()
    private val query = realm.where(AsmDataModel::class.java).equalTo("UpLoaded", "0").findAll().sort("Created_time", Sort.ASCENDING)
    private val itemCount = query.size

    override fun doInBackground(vararg params: String): String? {

        if (itemCount > 0) {
            try {
                val client = OkHttpClient()
                val mediaType = MediaType.parse("application/x-www-form-urlencoded")
                val UUID = MyApplication.getPsuedoUniqueID()
                val mDeviceAddress = params[0]
                val uploadToken = "Bearer " + params[1]
                val resistraionID = FirebaseInstanceId.getInstance().token
                val weather = query.toArray()

                val body = RequestBody.create(mediaType, "data=" + weather.toString())

                val request = Request.Builder()
                        .url("https://mjairql.com/api/v1/upUserData")
                        .post(body)
                        .addHeader("authorization", uploadToken)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {

                } else {
                    query.forEach {
                        it.upLoaded = "1"
                    }
                }
            } catch (e: Exception) {
                Log.e("return_body_erro", e.toString())
            }
        }


        return null
    }

    //取得資料庫資料並封裝上傳資料
    /*
    private fun prepareData(DeviceAddress: String): RequestBody? {
        //首先將要丟進陣列內的JSON物件存好內容後丟進陣列
        val unUpLoadedRealm = Realm.getDefaultInstance()
        val unUpLoadedQuery = unUpLoadedRealm.where(AsmDataModel::class.java)
        val unUpLoadedResult = unUpLoadedQuery.equalTo("UpLoaded", "0").findAll()
        Log.e("未上傳ID", unUpLoadedResult.toString())
        Log.e("未上傳資料筆數", unUpLoadedResult.size.toString())
        Log.e("未上傳資料", unUpLoadedResult.toString().toString())
        val UUID = MyApplication.getPsuedoUniqueID()
        //製造RequestBody的地方
        var postBody: RequestBody? = null
        //20170227
        val packJsonFinalObj = JSONObject()            //用來當內層被丟進陣列內的JSON物件
        val packJsonBigArry = JSONArray()                //JSON陣列
        try {
            if (unUpLoadedResult.size > 0) {
                for (i in unUpLoadedResult.indices) {
                    if (i == 6000) {
                        break
                    }
                    recordChangDataId.add(unUpLoadedResult[i]!!.dataId)
                    Log.i("text", "i=" + i + "\n")
                    val packJsonObj = JSONObject()            //單筆weather資料
                    packJsonObj.put("temperature", unUpLoadedResult[i]!!.tempValue)
                    packJsonObj.put("humidity", unUpLoadedResult[i]!!.humiValue)
                    packJsonObj.put("tvoc", unUpLoadedResult[i]!!.tvocValue)
                    packJsonObj.put("eco2", unUpLoadedResult[i]!!.ecO2Value)
                    packJsonObj.put("pm25", unUpLoadedResult[i]!!.pM25Value)
                    packJsonObj.put("longitude", unUpLoadedResult[i]!!.longitude!!.toString())
                    packJsonObj.put("latitude", unUpLoadedResult[i]!!.latitude!!.toString())
                    packJsonObj.put("timestamp", unUpLoadedResult[i]!!.created_time)
                    Log.e("timestamp", "i=" + i + "timestamp=" + unUpLoadedResult[i]!!.created_time!!.toString())
                    packJsonBigArry.put(packJsonObj)
                }
            } else {
                Log.e("未上傳資料筆數", unUpLoadedResult.size.toString())
            }
            packJsonFinalObj.put("uuid", UUID)
            packJsonFinalObj.put("mac_address", DeviceAddress)
            packJsonFinalObj.put("registration_id", FirebaseInstanceId.getInstance().getToken())
            //再來將JSON陣列設定key丟進JSON物件
            packJsonFinalObj.put("weather", packJsonBigArry)
            Log.e("全部資料", packJsonFinalObj.toString())
            val mediaType = MediaType.parse("application/x-www-form-urlencoded")
            postBody = RequestBody.create(mediaType, "data=" + packJsonFinalObj.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        unUpLoadedRealm.close()
        return postBody
    }

    //上傳資料庫資料
    private fun sendDataToClound(body: RequestBody, MyToKen: String): Boolean {
        var upLoadedResponse: Response? = null
        var upLoadedReselt = java.lang.Boolean.parseBoolean(null)
        try {
            if (body.contentLength() > 0) {
                //丟資料
                val request = Request.Builder()
                        .url("https://mjairql.com/api/v1/upUserData")
                        .post(body)
                        .addHeader("authorization", "Bearer " + MyToKen)
                        .addHeader("content-type", "application/x-www-form-urlencoded")
                        .build()
                try {
                    val client = OkHttpClient.Builder()
                            .connectTimeout(0, TimeUnit.SECONDS)
                            .writeTimeout(0, TimeUnit.SECONDS)
                            .readTimeout(0, TimeUnit.SECONDS)
                            .build()
                    //上傳資料
                    upLoadedResponse = client.newCall(request).execute()
                    if (upLoadedResponse!!.isSuccessful) {//正確回來
                        upLoadedReselt = true
                        Log.e("正確回來!!", upLoadedResponse!!.body()!!.string())
                    } else {//錯誤回來
                        Log.e("錯誤回來!!", upLoadedResponse!!.body()!!.string())
                        upLoadedReselt = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("回來處理有錯!", e.toString())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return upLoadedReselt
    }

    //更改資料庫欄位UpLoaded
    private fun changeDBstatus(): Boolean {
        var dbChangStatus = java.lang.Boolean.parseBoolean(null)
        val changRealmStatus = Realm.getDefaultInstance()
        try {
            changRealmStatus.executeTransaction { _ ->
                Log.e("正確回來TRY", recordChangDataId.size.toString())
                for (i in 0 until recordChangDataId.size) {
                    //realm.beginTransaction();
                    val changDB = changRealmStatus.where(AsmDataModel::class.java)
                            .equalTo("id", recordChangDataId.get(i))
                            .findFirst()
                    changDB!!.setUpLoaded("1")
                    Log.e("回來更新", changDB!!.getDataId()!!.toString() + "更新?" + changDB!!.getUpLoaded())
                }
            }
            dbChangStatus = true
        } catch (e: Exception) {
            Log.e("dbChangStatus", e.toString())
            dbChangStatus = false
        }
        changRealmStatus.close()
        return dbChangStatus
    }
    */
}