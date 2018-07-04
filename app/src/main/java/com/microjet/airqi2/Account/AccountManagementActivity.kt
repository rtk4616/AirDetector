package com.microjet.airqi2.Account

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginResult
import com.microjet.airqi2.CustomAPI.GetNetWork
import com.microjet.airqi2.FirebaseNotifSettingTask
import com.microjet.airqi2.PrefObjects
import com.microjet.airqi2.R
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern






class AccountManagementActivity : AppCompatActivity() {

    private var mContext: Context? = null

    var userEmail = ""
    var userName = ""
    private var loginResult: String? = null
    private var mMyThing: logInMything? = null
    //2018/06/07 enable data upload dialog & use share preference
    private lateinit var myPref: PrefObjects

    val callbackManager = CallbackManager.Factory.create()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mContext = this@AccountManagementActivity.applicationContext
        initActionBar()

        val bundle = intent.extras
        if (bundle != null) {
            userEmail = bundle.getString("email", "")
            userName = bundle.getString("name", "")
            email.setText(userEmail)
            //mPasswordEditText?.setText(userPassword)
            rememberID.isChecked = true
            Log.e(this.javaClass.simpleName, userEmail + userName)
        }
        val share = getSharedPreferences("TOKEN", MODE_PRIVATE)
        email.setText(share.getString("LoginEmail",""))
        rememberID.isChecked = share.getBoolean("rememberID",false)

        // ****** Create Account Button ******************** //
        newAccount.setOnClickListener {
            if (isConnected()) {
                registerShow()
            } else {
                showDialog(getString(R.string.checkConnection))
            }
        }
        // ****** Forget Password Feature ********************//
        forgotPassword.setOnClickListener {
            if (isConnected()) {
                forgotPasswordShow()
            } else {
                showDialog(getString(R.string.checkConnection))
            }
        }

        mMyThing = logInMything(login!!, false, "https://mjairql.com/api/v1/login")

        login?.setOnClickListener {
            if (GetNetWork.isFastGetNet) {
                if (isEmail(email?.text.toString()) && email?.text.toString() != "") {
                    if (com.microjet.airqi2.CustomAPI.Utils.isFastDoubleClick) {
                        //showDialog("按慢一點太快了")
                        showDialog(getString(R.string.tooFast))
                    } else {
                        login?.isEnabled = false
                        goLoginAsyncTasks().execute(mMyThing)
                    }
                } else {
                    //showDialog("請輸入正確的E-mail地址")
                    showDialog(getString(R.string.errorMail_address))
                }
            }else{
                //showDialog("請連接網路")
                if (isConnected()) {
                    goLoginAsyncTasks().execute(mMyThing)
                } else {
                    showDialog(getString(R.string.checkConnection))
                }
            }
        }
        //2018/06/07 enable data upload dialog & use share preference
        myPref = PrefObjects(this)


        val callbackMannger = CallbackManager.Factory.create()
        login_buttonFB.setReadPermissions("email","public_profile")
        // Callback registration
        loginButtonFB()

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

    // 2018/03/30 Check network status

    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    // 2018/04/17 Add function for intent activity

    private fun forgotPasswordShow() {
        val intent = Intent(mContext, AccountForgetPasswordActivity::class.java)
        startActivity(intent)
    }

    // 2018/06/07 Add function for intent activity

