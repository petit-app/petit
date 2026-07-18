package com.woliveiras.petit.di

import com.woliveiras.petit.data.lan.LanSyncController
import com.woliveiras.petit.data.lan.LanSyncCoordinator
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.FamilyGroupRepositoryImpl
import com.woliveiras.petit.data.repository.NearbyTransferRepository
import com.woliveiras.petit.data.repository.NearbyTransferRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that binds family group repository interfaces to implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FamilyGroupModule {

  @Binds
  @Singleton
  abstract fun bindFamilyGroupRepository(
    familyGroupRepositoryImpl: FamilyGroupRepositoryImpl
  ): FamilyGroupRepository

  @Binds
  @Singleton
  abstract fun bindNearbyTransferRepository(
    nearbyTransferRepositoryImpl: NearbyTransferRepositoryImpl
  ): NearbyTransferRepository

  @Binds
  @Singleton
  abstract fun bindLanSyncController(coordinator: LanSyncCoordinator): LanSyncController
}
