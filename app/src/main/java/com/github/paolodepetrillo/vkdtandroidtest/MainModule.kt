package com.github.paolodepetrillo.vkdtandroidtest

import android.content.Context
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtBase
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtLib
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MainModule {
    @Singleton
    @Provides
    fun provideVkdtBase(@ApplicationContext appContext: Context): VkdtBase {
        return VkdtBase(appContext)
    }

    @Singleton
    @Provides
    fun provideVkdtLib(vkdtBase: VkdtBase): VkdtLib {
        return VkdtLib(vkdtBase)
    }
}