    private fun AccountActivityShow() {
        val intent = Intent(mContext, AccountActiveActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun registerShow() {
        val i: Intent? = Intent(this, AccountRegisterActivity::class.java)
        startActivity(i)
    }

    private inner class goLoginAsyncTasks : AsyncTask<logInMything, Void, String>() {
        override fun doInBackground(vararg params: logInMything): String? {
            try {
                val response: okhttp3.Response?
                //val registerMail = user_register_mail?.text
                val client = OkHttpClient()
                val mediaType = MediaType.parse("application/x-www-form-urlencoded")

                userEmail = email!!.text.toString()
                userName = password!!.text.toString()
                val ccc = "email=" + userEmail + "&password=" + userName


                Log.e(this.javaClass.simpleName, "Email&Password"+ccc)
                val body = RequestBody.create(mediaType, ccc)// )
                val request = Request.Builder()
                        .url(params[0].myAddress)
                        .post(body)
                        .addHeader("cache-control", "no-cache")
                        .addHeader("content-type", "application/x-www-form-urlencoded")
                        .build()
                //上傳資料
                response = client.newCall(request).execute()
                val any = if (response.isSuccessful) {
                    runOnUiThread(java.lang.Runnable {
                        params[0].button!!.isEnabled = true
                    })
                    params[0].myBlean = false
                    try {
                        val tempBody: String = response.body()!!.string().toString()
                        Log.e("登入正確回來", tempBody)
                        val responseContent = JSONObject(tempBody)
                        val token = responseContent.getJSONObject("success").getString("token").toString()
                        val name = responseContent.getJSONObject("success").getJSONObject("userData").getString("name")
                        val email = responseContent.getJSONObject("success").getJSONObject("userData").getString("email")
                        // 2018/04/09
                        val share = getSharedPreferences("TOKEN", MODE_PRIVATE)
                        Log.e("登入正確回來拿token", token)
                        loginResult = "成功登入"
                        share.edit().putString("token", token).apply()
                        share.edit().putString("name", name).apply()
                        share.edit().putString("email", email).apply()
                        val deviceArr: JSONArray = responseContent.getJSONObject("success").getJSONArray("deviceList")
                        share.edit().putString("deviceLi", deviceArr.toString()).apply()
                        Log.d("DDDD",deviceArr.toString())


                        // ****** 2018/04/10 Remember ID *******************************************************//
                        if (rememberID.isChecked) {
                            share.edit().putBoolean("rememberID", true).apply()
                            share.edit().putString("LoginEmail", email).apply()
                        }else{
                            share.edit().putBoolean("rememberID", false).apply()
                            share.edit().putString("LoginEmail", "").apply()
                        }

                        share.edit().putString("LoginPassword", password!!.text.toString()).apply()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else {
                    runOnUiThread(java.lang.Runnable {
                        params[0].button!!.isEnabled = true
                        login?.isEnabled=true
                    })
                    params[0].myBlean = false
                    Log.e("登入錯誤回來", response.body()!!.string())
                    loginResult = "登入失敗"
                }
            }
            catch (e: Exception) {
                when (e) {
                    is JSONException ->{
                        e.printStackTrace()
                    }
                    is IOException -> {
                        e.printStackTrace()
                    }
                }
            }
            return loginResult
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result == "成功登入") {
                val shareToken = getSharedPreferences("TOKEN", Context.MODE_PRIVATE)
                val myToken = shareToken.getString("token", "")
                showEnableUploadDialog()
                FirebaseNotifSettingTask().execute(myToken)
            }else{
                val Dialog = android.app.AlertDialog.Builder(this@AccountManagementActivity).create()
                //必須是android.app.AlertDialog.Builder 否則alertDialog.show()會報錯
                //Dialog.setTitle("提示")
                Dialog.setTitle(getString(R.string.remind))
                Dialog.setMessage(result.toString())
                Dialog.setCancelable(false)//讓返回鍵與空白無效
                //Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "确定")
                Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.confirm))
                { dialog, _ ->
                    dialog.dismiss()
                }
                Dialog.show()
                //finish()
            }
        }
    }


    //20180311
    private fun isEmail(strEmail: String?): Boolean {
        val strPattern = ("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")
        val p = Pattern.compile(strPattern)
        val m = p.matcher(strEmail)
        return m.matches()
    }


    //20180312
    private fun showDialog(msg: String){
        val Dialog = android.app.AlertDialog.Builder(this@AccountManagementActivity).create()
        //必須是android.app.AlertDialog.Builder 否則alertDialog.show()會報錯
        //Dialog.setTitle("提示")
        Dialog.setTitle(getString(R.string.remind))
        Dialog.setMessage(msg.toString())
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        //Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "确定")
        Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.confirm))
        { dialog, _ ->
            dialog.dismiss()
            //finish()
        }
        Dialog.show()
    }

    // 2018/06/07 enable data upload dialog & use share preference
    private fun showEnableUploadDialog() {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        Dialog.setTitle(getString(R.string.text_UploadDialog_Title))
        Dialog.setMessage(getString(R.string.text_UploadDialog))
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        //Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "确定")

        Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.text_close))//否
        { dialog, _ ->
            myPref.setSharePreferenceCloudUploadStat(false)
            showUploadCloudClose()
            dialog.dismiss()
        }
        Dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.text_open))//是
        { dialog, _ ->
            myPref.setSharePreferenceCloudUploadStat(true)
            showUploadCloudOpen()
            dialog.dismiss()
        }
        Dialog.show()
    }

    private fun showUploadCloudOpen() {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        Dialog.setTitle(getString(R.string.allow_3G))
        Dialog.setMessage(getString(R.string.text_Enable3GDialog))
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        //Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "确定")

        Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.text_close))//否
        { dialog, _ ->
            myPref.setSharePreferenceCloudUpload3GStat(false)
            dialog.dismiss()
            AccountActivityShow()
        }
        Dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.text_open))//是
        { dialog, _ ->
            myPref.setSharePreferenceCloudUpload3GStat(true)
            dialog.dismiss()
            AccountActivityShow()
        }
        Dialog.show()
    }

    private fun showUploadCloudClose() {
        val Dialog = android.app.AlertDialog.Builder(this).create()
        Dialog.setTitle("")
        Dialog.setMessage(getString(R.string.text_CloseUploadDialog))
        Dialog.setCancelable(false)//讓返回鍵與空白無效
        //Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "确定")

        Dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.text_gotIt))
        { dialog, _ ->
            dialog.dismiss()
            AccountActivityShow()
        }
        Dialog.show()
    }

    private fun loginButtonFB() {
        login_buttonFB.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                // App code
                val userId = loginResult.accessToken.userId
                val graphRequest = GraphRequest.newMeRequest(loginResult.accessToken) { `object`, response -> displayUserInfo(`object`) }
                val parameters = Bundle()
                parameters.putString("fields", "first_name, last_name, email, id")
                graphRequest.parameters = parameters
                graphRequest.executeAsync()
                Log.e("FB_Token",loginResult.accessToken.token)
            }

            override fun onCancel() {
                // App code
            }

            override fun onError(exception: FacebookException) {
                // App code
            }
        })
    }

    fun displayUserInfo(`object`: JSONObject) {
        var first_name = ""
        var last_name = ""
        var email = ""
        var id = ""
        try {
            first_name = `object`.getString("first_name")
            last_name = `object`.getString("last_name")
            email = `object`.getString("email")
            id = `object`.getString("id")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Log.e("testFBandFUCK", first_name + "_" + last_name + "_" + email + "_" + id)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

}
class logInMything ( btn:Button?, blean:Boolean?, myString :String?){
    var button = btn
    var myBlean = blean
    var myAddress = myString
}
