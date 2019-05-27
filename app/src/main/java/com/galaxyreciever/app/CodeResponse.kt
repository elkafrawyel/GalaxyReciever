package com.galaxyreciever.app
import com.squareup.moshi.Json


data class CodeResponse(
    @field:Json(name = "status")
    val status: String?,
    @field:Json(name = "code")
    val code: String?,
    @field:Json(name = "codeimg")
    val code_image: String?
)