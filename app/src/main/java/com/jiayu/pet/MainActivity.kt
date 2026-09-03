package com.jiayu.pet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    private val OVERLAY_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPermission = findViewById<Button>(R.id.btn_permission)
        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnStop = findViewById<Button>(R.id.btn_stop)

        btnPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_REQUEST)
            } else {
                Toast.makeText(this, "权限已开启", Toast.LENGTH_SHORT).show()
            }
        }

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "先申请悬浮窗权限", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, PetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "桌宠已启动", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnStop.setOnClickListener {
            val intent = Intent(this, PetService::class.java)
            stopService(intent)
            Toast.makeText(this, "桌宠已关闭", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "权限已开启", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未授权", Toast.LENGTH_SHORT).show()
            }
        }
    }
}