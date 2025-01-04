package com.example.media_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.net.Uri
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.content.ComponentName
import android.media.session.PlaybackState
import android.graphics.BitmapFactory
import android.util.Base64

class MediaControlWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            val track = HomeWidget.getWidgetData(context, "track") ?: "No track playing"
            val artist = HomeWidget.getWidgetData(context, "artist") ?: "Unknown artist"
            val thumbnailBase64 = HomeWidget.getWidgetData(context, "thumbnail") ?: ""

            val thumbnail = if (thumbnailBase64.isNotEmpty()) {
                Base64.decode(thumbnailBase64, Base64.DEFAULT)
            } else null

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setTextViewText(R.id.track_title, track)
                setTextViewText(R.id.artist_name, artist)
                thumbnail?.let {
                    setImageViewBitmap(R.id.thumbnail, BitmapFactory.decodeByteArray(it, 0, it.size))
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }


    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Set up click listeners for media controls
        setControlButtons(context, views, appWidgetId)

        // Update UI with current track info
        try {
            // Get SharedPreferences instance
            val sharedPreferences = context.getSharedPreferences("your_shared_prefs_name", Context.MODE_PRIVATE)

            // Fetch data from SharedPreferences
            val track = sharedPreferences.getString("track", "No track") ?: "No track"
            val artist = sharedPreferences.getString("artist", "Unknown artist") ?: "Unknown artist"
            val thumbnailBase64 = sharedPreferences.getString("thumbnail", null)
            val isPlaying = sharedPreferences.getBoolean("isPlaying", false)

            // Set text data
            views.setTextViewText(R.id.track_title, track)
            views.setTextViewText(R.id.artist_name, artist)

            // Update play/pause button image
            views.setImageViewResource(
                    R.id.button_play_pause,
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )

            // Update thumbnail if available
            if (!thumbnailBase64.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(thumbnailBase64.replace("\\s".toRegex(), ""), Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    views.setImageViewBitmap(R.id.thumbnail, bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setControlButtons(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(
                R.id.button_previous,
                getPendingSelfIntent(context, "previous", appWidgetId)
        )
        views.setOnClickPendingIntent(
                R.id.button_play_pause,
                getPendingSelfIntent(context, "playPause", appWidgetId)
        )
        views.setOnClickPendingIntent(
                R.id.button_next,
                getPendingSelfIntent(context, "next", appWidgetId)
        )
    }

    private fun getPendingSelfIntent(
            context: Context,
            action: String,
            appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        intent.data = Uri.parse("homewidget://media_action?action=$action&widgetId=$appWidgetId")
        return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            "previous" -> sendMediaCommand(context, "previous")
            "playPause" -> sendMediaCommand(context, "playPause")
            "next" -> sendMediaCommand(context, "next")
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun sendMediaCommand(context: Context, action: String) {
        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, this.javaClass)

        try {
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            if (controllers.isNotEmpty()) {
                val mediaController = controllers[0]
                when (action) {
                    "previous" -> mediaController.transportControls.skipToPrevious()
                    "playPause" -> {
                        if (mediaController.playbackState?.state == PlaybackState.STATE_PLAYING) {
                            mediaController.transportControls.pause()
                        } else {
                            mediaController.transportControls.play()
                        }
                    }
                    "next" -> mediaController.transportControls.skipToNext()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}