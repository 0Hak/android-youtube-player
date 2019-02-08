package com.pierfrancescosoffritti.cyplayersample.examples.notificationExample

import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.cast.framework.CastContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerInitListener
import com.pierfrancescosoffritti.androidyoutubeplayer.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.cyplayersample.R
import com.pierfrancescosoffritti.cyplayersample.notifications.NotificationManager
import com.pierfrancescosoffritti.cyplayersample.notifications.PlaybackControllerBroadcastReceiver
import com.pierfrancescosoffritti.cyplayersample.utils.MediaRouteButtonUtils
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.utils.PlayServicesUtils
import com.pierfrancescosoffritti.cyplayersample.utils.VideoIdsProvider
import kotlinx.android.synthetic.main.activity_basic_example.*

/**
 * Simple example showing how to build a notification to control the cast player.
 * In a real application both notification and playback should be managed in a service.
 */
class NotificationExampleActivity : AppCompatActivity() {

    private val googlePlayServicesAvailabilityRequestCode = 1
    private val playbackControllerBroadcastReceiver = PlaybackControllerBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_example)

        registerBroadcastReceiver()

        MediaRouteButtonUtils.initMediaRouteButton(media_route_button)

        // can't use CastContext until I'm sure the user has GooglePlayServices
        PlayServicesUtils.checkGooglePlayServicesAvailability(this, googlePlayServicesAvailabilityRequestCode) { initChromecast() }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(playbackControllerBroadcastReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // can't use CastContext until I'm sure the user has GooglePlayServices
        if(requestCode == googlePlayServicesAvailabilityRequestCode)
            PlayServicesUtils.checkGooglePlayServicesAvailability(this, googlePlayServicesAvailabilityRequestCode) {initChromecast()}
    }

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter(PlaybackControllerBroadcastReceiver.TOGGLE_PLAYBACK)
        filter.addAction(PlaybackControllerBroadcastReceiver.STOP_CAST_SESSION)
        applicationContext.registerReceiver(playbackControllerBroadcastReceiver, filter)
    }

    private fun initChromecast() {
        val chromecastConnectionListener = SimpleChromecastConnectionListener()
        ChromecastYouTubePlayerContext(CastContext.getSharedInstance(this).sessionManager, chromecastConnectionListener, playbackControllerBroadcastReceiver)
    }

    inner class SimpleChromecastConnectionListener: ChromecastConnectionListener {

        private val notificationManager = NotificationManager(applicationContext, NotificationExampleActivity::class.java)

        override fun onChromecastConnecting() {
            Log.d(javaClass.simpleName, "onChromecastConnecting")
        }

        override fun onChromecastConnected(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
            Log.d(javaClass.simpleName, "onChromecastConnected")

            initializeCastPlayer(chromecastYouTubePlayerContext)
            notificationManager.showNotification()
        }

        override fun onChromecastDisconnected() {
            Log.d(javaClass.simpleName, "onChromecastDisconnected")
            notificationManager.dismissNotification()
        }

        private fun initializeCastPlayer(chromecastYouTubePlayerContext: ChromecastYouTubePlayerContext) {
            chromecastYouTubePlayerContext.initialize( YouTubePlayerInitListener { youtubePlayer ->

                val playerStateTracker = YouTubePlayerTracker()

                initBroadcastReceiver(youtubePlayer, playerStateTracker)

                youtubePlayer.addListener(notificationManager)
                youtubePlayer.addListener(playerStateTracker)

                youtubePlayer.addListener(object: AbstractYouTubePlayerListener() {
                    override fun onReady() {
                        youtubePlayer.loadVideo(VideoIdsProvider.getNextVideoId(), 0f)
                    }
                })
            })
        }

        private fun initBroadcastReceiver(youTubePlayer: YouTubePlayer, playerTracker: YouTubePlayerTracker) {
            playbackControllerBroadcastReceiver.togglePlayback = {
                if(playerTracker.state == PlayerConstants.PlayerState.PLAYING)
                    youTubePlayer.pause()
                else
                    youTubePlayer.play()
            }
        }
    }
}
