package com.dzboot.ovpn.data.remote

import android.content.Context
import com.dzboot.ovpn.BuildConfig
import com.dzboot.ovpn.custom.BooleanTypeAdapter
import com.google.gson.GsonBuilder
import io.michaelrocks.paranoid.Obfuscate
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File


@Obfuscate
class NetworkCaller {

    companion object {

        //convenient constant to avoid syncing gradle on every change
        //const does not work with obfuscate
        private val LOCAL_BASE_URL = "http://192.168.1.100/"
        private val API = "api/v1/"
        val BASE_URL = if (isUsingLocalServer()) LOCAL_BASE_URL else BuildConfig.BASE_URL

        fun isUsingLocalServer() = BuildConfig.BASE_URL.isEmpty()

        // For Singleton instantiation
        @Volatile
        private var api: Api? = null

        fun getApi(context: Context): Api {
            return api ?: synchronized(this) {
                val logging = HttpLoggingInterceptor()
                logging.level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE

                val httpCacheDirectory = File(context.cacheDir, "http-cache")

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(CacheInterceptor())
                    .cache(Cache(httpCacheDirectory, 10 * 1024 * 1024L))
                    .build()

                val gsonBuilder = GsonBuilder()
                gsonBuilder.registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())

                Retrofit.Builder()
                    .baseUrl("$BASE_URL$API")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                    .client(client)
                    .build()
                    .create(Api::class.java)
                    .also { api = it }
            }
        }
    }
}