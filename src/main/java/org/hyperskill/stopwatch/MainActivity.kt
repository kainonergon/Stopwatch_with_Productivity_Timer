package org.hyperskill.stopwatch

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import org.hyperskill.stopwatch.databinding.ActivityMainBinding

const val CHANNEL_ID = "org.hyperskill"
const val NOTIFICATION_ID = 393939

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager
    private val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE)
    private var color = colors[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener(::startTimer)
        binding.resetButton.setOnClickListener(::resetTimer)
        binding.settingsButton.setOnClickListener(::settingsClick)
        binding.progressBar.visibility = View.INVISIBLE

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        registerNotificationChannel()
    }

    private var secondsPassed = 0
    private val timeString: String
        get() = "%02d:%02d".format(secondsPassed / 60, secondsPassed % 60)
    private var timerIsOn = false
    private var timeLimit: Int = Int.MAX_VALUE
    private var overtime = false

    private val handler = Handler(Looper.getMainLooper())

    private val timerTick: Runnable = object : Runnable {
        override fun run() {
            binding.textView.text = timeString
            color = colors[(colors.indexOf(color) + 1) % colors.size]
            binding.progressBar.indeterminateTintList = ColorStateList.valueOf(color)
            if (secondsPassed > timeLimit && !overtime) {
                binding.textView.setTextColor(Color.RED)
                overtime = true
                showNotification()
            }
            secondsPassed++
            handler.postDelayed(this, 1000)
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        binding.textView.text = timeString
    }

    private fun startTimer(view: View) {
        if (!timerIsOn) {
            timerIsOn = true
            handler.post(timerTick)
            binding.progressBar.visibility = View.VISIBLE
            binding.settingsButton.isEnabled = false
        }
    }

    private fun resetTimer(view: View) {
        timerIsOn = false
        binding.progressBar.visibility = View.INVISIBLE
        binding.settingsButton.isEnabled = true
        handler.removeCallbacks(timerTick)
        secondsPassed = 0
        binding.textView.text = timeString
        binding.textView.setTextColor(Color.BLACK)
        overtime = false
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun settingsClick(view: View) {
        val contentView = LayoutInflater.from(this).inflate(R.layout.settings_dialog, null, false)
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(contentView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val editText = contentView.findViewById<EditText>(R.id.upperLimitEditText)
                timeLimit = editText.text.toString().toIntOrNull() ?: Int.MAX_VALUE
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerTick)
    }

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pIntent)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}