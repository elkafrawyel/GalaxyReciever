package com.galaxyreciever.app

import android.app.Application
import com.blankj.utilcode.util.Utils

class GalaxyApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        instance = this
        Utils.init(this)
    }


    companion object {
        lateinit var instance: GalaxyApplication
            private set


        fun getPreferenceHelper() = PreferencesHelper(instance)

    }
}