package com.example.loadapp

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.loadapp.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    private var downloadID: Long = 0

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        Timber.plant(Timber.DebugTree())

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))


        custom_button.setOnClickListener {
            // Get the checked radio button id from radio group
            var id: Int = radioGroup.checkedRadioButtonId
            if (id != -1) {
                disableLoadingButton()
                // If any radio button checked from radio group
                // Get the instance of radio button using id
                val radio: RadioButton = findViewById(id)
                when (radio) {
                    radioButtonGlide -> applicationScope.launch {download(GLIDE_URL, GLIDE_SIZE)}
                    radioButtonLoadApp -> applicationScope.launch {download(LOAD_APP_URL, LOAD_APP_SIZE)}
                    radioButtonRetrofit -> applicationScope.launch {download(RETROFIT_URL, RETROFIT_SIZE)}
                    else -> Toast.makeText(
                        applicationContext, "Unknown Radio Button Selected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // If no radio button checked in this radio group
                Toast.makeText(
                    applicationContext, resources.getText(R.string.radio_button_not_selected_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    private fun disableLoadingButton() {
        custom_button.isEnabled = false
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        }
    }

    private fun download(url: String, bytesTotal: Long) {
        custom_button.setCustomButtonState(ButtonState.Clicked)
        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        var downloading = true
        runOnUiThread { custom_button.setCustomButtonState(ButtonState.Loading) }

        Timber.i("Download starting...")
        var downloadProgress: Int
        var oldDownloadProgress = 0
        while (downloading) {
            Thread.sleep(500)
            val query = DownloadManager.Query()

            val cursor = downloadManager.query(query)

            cursor.moveToFirst()
            val bytesDownloaded =
                cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))

            downloadProgress = if (bytesTotal != 0L) {
                ((bytesDownloaded * 100L) / bytesTotal).toInt()
            } else {
                100
            }
            if (downloadProgress > oldDownloadProgress) {
                if (downloadProgress > 100) {
                    downloadProgress = 100
                }
                runOnUiThread { custom_button.setLoadingPercentage(downloadProgress / 100f) }
                oldDownloadProgress = downloadProgress
            }
            Timber.i("Download at $downloadProgress percent")
            downloading = checkDownloadStatus(cursor, downloading)
            cursor.close()
        }
    }

    private fun checkDownloadStatus(
        cursor: Cursor,
        downloading: Boolean
    ): Boolean {
        var downloading1 = downloading
        val downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL ||
            downloadStatus == DownloadManager.STATUS_FAILED
        ) {
            downloading1 = false
            Thread.sleep(2500)
            runOnUiThread {
                enableLoadingButton()
                custom_button.setCustomButtonState(ButtonState.Completed)
            }
        }
        val statusMessage = getDownloadStatus(downloadStatus)
        Timber.i("Download status is $statusMessage")
        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reason = cursor.getInt(columnReason)

        when (downloadStatus) {
            DownloadManager.STATUS_FAILED -> {
                val failedReason = getFailedReason(reason)
                Timber.i("FAILED: $failedReason")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "FAILED",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            DownloadManager.STATUS_PAUSED -> {
                val pausedReason = getPausedReason(reason)
                Timber.i("PAUSED: $pausedReason")
            }
            DownloadManager.STATUS_PENDING -> Timber.i("Pending")
            DownloadManager.STATUS_RUNNING -> Timber.i("Running")
            DownloadManager.STATUS_SUCCESSFUL -> {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "SUCCESSFUL",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return downloading1
    }

    private fun getPausedReason(reason: Int): String {
        var pausedReason = ""
        when (reason) {
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> pausedReason =
                "PAUSED_QUEUED_FOR_WIFI"
            DownloadManager.PAUSED_UNKNOWN -> pausedReason = "PAUSED_UNKNOWN"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> pausedReason =
                "PAUSED_WAITING_FOR_NETWORK"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> pausedReason =
                "PAUSED_WAITING_TO_RETRY"
        }
        return pausedReason
    }

    private fun getFailedReason(reason: Int): String {
        var failedReason = ""
        when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> failedReason = "ERROR_CANNOT_RESUME"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> failedReason =
                "ERROR_DEVICE_NOT_FOUND"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> failedReason =
                "ERROR_FILE_ALREADY_EXISTS"
            DownloadManager.ERROR_FILE_ERROR -> failedReason = "ERROR_FILE_ERROR"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> failedReason =
                "ERROR_HTTP_DATA_ERROR"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> failedReason =
                "ERROR_INSUFFICIENT_SPACE"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> failedReason =
                "ERROR_TOO_MANY_REDIRECTS"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> failedReason =
                "ERROR_UNHANDLED_HTTP_CODE"
            DownloadManager.ERROR_UNKNOWN -> failedReason = "ERROR_UNKNOWN"
        }
        return failedReason
    }

    private fun enableLoadingButton() {
        custom_button.isEnabled = true
    }

    private fun getDownloadStatus(status: Int): String {
        var statusMessage = ""
        when (status) {
            DownloadManager.STATUS_FAILED -> statusMessage = "Download Failed"
            DownloadManager.STATUS_PAUSED -> statusMessage = "Download Paused"
            DownloadManager.STATUS_PENDING -> statusMessage = "Download Pending"
            DownloadManager.STATUS_RUNNING -> statusMessage = "Download Running"
            DownloadManager.STATUS_SUCCESSFUL -> statusMessage = "Download Successful"
        }
        return statusMessage
    }

    companion object {
        private const val GLIDE_URL =
            "https://github.com/bumptech/glide/archive/refs/heads/master.zip"
        private const val GLIDE_SIZE = 93218406L
        private const val LOAD_APP_URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val LOAD_APP_SIZE = 147456L
        private const val RETROFIT_URL =
            "https://github.com/square/retrofit/archive/refs/heads/master.zip"
        private const val RETROFIT_SIZE = 5641338L
        private const val CHANNEL_ID = "channelId"
    }

}
