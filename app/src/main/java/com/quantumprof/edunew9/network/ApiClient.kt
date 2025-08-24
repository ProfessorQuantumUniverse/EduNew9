package com.quantumprof.edunew9.network

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://waldorfschule-frankfurt.edupage.org"

    // CookieManager speichert die Cookies (unsere Login-Session) automatisch.
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }
    private val cookieJar: CookieJar = object : CookieJar {
        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
            cookieManager.cookieStore.add(url.toUri(),
                java.net.HttpCookie(cookies.firstOrNull()?.name ?: "", cookies.firstOrNull()?.value ?: ""))
        }

        override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
            val cookies = cookieManager.cookieStore.get(url.toUri())
            return cookies.map {
                okhttp3.Cookie.Builder()
                    .name(it.name)
                    .value(it.value)
                    .domain(url.host)
                    .build()
            }
        }
    }

    // Logging Interceptor, um die Netzwerkkommunikation zu sehen.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // *** DAS IST DIE NEUE, ENTSCHEIDENDE ÄNDERUNG ***
    // Ein Interceptor, der bei JEDER Anfrage den Referer-Header hinzufügt.
    private val refererInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithReferer = originalRequest.newBuilder()
            // Wir tun so, als käme jede Anfrage von der Hauptseite.
            // Das ist der Schlüssel, um als "legitimer" Client zu gelten.
            .header("Referer", "$BASE_URL/user/")
            .build()
        chain.proceed(requestWithReferer)
    }

    // Der OkHttpClient, der jetzt ALLE unsere Helferlein verwendet.
    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(refererInterceptor) // <-- Hier wird unser neuer Interceptor hinzugefügt
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit-Instanz, die den präparierten HttpClient nutzt.
    val instance: EdupageApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(EdupageApiService::class.java)
    }
}