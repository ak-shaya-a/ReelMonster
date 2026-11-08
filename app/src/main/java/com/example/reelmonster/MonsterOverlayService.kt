package com.example.reelmonster

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat

class MonsterOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var reelCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        startForegroundServiceNotification()
        loadCount()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_monster_bubble, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 300

        windowManager.addView(floatingView, layoutParams)

        val monsterIcon = floatingView?.findViewById<View>(R.id.monster_bubble)
        val tvCount = floatingView?.findViewById<TextView>(R.id.tv_badge_count)
        val tvSpeech = floatingView?.findViewById<TextView>(R.id.tv_speech_bubble)

        updateUI(tvCount, tvSpeech)

        monsterIcon?.setOnClickListener {
            reelCount++
            saveCount()
            updateUI(tvCount, tvSpeech)
        }

        monsterIcon?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun updateUI(tvCount: TextView?, tvSpeech: TextView?) {
        tvCount?.text = "$reelCount"
        tvSpeech?.text = getMonsterSpeech(reelCount)
        
        val scale = (1.0f + (reelCount / 100.0f).coerceAtMost(0.5f))
        floatingView?.findViewById<View>(R.id.monster_face)?.scaleX = scale
        floatingView?.findViewById<View>(R.id.monster_face)?.scaleY = scale
    }

    private fun getMonsterSpeech(count: Int): String {
        return when {
            count == 0 -> "Tap me +1"
            count < 5 -> "Yum! Keep scrolling..."
            count < 15 -> "Tap me! Another reel eaten!"
            count < 30 -> "Burp! Your homework says hi."
            else -> "I'm huge now! Stop scrolling!"
        }
    }

    private fun loadCount() {
        val sharedPref = getSharedPreferences("ReelMonsterPrefs", Context.MODE_PRIVATE)
        reelCount = sharedPref.getInt("today_count", 0)
    }

    private fun saveCount() {
        val sharedPref = getSharedPreferences("ReelMonsterPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("today_count", reelCount)
            apply()
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "reel_monster_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reel Monster Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Reel Monster Active")
            .setContentText("Monster is floating on top of your apps!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }
    }
}