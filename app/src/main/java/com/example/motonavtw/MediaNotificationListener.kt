package com.example.motonavtw

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.content.Context
import android.util.Log

class MediaNotificationListener : NotificationListenerService() {

    companion object {
        var currentSongTitle: String = "未播放"
            private set
        var currentArtist: String = ""
            private set
        var currentAppSource: String = "未選擇" // 顯示當前播放來源 (如 Spotify, 番茄小說, 抖音)
            private set
        var onSongChangedListener: ((String, String, String) -> Unit)? = null

        private var activeController: MediaController? = null

        fun skipToPrevious() {
            activeController?.transportControls?.skipToPrevious()
        }

        fun togglePlayPause() {
            val state = activeController?.playbackState?.state
            if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                activeController?.transportControls?.pause()
            } else {
                activeController?.transportControls?.play()
            }
        }

        fun skipToNext() {
            activeController?.transportControls?.skipToNext()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateActiveController()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateActiveController()

        sbn?.let {
            val packageName = it.packageName.lowercase()

            // 擴充支援：音樂、有聲書(番茄小說)、短影音(抖音/TikTok)等
            if (packageName.contains("spotify") ||
                packageName.contains("youtube") ||
                packageName.contains("music") ||
                packageName.contains("dragonfly") ||
                packageName.contains("novel") ||
                packageName.contains("reader") ||
                packageName.contains("zhiliao") || // 抖音海外/部分版本
                packageName.contains("toutiao") ||
                packageName.contains("ss.android.ugc.aweme") || // 抖音包名
                packageName.contains("reader.comic") ||
                packageName.contains("tomato") // 番茄小說英文包名常見字眼
            ) {
                val extras = it.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                // 簡短標示來源名稱
                currentAppSource = when {
                    packageName.contains("spotify") -> "Spotify"
                    packageName.contains("youtube") -> "YouTube"
                    packageName.contains("tomato") || packageName.contains("novel") -> "番茄小說"
                    packageName.contains("aweme") || packageName.contains("douyin") -> "抖音"
                    else -> "多媒體"
                }

                if (title.isNotEmpty()) {
                    currentSongTitle = title
                    currentArtist = artist
                    onSongChangedListener?.invoke(currentAppSource, title, artist)
                }
            }
        }
    }

    private fun updateActiveController() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = mediaSessionManager.getActiveSessions(android.content.ComponentName(this, this::class.java))
            if (!controllers.isNullOrEmpty()) {
                // 優先選擇正在播放的那個控制器
                activeController = controllers.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
                    ?: controllers[0]
            }
        } catch (e: Exception) {
            Log.e("MotoNavTW", "取得 MediaController 失敗: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}