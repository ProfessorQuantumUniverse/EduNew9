package com.quantumprof.edunew9.network

import com.google.gson.JsonObject
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface EdupageApiService {

    // NEU: Phase 1 - Die Login-Seite "besuchen", um ein Session-Cookie und den gsechash zu erhalten.
    @GET("/login/")
    suspend fun getLoginPage(): Response<ResponseBody>

    // Angepasst: Phase 2 - Der Login-POST, der jetzt den gsechash mitsendet.
    @FormUrlEncoded
    @POST("/login/edubarLogin.php")
    suspend fun login(
        @Field("username") user: String,
        @Field("password") pass: String,
        @Field("edupage") edupage: String = "edupage",
        @Field("gsechash") gsechash: String // Der Security-Hash ist jetzt ein Feld im POST-Body
    ): Response<ResponseBody>

    @GET("/substitution/server/getSubst.php")
    suspend fun getSubstitutionPlanForDate(
        @Query("date") date: String,
        @Query("gsechash") gsechash: String
    ): Response<ResponseBody>

    @GET("/rpr/server/maindbi.js?__func=mainDBIAccessor")
    suspend fun getMainDbi(): Response<ResponseBody>

    @POST("/rpr/server/maindbi.js?__func=mainDBIAccessor")
    suspend fun getMainDbiWithPost(@Body requestBody: RequestBody): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/rpr/server/maindbi.js?__func=mainDBIAccessor")
    suspend fun getMainDbiWithPost(
        @Field("__gsh") gsechash: String
    ): Response<ResponseBody>

    // 2. Holt die aktuelle Stundenplan-Versionsnummer (tt_num)
    @GET("/timetable/server/currenttt.js?__func=curentttGetData")
    suspend fun getCurrentTimetableData(): Response<ResponseBody>

    // 3. Holt die eigentlichen Stundenplan-Daten für eine Woche
    @GET("/timetable/server/ttviewer.js?__func=getTTViewerData")
    suspend fun getTimetableViewerData(
        @Query("date") date: String, // Format: YYYY-MM-DD
        @Query("tt_num") tt_num: Int,
        @Query("gsechash") gsechash: String
    ): Response<ResponseBody>
}