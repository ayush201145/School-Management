package com.schoolmgmt.app.di

import com.schoolmgmt.app.BuildConfig
import com.schoolmgmt.app.data.remote.AuthApi
import com.schoolmgmt.app.data.remote.AuthInterceptor
import com.schoolmgmt.app.data.remote.FeeApi
import com.schoolmgmt.app.data.remote.ItemApi
import com.schoolmgmt.app.data.remote.StudentApi
import com.schoolmgmt.app.data.remote.SyncApi
import com.schoolmgmt.app.data.remote.TeacherApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()
    // NOTE: no KotlinJsonAdapterFactory needed here — every DTO uses
    // @JsonClass(generateAdapter = true) (codegen via KSP, see
    // moshi-kotlin-codegen in build.gradle.kts), which generates a
    // dedicated adapter per class at compile time. Adding the
    // reflection-based factory on top would be redundant and reintroduce
    // exactly the ProGuard/R8 fragility we deliberately moved away from.

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BODY logging is verbose but very useful while wiring up
            // new endpoints; BuildConfig.DEBUG ensures it's stripped from
            // release builds so request/response bodies (which include
            // JWTs and student PII) never end up in production logs.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideStudentApi(retrofit: Retrofit): StudentApi = retrofit.create(StudentApi::class.java)

    @Provides
    @Singleton
    fun provideTeacherApi(retrofit: Retrofit): TeacherApi = retrofit.create(TeacherApi::class.java)

    @Provides
    @Singleton
    fun provideFeeApi(retrofit: Retrofit): FeeApi = retrofit.create(FeeApi::class.java)

    @Provides
    @Singleton
    fun provideItemApi(retrofit: Retrofit): ItemApi = retrofit.create(ItemApi::class.java)

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi = retrofit.create(SyncApi::class.java)
}
