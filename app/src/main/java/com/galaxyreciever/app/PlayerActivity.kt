package com.galaxyreciever.app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import at.huber.youtubeExtractor.Format
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_connection.parentView
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity(), Player.EventListener, VideoListener {

    var BANDWIDTH = DefaultBandwidthMeter()
    var player: SimpleExoPlayer? = null
    lateinit var tvRef: DatabaseReference
    var positionBeforePause: Long? = null
    var mUrl: String? = null
    lateinit var valueListener: ValueEventListener
    lateinit var audioManager: AudioManager
    private var youtubeLinks = arrayListOf<String>()
    var listPosition: Int? = 0
    var playType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        initiatePlayer()

        val code = GalaxyApplication.getPreferenceHelper().code

        tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code!!)

        valueListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.i("MyApp", "onCancelled")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                receivedActions(dataSnapshot)
            }
        }

        tvRef.addValueEventListener(valueListener)
    }

    override fun onStart() {
        hideSystemUI()
        super.onStart()
    }


    @SuppressLint("InlinedApi")
    private fun hideSystemUI() {
        parentView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private fun receivedActions(dataSnapshot: DataSnapshot) {
        if (dataSnapshot.exists()) {

            val connectedValue = dataSnapshot.child(CONNECTED).value?.toString()

            if (connectedValue != null && connectedValue == "0") {
                finish()
            }

            playType = dataSnapshot.child(PLAY_TYPE).value?.toString()
            if (playType == "single") {
                val urlValue = dataSnapshot.child(URL).value?.toString()

                if (urlValue != null && urlValue.isNotEmpty()) {
                    val playValue = dataSnapshot.child(PLAY).value?.toString()

                    if (playValue != null && playValue == "1") {
                        //   play("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        //   play("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                        //there was running video before not first time
                        //but same video
                        if (mUrl == urlValue) {
                            if (player?.playWhenReady != true) {
                                if (positionBeforePause != null) {
                                    player?.playWhenReady = true
                                    player?.seekTo(positionBeforePause!!)
                                } else {
                                    mUrl = urlValue
                                    play(mUrl!!)
                                }
                            }
                        } else {
                            //url changed
                            positionBeforePause = null
                            mUrl = urlValue
                            play(mUrl!!)
                        }

                    } else if (playValue != null && playValue == "0") {
                        pause()
                    } else {

                    }

                    val seekUpValue = dataSnapshot.child(SEEK_UP).value?.toString()
                    val seekDownValue = dataSnapshot.child(SEEK_DOWN).value?.toString()
                    val seekValue = dataSnapshot.child(SEEK).value?.toString()

                    if (player != null) {
                        if (seekValue != null && seekValue != "") {
                            player?.seekTo(seekValue.toLong())
                            tvRef.child(SEEK).setValue("")
                        }

                        if (seekUpValue != null && seekUpValue != "") {
                            player?.seekTo(player?.currentPosition!!.plus(10000))
                            tvRef.child(SEEK_UP).setValue("")
                        }

                        if (seekDownValue != null && seekDownValue != "") {
                            player?.seekTo(player?.currentPosition!!.minus(10000))
                            tvRef.child(SEEK_DOWN).setValue("")
                        }
                    }

                    val soundUpValue = dataSnapshot.child(SOUND_UP).value?.toString()
                    val soundDownValue = dataSnapshot.child(Sound_Down).value?.toString()


                    if (player != null) {

                        if (soundDownValue != null && soundDownValue != "") {
                            //Down
                            tvRef.child(Sound_Down).setValue("")
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_SAME,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }

                        if (soundUpValue != null && soundUpValue != "") {
                            //Up
                            tvRef.child(SOUND_UP).setValue("")
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_SAME,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                    }
                }
            } else {
                listPosition = dataSnapshot.child(LIST_POSITION).value?.toString()?.toInt()
                val playValue = dataSnapshot.child(PLAY).value?.toString()
                val seekUpValue = dataSnapshot.child(SEEK_UP).value?.toString()
                val seekDownValue = dataSnapshot.child(SEEK_DOWN).value?.toString()
                val seekValue = dataSnapshot.child(SEEK).value?.toString()
                val soundUpValue = dataSnapshot.child(SOUND_UP).value?.toString()
                val soundDownValue = dataSnapshot.child(Sound_Down).value?.toString()
                var url: String? = null
                @SuppressLint("StaticFieldLeak") val mExtractor =
                    object : YouTubeExtractor(this@PlayerActivity) {
                        override fun onExtractionComplete(
                            sparseArray: SparseArray<YtFile>?,
                            videoMeta: VideoMeta
                        ) {

                        }
                    }


                if (youtubeLinks.size == 0) {
                    dataSnapshot.child(URL_LIST).ref.addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(dataSnapshot1: DatabaseError) {

                        }

                        override fun onDataChange(dataSnapshot1: DataSnapshot) {
                            dataSnapshot.child(URL_LIST).ref.removeEventListener(this)
                            youtubeLinks.clear()
                            dataSnapshot1.children.forEach {
                                youtubeLinks.add(it.value as String)
                            }



                            mExtractor.extract(youtubeLinks[listPosition!!], true, true)
                            val sparseArray = mExtractor.get()

                            if (sparseArray != null) {
                                // Initialize an array of colors
                                val item = sparseArray.valueAt(0) as YtFile
                                url = item.url


                                if (playValue != null && playValue == "1") {
                                    if (mUrl.equals(url)) {
                                        if (player?.playWhenReady != true) {
                                            if (positionBeforePause != null) {
                                                player?.playWhenReady = true
                                                player?.seekTo(positionBeforePause!!)
                                            } else {
                                                mUrl = url
                                                play(mUrl!!)
                                            }
                                        }
                                    } else {
                                        //url changed
                                        positionBeforePause = null
                                        mUrl = url
                                        play(mUrl!!)
                                    }


                                } else if (playValue != null && playValue == "0") {
                                    pause()
                                } else {

                                }

                                if (player != null) {
                                    if (seekValue != null && seekValue != "") {
                                        player?.seekTo(seekValue.toLong())
                                        tvRef.child(SEEK).setValue("")
                                    }

                                    if (seekUpValue != null && seekUpValue != "") {
                                        player?.seekTo(player?.currentPosition!!.plus(10000))
                                        tvRef.child(SEEK_UP).setValue("")
                                    }

                                    if (seekDownValue != null && seekDownValue != "") {
                                        player?.seekTo(player?.currentPosition!!.minus(10000))
                                        tvRef.child(SEEK_DOWN).setValue("")
                                    }
                                }



                                if (player != null) {

                                    if (soundDownValue != null && soundDownValue != "") {
                                        //Down
                                        tvRef.child(Sound_Down).setValue("")
                                        audioManager.adjustVolume(
                                            AudioManager.ADJUST_LOWER,
                                            AudioManager.FLAG_PLAY_SOUND
                                        )
                                        audioManager.adjustStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            AudioManager.ADJUST_SAME,
                                            AudioManager.FLAG_SHOW_UI
                                        );
                                    }

                                    if (soundUpValue != null && soundUpValue != "") {
                                        //Up
                                        tvRef.child(SOUND_UP).setValue("")
                                        audioManager.adjustVolume(
                                            AudioManager.ADJUST_RAISE,
                                            AudioManager.FLAG_PLAY_SOUND
                                        )
                                        audioManager.adjustStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            AudioManager.ADJUST_SAME,
                                            AudioManager.FLAG_SHOW_UI
                                        );
                                    }
                                }
                            } else {
                                toast("Error Casting this content")
                            }
                        }
                    })


                } else {
                    if (playValue != null && playValue == "1") {
                        if (player?.playWhenReady != true) {
                            if (positionBeforePause != null) {
                                player?.playWhenReady = true
                                player?.seekTo(positionBeforePause!!)
                            }
                        }
                    } else if (playValue != null && playValue == "0") {
                        pause()
                    } else {

                    }
                    if (player != null) {
                        if (seekValue != null && seekValue != "") {
                            player?.seekTo(seekValue.toLong())
                            tvRef.child(SEEK).setValue("")
                        }

                        if (seekUpValue != null && seekUpValue != "") {
                            player?.seekTo(player?.currentPosition!!.plus(10000))
                            tvRef.child(SEEK_UP).setValue("")
                        }

                        if (seekDownValue != null && seekDownValue != "") {
                            player?.seekTo(player?.currentPosition!!.minus(10000))
                            tvRef.child(SEEK_DOWN).setValue("")
                        }
                    }



                    if (player != null) {

                        if (soundDownValue != null && soundDownValue != "") {
                            //Down
                            tvRef.child(Sound_Down).setValue("")
                            audioManager.adjustVolume(
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_PLAY_SOUND
                            )
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_SAME,
                                AudioManager.FLAG_SHOW_UI
                            );
                        }

                        if (soundUpValue != null && soundUpValue != "") {
                            //Up
                            tvRef.child(SOUND_UP).setValue("")
                            audioManager.adjustVolume(
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_PLAY_SOUND
                            )
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_SAME,
                                AudioManager.FLAG_SHOW_UI
                            );
                        }
                    }
                }
            }
        }
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tvRef.removeEventListener(valueListener)
        release()
    }

    private fun initiatePlayer() {

        Log.i(TAG, "PLAYER INITIALIZED")

        val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()

        player = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(
                this,
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            ),
            DefaultTrackSelector(adaptiveTrackSelectionFactory)
        )

        video_view.useController = true

        if (video_view.player == null)
            video_view.player = player

        player?.addListener(this)

        player?.addVideoListener(this)

    }

    fun play(url: String) {

        val videoUri = Uri.parse(url)

        Log.i(TAG, "Url to play : $videoUri")

        val mediaSource = buildMediaSource(videoUri)

        player?.prepare(mediaSource)

        player?.playWhenReady = true

        if (positionBeforePause != null) {
            player?.seekTo(positionBeforePause!!)
        }
    }

    override fun onPause() {
        super.onPause()
        release()
    }

    fun pause() {
        positionBeforePause = player?.currentPosition!!
        player?.playWhenReady = false
    }

    private fun release() {
        if (player != null) {
            player?.removeListener(this)
            player?.removeVideoListener(this)
            player?.release()
            player = null
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val defaultExtractorsFactory = DefaultExtractorsFactory()
        defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
        defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)

        return ExtractorMediaSource.Factory {
            DefaultDataSourceFactory(
                this@PlayerActivity,
                BANDWIDTH,
                DefaultHttpDataSourceFactory(
                    Util.getUserAgent(this@PlayerActivity, getString(R.string.app_name)),
                    BANDWIDTH
                )
            ).createDataSource()
        }.setExtractorsFactory(defaultExtractorsFactory)
            .createMediaSource(uri)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {
            }
            Player.STATE_BUFFERING -> {
            }
            Player.STATE_READY -> {
            }
            Player.STATE_ENDED -> {
//                tvRef.child(PLAY).setValue(STOP_ACTION)
//                tvRef.child(URL).setValue("")
//                tvRef.child(SEEK).setValue("")
//                tvRef.child(SEEK_UP).setValue("")
//                tvRef.child(SEEK_DOWN).setValue("")
//                tvRef.child(CONNECTED).setValue(DISCONNECTED_ACTION)

                if (playType == "single") {
                    finish()
                } else {
                    listPosition = listPosition!! + 1
                    if (listPosition!! < youtubeLinks.size) {
                        tvRef.child(LIST_POSITION).setValue(listPosition)
                        positionBeforePause = null
                        saveVideoImageToFireBase(youtubeLinks[listPosition!!])
                        extractYoutubeUrl(youtubeLinks[listPosition!!])
                    } else {
                        finish()
                    }
                }
            }
            else -> {
            }
        }
    }

    private fun saveVideoImageToFireBase(videoUrl: String) {
//        val link = "https://www.youtube.com/watch?v=SAk0ky6V5eY"
        val videoId = videoUrl.replace("https://www.youtube.com/watch?v=", "")
        val imageUrl = getVideoImage(videoId)
        tvRef.child(VIDEO_IMAGE).setValue(imageUrl)
    }

    private fun getVideoImage(videoId: String): String {
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }

    private fun extractYoutubeUrl(mYoutubeLink: String) {
        @SuppressLint("StaticFieldLeak") val mExtractor = object : YouTubeExtractor(this) {
            override fun onExtractionComplete(sparseArray: SparseArray<YtFile>?, videoMeta: VideoMeta) {
                if (sparseArray != null) {
                    // Initialize an array of colors

                    val item = sparseArray.valueAt(0) as YtFile
                    val format = item.format as Format
                    val url = item.url

                    play(url)
                }
            }
        }
        mExtractor.extract(mYoutubeLink, true, true)
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        toast(error?.message!!)
    }

}
