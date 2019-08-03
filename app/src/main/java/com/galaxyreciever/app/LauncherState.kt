package com.galaxyreciever.app

import java.io.File

class LauncherState {
    var showUpdateDialog: Boolean? = false
    var startApp: Boolean? = false
    var isDownloading: Boolean? = false
    var progress = 0
    var onSuccess: Boolean? = false
    var updateFile: File? = null
    var onError: Boolean? = false
}