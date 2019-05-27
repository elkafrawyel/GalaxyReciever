package com.galaxyreciever.app

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WikiApiService {

    @GET("acitve.php")
    fun active(@Query("uid") action: String): Call<CodeResponse>

}