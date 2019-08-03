package com.galaxyreciever.app

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.jetbrains.anko.toast

const val REMOTE_CONFIG_VERSION_CODE = "code"

class LauncherViewModel : ViewModel() {

    private var mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private var mStorageRef: StorageReference? = null

    var liveData = MutableLiveData<LauncherState>()

    init {
        val remoteConfigCacheExpirationSeconds = 60L * 60L
        mFirebaseRemoteConfig.fetch(remoteConfigCacheExpirationSeconds)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mFirebaseRemoteConfig.activate()
                    checkUpdate()
                }
            }
    }

    private fun checkUpdate() {
        val versionCode = mFirebaseRemoteConfig.getLong(REMOTE_CONFIG_VERSION_CODE)
//        GalaxyApplication.instance.toast(versionCode.toString())
        if (versionCode > BuildConfig.VERSION_CODE) {
            val state = LauncherState()
            state.showUpdateDialog = true
            liveData.value = state
        } else {
            val state = LauncherState()
            state.startApp = true
            liveData.setValue(state)
        }

//        val state = LauncherState()
//        state.showUpdateDialog = true
//        liveData.value = state
    }

    internal fun download() {
        val state = LauncherState()
        state.isDownloading = true
        liveData.value = state

        downloadUpdate()
    }

    private fun downloadUpdate() {
        val storageReference = FirebaseStorage.getInstance().reference
        mStorageRef = storageReference.child("tv.apk")
        mStorageRef!!.metadata.addOnCompleteListener {
            val metadata = it.result
            if (metadata != null) {
                val fileSize = metadata.sizeBytes
                val updateFile = UpdateUtility().getUpdateFile(fileSize)
                if (updateFile != null) {
                    mStorageRef!!.getFile(updateFile).addOnProgressListener { taskSnapshot ->
                        val progress =
                            taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        val progressPercentage = (progress * 100).toInt()
                        if (progressPercentage > 0) {
                            val state = LauncherState()
                            state.isDownloading = true
                            state.progress = progressPercentage
                            liveData.value = state
                        }
                    }.addOnSuccessListener {
                        // Local temp file has been created
                        Log.i("Update Progress: ", "success")
                        Log.i("Update Progress: ", updateFile.absolutePath)
                        val state = LauncherState()
                        state.onSuccess = true
                        state.updateFile = updateFile
                        liveData.setValue(state)
                    }.addOnFailureListener {
                        // Handle any errors
                        Log.i("Update Progress: ", "error")
                        val state = LauncherState()
                        state.onError = true
                        state.updateFile = updateFile
                        liveData.setValue(state)
                    }
                }
            }
        }

    }
}


