package com.example.android_stopwatch

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.android_stopwatch.databinding.ActivityMainBinding

const val CHANNEL_ID = "org.example"
const val NOTIFICATION_ID = 393939

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager
    private val colors = arrayOf(Color.RED, Color.GREEN, Color.BLUE)
    private var color = colors[0]

    private var secondsPassed = 0
    private val timeString: String
        get() = "%02d:%02d".format(
            secondsPassed / 60,
            secondsPassed % 60
        )
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

    /**
     * When the app is first opened, the onCreate method is called. This method is used to
     * initialize the app. In this case, we are setting up the UI, and setting up the
     * notification channel.
     * @param savedInstanceState The saved state of the app, if any
     * @see getSystemService For more information on getting the notification manager
     */
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

    /**
     * This method is called when the app is opened from the notification. It is used to set the
     * text of the timer to the correct value
     */
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        binding.textView.text = timeString
    }

    /**
     * Start the timer when the start button is clicked.
     * @param view The view that was clicked
     */
    private fun startTimer(view: View) {
        if (!timerIsOn) {
            timerIsOn = true
            handler.post(timerTick)
            binding.progressBar.visibility = View.VISIBLE
            binding.settingsButton.isEnabled = false
        }
    }

    /**
     * Reset the timer when the reset button is clicked.
     * @param view The view that was clicked
     */
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

    /**
     * Show the settings dialog to set the limit for the timer.
     * @param view The view that was clicked
     */
    private fun settingsClick(view: View) {
        val contentView = LayoutInflater.from(this)
            .inflate(R.layout.settings_dialog, null, false)
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

    /**
     * When the app is closed, the onDestroy method is called. This method is used to remove the
     * timer callback, so that the timer stops running when the app is closed.
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerTick)
    }

    /**
     * Create the notification channel for the app. This is required for Android 8.0 and above.
     * @see NotificationChannel For more information on notification channels
     */
    private fun registerNotificationChannel() {
        val name = getString(R.string.notification_channel)
        val descriptionText = getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Show a notification when the timer reaches the time limit.
     * @see NotificationCompat.Builder For more information on building notifications
     */
    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pIntent)
            .setOnlyAlertOnce(true)
        // There should be no notification sent if the time limit is negative
        if (timeLimit > 0) {
            val notification = notificationBuilder.build()
            notification.flags = Notification.FLAG_INSISTENT or Notification.FLAG_ONLY_ALERT_ONCE
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}