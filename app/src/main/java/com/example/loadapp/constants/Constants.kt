package com.example.loadapp.constants

object Constants {
    const val CHANNEL_ID = "channelId"
    const val CHANNEL = "Downloads"
    const val DOWNLOAD_STATUS = "downloadStatus"
    const val DOWNLOAD_FILENAME = "downloadFilename"
    // Notification ID.
    const val NOTIFICATION_ID = "notificationId"
    const val GLIDE_URL =
        "https://github.com/bumptech/glide/archive/refs/heads/master.zip"
    // ideally the file size would be obtained dynamically, but I was unable to get this information from github so I have set the file sizes as constants
    const val GLIDE_SIZE = 93218406L
    const val LOAD_APP_URL =
        "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
    const val LOAD_APP_SIZE = 147456L
    const val RETROFIT_URL =
        "https://github.com/square/retrofit/archive/refs/heads/master.zip"
    const val RETROFIT_SIZE = 5641338L
}