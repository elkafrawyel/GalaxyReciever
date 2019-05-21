package com.galaxyreciever.app

import android.content.Context
import android.preference.PreferenceManager

class PreferencesHelper(context: Context) {
    companion object {
        private const val CODE = "code"
        private const val ACTIVE_CODE = "active_code"
    }

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    var code = preference.getString(CODE, null)
        set(value) = preference.edit().putString(CODE, value).apply()

    var active_code = preference.getString(ACTIVE_CODE, null)
        set(value) = preference.edit().putString(ACTIVE_CODE, value).apply()

}