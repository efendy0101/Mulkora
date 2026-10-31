package com.mulkora.feature.contracts.di

import com.mulkora.feature.contracts.data.repository.ContractRepositoryImpl
import com.mulkora.feature.contracts.domain.repository.ContractRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI modülü — ContractRepository arayüzünü gerçek implementasyona bağlar.
 *
 * FirebaseFirestore, FirebaseFunctions ve FirebaseAuth bağımlılıkları
 * core-firebase modülündeki FirebaseModule tarafından sağlanır.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ContractModule {

    @Binds
    @Singleton
    abstract fun bindContractRepository(
        impl: ContractRepositoryImpl,
    ): ContractRepository
}