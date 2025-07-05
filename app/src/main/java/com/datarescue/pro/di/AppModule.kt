package com.datarescue.pro.di

import com.datarescue.pro.data.repository.FileRecoveryRepositoryImpl
import com.datarescue.pro.domain.repository.FileRecoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindFileRecoveryRepository(
        fileRecoveryRepositoryImpl: FileRecoveryRepositoryImpl
    ): FileRecoveryRepository
}