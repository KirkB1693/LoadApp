package com.example.loadapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.loadapp.constants.Constants.DOWNLOAD_FILENAME
import com.example.loadapp.constants.Constants.DOWNLOAD_STATUS
import com.example.loadapp.constants.Constants.NOTIFICATION_ID
import com.example.loadapp.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.detailToolbar)

        if (intent != null) {
            val status = intent.getStringExtra(DOWNLOAD_STATUS)
            val filename = intent.getStringExtra(DOWNLOAD_FILENAME)
            val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
            NotificationManagerCompat.from(application).cancel(notificationId)

            binding.includeDetailContent.filenameTextview.text = filename
            binding.includeDetailContent.statusTextview.text = status
            if (status.equals(resources.getString(R.string.download_status_failure))) {
                binding.includeDetailContent.statusTextview.setTextColor(ContextCompat.getColor(this, R.color.red))
            } else {
                binding.includeDetailContent.statusTextview.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            }

        }


    }

    fun returnToMainActivity(view: View) {
        val returnToMainIntent = Intent(applicationContext, MainActivity::class.java)
        startActivity(returnToMainIntent)
    }

}
