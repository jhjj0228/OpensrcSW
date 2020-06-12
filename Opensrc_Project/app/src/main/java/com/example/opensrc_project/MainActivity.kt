package com.example.opensrc_project

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle

import android.view.View
import android.widget.Toast


class MainActivity : Activity() {
    var isUnityLoaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        setIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 1) isUnityLoaded = false
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null || intent.extras == null) return
        if (intent.extras!!.containsKey("setColor")) {
            val v = findViewById<View>(R.id.button3)
            when (intent.extras!!.getString("setColor")) {
                "yellow" -> v.setBackgroundColor(Color.YELLOW)
                "red" -> v.setBackgroundColor(Color.RED)
                "blue" -> v.setBackgroundColor(Color.BLUE)
                else -> {
                }
            }
        }
    }


    fun showToast(message: String) = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onBackPressed() {
        finishAffinity()
    }


   fun btnShowOpencvTest(view: View?) {
       val intent = Intent(this, OpencvTestActivity::class.java)
       startActivity(intent)
   }



}