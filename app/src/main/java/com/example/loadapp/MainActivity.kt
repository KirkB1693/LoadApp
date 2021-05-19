package com.example.loadapp

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.loadapp.constants.Constants.GLIDE_SIZE
import com.example.loadapp.constants.Constants.GLIDE_URL
import com.example.loadapp.constants.Constants.LOAD_APP_SIZE
import com.example.loadapp.constants.Constants.LOAD_APP_URL
import com.example.loadapp.constants.Constants.RETROFIT_SIZE
import com.example.loadapp.constants.Constants.RETROFIT_URL
import com.example.loadapp.databinding.ActivityMainBinding
import com.example.loadapp.utils.sendNotification
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager

    private var downloading = false
    private var downloadStatus: Int = 0
    private var downloadFilename = ""
    private lateinit var downloadManager: DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        Timber.plant(Timber.DebugTree())

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        custom_button.setOnClickListener {
            // Get the checked radio button id from radio group
            val id: Int = radioGroup.checkedRadioButtonId
            if (id != -1) {
                disableLoadingButton()
                // If any radio button checked from radio group
                // Get the instance of radio button using id
                val radio: RadioButton = findViewById(id)
                when (radio) {
                    radioButtonGlide -> applicationScope.launch {
                        downloadFilename = resources.getString(R.string.radio_button_glide_download)
                        download(GLIDE_URL, GLIDE_SIZE)
                    }
                    radioButtonLoadApp -> applicationScope.launch {
                        downloadFilename = resources.getString(R.string.radio_button_load_app_download)
                        download(LOAD_APP_URL, LOAD_APP_SIZE)
                    }
                    radioButtonRetrofit -> applicationScope.launch {
                        downloadFilename = resources.getString(R.string.radio_button_retrofit_download)
                        download(RETROFIT_URL, RETROFIT_SIZE)
                    }
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun disableLoadingButton() {
        custom_button.isEnabled = false
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent!!.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            displayNotification(id)
        }
    }

    private fun displayNotification(id: Long) {
        val query = DownloadManager.Query()
            .setFilterById(id)
        val cursor = downloadManager.query(query)
        var statusToDisplay = ""
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(
                cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            )
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    statusToDisplay = resources.getString(R.string.download_status_success)
                }
                DownloadManager.STATUS_FAILED -> {
                    statusToDisplay = resources.getString(R.string.download_status_failure)
                }
            }
        }

        notificationManager.sendNotification(resources.getString(R.string.notification_description),
            applicationContext,
            statusToDisplay,
            downloadFilename
        )
    }

    private fun download(url: String, bytesTotal: Long) {
        runOnUiThread { custom_button.setCustomButtonState(ButtonState.Clicked)
        progressBar.visibility = View.VISIBLE }
        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        val downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        downloading = true
        runOnUiThread { custom_button.setCustomButtonState(ButtonState.Loading) }

        Timber.i("Download starting...")
        var downloadProgress: Int
        var oldDownloadProgress = 0
        while (downloading) {
            Thread.sleep(500)
            val query = DownloadManager.Query().setFilterById(downloadID)

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
            checkDownloadStatus(cursor)
            cursor.close()
        }
    }

    private fun checkDownloadStatus(
        cursor: Cursor) {
        downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL ||
            downloadStatus == DownloadManager.STATUS_FAILED
        ) {
            downloading = false
            Thread.sleep(2500)
            runOnUiThread {
                enableLoadingButton()
                custom_button.setCustomButtonState(ButtonState.Completed)
                progressBar.visibility = View.GONE
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
            }
            DownloadManager.STATUS_PAUSED -> {
                val pausedReason = getPausedReason(reason)
                Timber.i("PAUSED: $pausedReason")
            }
            DownloadManager.STATUS_PENDING -> Timber.i("Pending")
            DownloadManager.STATUS_RUNNING -> Timber.i("Running")
            DownloadManager.STATUS_SUCCESSFUL -> Timber.i("Success")
        }
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
            DownloadManager.STATUS_FAILED -> statusMessage = resources.getString(R.string.download_status_failure)
            DownloadManager.STATUS_PAUSED -> statusMessage = "Download Paused"
            DownloadManager.STATUS_PENDING -> statusMessage = "Download Pending"
            DownloadManager.STATUS_RUNNING -> statusMessage = "Download Running"
            DownloadManager.STATUS_SUCCESSFUL -> statusMessage = resources.getString(R.string.download_status_success)
        }
        return statusMessage
    }

}
