package com.galaxyreciever.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.blankj.utilcode.util.DeviceUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.video.VideoListener
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_connection.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

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

const val RC_WRITE_EXTERNAL_STORAGE = 1
class ConnectionActivity : AppCompatActivity(), Player.EventListener, VideoListener {

    private lateinit var viewModel: LauncherViewModel

    lateinit var tvRef: DatabaseReference
    lateinit var valueListener: ValueEventListener
    var clicked: Boolean = true
    var firstLoop: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel = ViewModelProviders.of(this).get(LauncherViewModel::class.java)
        viewModel.liveData.observe(this, Observer<LauncherState?> { state ->
            when {
                state!!.showUpdateDialog!! -> {
                    updateView.visibility = View.VISIBLE
                    progressText.visibility = View.VISIBLE
                    progressText.text = "Outdated version installed.\nA newer version is available. You have to download and install it before continue to use app."
                    downloadApk()
                }
                state.isDownloading!! -> {
                    updateView.visibility = View.VISIBLE
                    progressText.visibility = View.VISIBLE
                    progressText.text = "Downloading Update " + state.progress.toString() + "%"
                }
                state.onSuccess!! -> installApk(state.updateFile!!)
                state.onError!! -> {

                }
                state.startApp!! -> updateView.visibility = View.GONE
            }
        })


//        val rotate = RotateAnimation(
//            30f,
//            360f,
//            Animation.RELATIVE_TO_SELF,
//            0.5f,
//            Animation.RELATIVE_TO_SELF,
//            0.5f
//        )
//        rotate.duration = 2500
//        logo.startAnimation(rotate)
//

        val mClockHandler = Handler(GalaxyApplication.instance.mainLooper)
        object : Runnable {
            override fun run() {
                mClockHandler.postDelayed(this, 60000)
                if (!firstLoop) {
                    if (clicked && splashView.visibility == View.GONE) {
                        manageSplashWindow()
                    }
                } else {
                    firstLoop = false
                }
            }
        }.run()

        splashView.setOnClickListener {
            splashView.visibility = View.GONE
            clicked = true
        }
    }

    private fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val data = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
            grantUriPermission(this.packageName, data, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(data, "application/vnd.android.package-archive")

                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
            finish()

        } else {
            val uri = Uri.parse("file://" + file.getAbsolutePath())
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.setDataAndType(uri, UpdateUtility().APK_MIME_TYPE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
            exitProcess(0)
        }
    }

    @AfterPermissionGranted(RC_WRITE_EXTERNAL_STORAGE)
    private fun downloadApk() {
        val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            viewModel.download()
        } else {
            EasyPermissions.requestPermissions(
                this, "",
                RC_WRITE_EXTERNAL_STORAGE, *perms
            )
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (splashView.visibility == View.VISIBLE) {
            splashView.visibility = View.GONE
            clicked = true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun manageSplashWindow() {
        splashView.visibility = View.VISIBLE
        clicked = false
        val mClockHandler = Handler(GalaxyApplication.instance.mainLooper)
        object : Runnable {
            override fun run() {
                val dateFormatter = SimpleDateFormat("dd MMMM yyyy")
                val timeFormatter = SimpleDateFormat("HH:mm")
                val date = Date(System.currentTimeMillis())
                val dateString = dateFormatter.format(date)
                val timeString = timeFormatter.format(date)
                timeTv.text = timeString
                dateTv.text = dateString

                mClockHandler.postDelayed(this, 1000)
            }
        }.run()

        val images: ArrayList<Int> = arrayListOf()
        var index = 0
        images.add(R.drawable.bg1)
        images.add(R.drawable.bg2)
        images.add(R.drawable.bg3)
        images.add(R.drawable.bg4)
        val mImagesHandler = Handler(GalaxyApplication.instance.mainLooper)
        object : Runnable {
            override fun run() {
                Glide.with(this@ConnectionActivity).load(images[index]).into(background)
                index++
                if (index >= 4)
                    index = 0

                mImagesHandler.postDelayed(this, 20000)
            }
        }.run()
    }

    override fun onResume() {
        super.onResume()

        var code: String? = null

        val id = DeviceUtils.getAndroidID()
        val call = GalaxyApplication.create().active(id)

        call.enqueue(object : Callback<CodeResponse?> {
            override fun onFailure(call: Call<CodeResponse?>, t: Throwable) {

            }

            override fun onResponse(call: Call<CodeResponse?>, response: Response<CodeResponse?>) {
                val result = response.body()!!

                if (result.status == "success") {
                    code = result.code
                    Glide.with(this@ConnectionActivity).load(result.code_image).into(barCodeImage)
                } else {
                    codeTv.text = result.status
                }

                if (code != null) {
                    GalaxyApplication.getPreferenceHelper().code = code
                    codeTv.text = code

                    tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code!!)

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
                                    toast("Connected!!, Waiting For Actions.")
                                }
                            }
                        }
                    }

                    tvRef.addValueEventListener(valueListener)
                }

            }
        })
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}



























