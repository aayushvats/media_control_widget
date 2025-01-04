package com.example.media_widget

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.media.session.MediaSessionManager
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.media.MediaMetadata
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

import android.os.Build
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.mediaWidget/media"
    private val PERMISSION_REQUEST_CODE = 123

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        requestPermissions()
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getMediaInfo" -> {
                    try {
                        val mediaInfo = getCurrentMediaInfo()
                        result.success(mediaInfo)
                    } catch (e: Exception) {
                        result.error("MEDIA_ERROR", "Failed to get media info", e.message)
                    }
                }
                "mediaAction" -> {
                    try {
                        val action = call.argument<String>("action")
                        if (action != null) {
                            handleMediaAction(action)
                            result.success(null)
                        } else {
                            result.error("INVALID_ARGUMENT", "Action cannot be null", null)
                        }
                    } catch (e: Exception) {
                        result.error("ACTION_ERROR", "Failed to perform media action", e.message)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun getCurrentMediaInfo(): Map<String, Any> {
        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, MediaControlWidgetProvider::class.java)

        try {
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            if (controllers.isNotEmpty()) {
                val controller = controllers[0]
                val metadata = controller.metadata
                val playbackState = controller.playbackState

                // Get thumbnail if available
                var thumbnailBase64 = ""
                try {
                    val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    if (artwork != null) {
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        artwork.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        thumbnailBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return mapOf(
                        "track" to (metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Track"),
                        "artist" to (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"),
                        "thumbnailUrl" to thumbnailBase64,
                        "isPlaying" to (playbackState?.state == PlaybackState.STATE_PLAYING)
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return mapOf(
                "track" to "No track playing",
                "artist" to "Unknown artist",
                "thumbnailUrl" to "",
                "isPlaying" to false
        )
    }

    private fun handleMediaAction(action: String) {
        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, MediaControlWidgetProvider::class.java)

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
            throw e
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_CODE
                )
            }
        }

        // Open notification listener settings
        startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}