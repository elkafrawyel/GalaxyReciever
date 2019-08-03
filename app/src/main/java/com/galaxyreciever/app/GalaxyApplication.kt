package com.galaxyreciever.app

import android.app.Application
import com.blankj.utilcode.util.Utils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

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


        private fun getLoggingInterceptor(): HttpLoggingInterceptor {
            return HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        }

        private fun getOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .readTimeout(60,TimeUnit.SECONDS)
                .writeTimeout(60,TimeUnit.SECONDS)
                .addInterceptor(getLoggingInterceptor())
                .build()
        }

        fun create(): WikiApiService {

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl("https://www.galaxygcast.com/")
                .client(getOkHttpClient())
                .build()

            return retrofit.create(WikiApiService::class.java)
        }

    }
}