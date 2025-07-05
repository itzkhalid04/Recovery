package com.datarescue.pro.di

import com.datarescue.pro.data.native.NativeFileScanner
import com.datarescue.pro.data.repository.AdvancedFileRecoveryRepository
import com.datarescue.pro.data.repository.DeviceInfoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNativeFileScanner(): NativeFileScanner {
        return NativeFileScanner()
    }
}