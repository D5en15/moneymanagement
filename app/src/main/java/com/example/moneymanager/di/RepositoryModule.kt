package com.example.moneymanager.di

import com.example.moneymanager.data.repository.MoneyRepositoryImpl
import com.example.moneymanager.domain.repository.MoneyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMoneyRepository(
        moneyRepositoryImpl: MoneyRepositoryImpl
    ): MoneyRepository
}
