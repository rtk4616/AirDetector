package com.microjet.airqi2

import android.os.AsyncTask
import android.util.Log
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Created by B00190 on 2018/5/22. by 白~~~~~~~~~~~~~~~~~~~~~~~~~~告
 */
class ScrollingTextTask : AsyncTask<String, Int, Void>() {

    val TAG = "ScrollingTextTask"

    //主要背景執行
    override fun doInBackground(vararg params: String?): Void? {
        val client = OkHttpClient()
        val urlBuilder = HttpUrl.parse("https://www.addwii.com/api/get_recent_posts/")!!.newBuilder()
        val url = urlBuilder.build().toString()
        val request = Request.Builder()
                .url(url)
                .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "ERROR")
            } else {
                val res = response.body()?.string()
                Log.e(TAG, res)
                val jsonObj = JSONObject(res)
                //取出posts內容
                val resultArray = jsonObj.getJSONArray("posts")

                for (i in 0 until resultArray.length()) {
                    val jsonObjScrolling = resultArray.getJSONObject(i)
                    val hashMap = HashMap<String, String>()

                    val jsonObjLongTitle = jsonObjScrolling.optString("title")
                    val jsonObjURL = jsonObjScrolling.optString("url")
                    val jsonObjShortTitle = jsonObjScrolling
                            .getJSONObject("custom_fields")
                            .optJSONArray("tm_posts_short_description")

                    when (jsonObjLongTitle != null && jsonObjURL != null) {
                        true -> {
                            hashMap["url"] = jsonObjScrolling.optString("url")
                            if (jsonObjShortTitle != null) {
                                hashMap["title"] = jsonObjShortTitle.getString(0)
                            } else {
                                hashMap["title"] = jsonObjScrolling["title"].toString()
                            }
                        }
                        false ->{
                            hashMap["url"] = "https://www.addwii.com/issues/"
                            hashMap["title"] = "ADDWII5重要議題都在這裡，歡迎點擊!!"
                        }
                    }
                    TvocNoseData.scrollingList.add(hashMap)
                }
                Log.e(TAG, TvocNoseData.scrollingList.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}