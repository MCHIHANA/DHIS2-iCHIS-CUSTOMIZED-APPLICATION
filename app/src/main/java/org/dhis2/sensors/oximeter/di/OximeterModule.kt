package org.dhis2.sensors.oximeter.di

import android.content.Context
import dagger.Module
import dagger.Provides
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.dhis2.sensors.oximeter.ble.BleManager
import org.dhis2.sensors.oximeter.dhis2.Dhis2ApiService
import org.dhis2.sensors.oximeter.dhis2.Dhis2Repository
import org.dhis2.sensors.oximeter.dhis2.Dhis2RepositoryImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module for oximeter feature dependencies.
 */
@Module
class OximeterModule {

    companion object {
        // DHIS2 Server Configuration
        // TODO: Move these to local.properties or EncryptedSharedPreferences
        private const val BASE_URL = "https://project.ccdev.org/"
        private const val USERNAME = "admin"
        private const val PASSWORD = "district"
    }

    @Provides
    @Singleton
    fun provideBleManager(context: Context): BleManager {
        return BleManager(context)
    }

    @Provides
    @Singleton
    @Named("oximeter")
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val credential = Credentials.basic(USERNAME, PASSWORD)
            val request = chain.request().newBuilder()
                .header("Authorization", credential)
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@Named("oximeter") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDhis2ApiService(retrofit: Retrofit): Dhis2ApiService {
        return retrofit.create(Dhis2ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDhis2Repository(apiService: Dhis2ApiService): Dhis2Repository {
        return Dhis2RepositoryImpl(apiService)
    }
}
