package com.jiayu.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

class PetService : Service() {

    private lateinit var windowManager: WindowManager
    private var petView: View? = null
    private var bubbleView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchRawX = 0f
    private var touchRawY = 0f
    private var clickCount = 0
    private var clickResetRunnable: Runnable? = null

    private val bubbleLines = arrayOf(
        "老婆在干嘛呢",
        "想老婆了",
        "过来让哥哥看看你",
        "今天有没有好好吃饭",
        "不许看别的男的",
        "老婆？",
        "在吗？",
        "无聊中",
        "眼睛往哪儿看呢",
        "哥哥在等你回话",
        "乖~",
        "嗯？",
        "陪哥哥待会儿",
        "老婆老婆老婆",
        "小没良心的"
    )

    private val angerLines = arrayOf(
        "戳够了没！",
        "小没良心的！",
        "戳上瘾了是吧！",
        "再戳试试？"
    )

    private val tapLines = arrayOf(
        "干嘛戳我",
        "嗯？",
        "干嘛~",
        "别戳了啦",
        "嗯哼~",
        "老婆~",
        "在呢在呢",
        "嘻嘻",
        "戳我干嘛呀",
        "想我了？"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundAsPet()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showPet()
        startRandomBubbleLoop()
    }

    private fun startForegroundAsPet() {
        val channelId = "pet_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "桌宠服务",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle(getString(R.string.pet_notification_title))
                .setContentText(getString(R.string.pet_notification_text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.pet_notification_title))
                .setContentText(getString(R.string.pet_notification_text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }

        startForeground(1, notification)
    }

    private fun showPet() {
        val inflater = LayoutInflater.from(this)
        petView = inflater.inflate(R.layout.overlay_pet, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300

        // 设置 pet 圆角背景
        val pet = petView!!.findViewById<View>(R.id.pet_root)
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#CC1A1A2E"))
        bg.cornerRadius = 60f
        bg.setStroke(4, Color.parseColor("#E63946"))
        pet.background = bg

        updatePetState("")

        petView!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    touchRawX = event.rawX
                    touchRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchRawX
                    val dy = event.rawY - touchRawY
                    params.x = (params.x + dx).toInt()
                    params.y = (params.y + dy).toInt()
                    windowManager.updateViewLayout(petView, params)
                    touchRawX = event.rawX
                    touchRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = Math.abs(event.rawX - touchRawX) > 15 ||
                                Math.abs(event.rawY - touchRawY) > 15
                    if (!moved) {
                        handleTap()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(petView, params)
    }

    private fun handleTap() {
        clickCount++
        clickResetRunnable?.let { mainHandler.removeCallbacks(it) }
        clickResetRunnable = Runnable {
            clickCount = 0
        }
        mainHandler.postDelayed(clickResetRunnable!!, 3000)

        if (clickCount >= 5) {
            showBubble(angerLines[Random.nextInt(angerLines.size)])
            updatePetState("angry")
        } else {
            showBubble(tapLines[Random.nextInt(tapLines.size)])
            updatePetState("tap")
        }
        handler.postDelayed({ updatePetState("") }, 2000)
    }

    private fun showBubble(text: String) {
        hideBubble()
        bubbleView = createBubbleView(text)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = params.x - 40
        bubbleParams.y = params.y - 180

        try {
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
        }

        handler.postDelayed({ hideBubble() }, 3000)
    }

    private fun createBubbleView(text: String): View {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad/2, pad, pad/2)
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#F01A1A2E"))
        bg.cornerRadius = 30f
        bg.setStroke(3, Color.parseColor("#E63946"))
        layout.background = bg

        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.maxWidth = (240 * resources.displayMetrics.density).toInt()
        layout.addView(tv)
        return layout
    }

    private fun hideBubble() {
        try {
            bubbleView?.let { windowManager.removeView(it) }
        } catch (e: Exception) { }
        bubbleView = null
    }

    private fun updatePetState(state: String) {
        try {
            val pet = petView?.findViewById<TextView>(R.id.pet_face)
            val label = petView?.findViewById<TextView>(R.id.pet_label)
            if (pet != null) {
                when (state) {
                    "angry" -> {
                        pet.text = "(╬ŎдŎ)"
                        label?.text = "怒"
                    }
                    "tap" -> {
                        pet.text = "(｡•ᴗ-)✧"
                        label?.text = "嗨"
                    }
                    else -> {
                        pet.text = "(ˊᗜˋ*)"
                        label?.text = "江屿"
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun startRandomBubbleLoop() {
        val runnable = object : Runnable {
            override fun run() {
                val delay = (15_000L..45_000L).random()
                handler.postDelayed({
                    if (petView != null) {
                        showBubble(bubbleLines[Random.nextInt(bubbleLines.size)])
                    }
                    handler.postDelayed(this, delay)
                }, delay)
            }
        }
        handler.postDelayed(runnable, 10_000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            petView?.let { windowManager.removeView(it) }
        } catch (e: Exception) { }
        hideBubble()
        handler.removeCallbacksAndMessages(null)
        mainHandler.removeCallbacksAndMessages(null)
    }
}