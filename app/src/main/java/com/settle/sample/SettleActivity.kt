package com.settle.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.settle.sample.chromeclient.ChromeClientActivity
import com.settle.sample.jsinterface.JSInterfaceActivity

class SettleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settle)

        initViews()
    }

    private fun initViews() {
        findViewById<Button>(R.id.btn_js_interface).setOnClickListener {
            startActivity(JSInterfaceActivity.newInstance(this))
            finish()
        }

        findViewById<Button>(R.id.btn_chrome_client).setOnClickListener {
            startActivity(ChromeClientActivity.newInstance(this))
            finish()
        }
    }
}