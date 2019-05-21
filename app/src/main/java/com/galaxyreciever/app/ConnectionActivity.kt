package com.galaxyreciever.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.video.VideoListener
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_connection.*

const val PLAY = "play"
const val URL = "url"
const val VIDEO_IMAGE = "videoImage"
const val SEEK_UP = "seekUp"
const val SEEK_DOWN = "seekDOWN"
const val SOUND_UP = "soundUp"
const val Sound_Down = "soundDOWN"
const val SEEK = "seek"
const val CONNECTED = "connected"
const val PLAY_TYPE = "playType"
const val URL_LIST = "urlList"
const val LIST_POSITION = "listPosition"
const val SEEKUP = "seekUp"
const val SEEKDOWN = "seekDOWN"
const val STOP_ACTION = ""
const val DISCONNECTED_ACTION = "0"

const val TAG = "GalaxyApp"

class ConnectionActivity : AppCompatActivity(), Player.EventListener, VideoListener {

    lateinit var tvRef: DatabaseReference
    lateinit var valueListener: ValueEventListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        val rotate = RotateAnimation(
            30f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotate.duration = 2500
        logo.startAnimation(rotate)
    }

    override fun onResume() {
        super.onResume()
        val code = "Amh245tfD464"
        GalaxyApplication.getPreferenceHelper().code = code
        codeTv.text = code

        tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code)

        tvRef.child(PLAY).setValue(STOP_ACTION)
        tvRef.child(PLAY_TYPE).setValue(null)
        tvRef.child(VIDEO_IMAGE).setValue(null)
        tvRef.child(URL).setValue(null)
        tvRef.child(URL_LIST).setValue(null)
        tvRef.child(LIST_POSITION).setValue(null)
        tvRef.child(SEEK).setValue("")
        tvRef.child(SEEK_UP).setValue("")
        tvRef.child(SEEK_DOWN).setValue("")
        tvRef.child(SOUND_UP).setValue("")
        tvRef.child(Sound_Down).setValue("")
        tvRef.child(CONNECTED).setValue(DISCONNECTED_ACTION)

        valueListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.i("MyApp", "onCancelled")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val connectedValue = dataSnapshot.child(CONNECTED).value?.toString()
                    if (connectedValue != null && connectedValue == "1") {
                        tvRef.removeEventListener(this)
                        startActivity(Intent(this@ConnectionActivity, PlayerActivity::class.java))
                        toast("Connected to mobile Waiting for actions")
                    }
                }
            }
        }

        tvRef.addValueEventListener(valueListener)
    }

    override fun onStart() {
        hideSystemUI()
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        tvRef.removeValue()
    }

    fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
}



























