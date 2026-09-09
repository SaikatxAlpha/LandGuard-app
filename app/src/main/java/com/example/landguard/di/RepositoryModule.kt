package com.example.landguard.di

import com.example.landguard.data.repository.AlertRepository
import com.example.landguard.data.repository.AlertRepositoryImpl
import com.example.landguard.data.repository.ParcelRepository
import com.example.landguard.data.repository.ParcelRepositoryImpl
import com.example.landguard.data.repository.ProfileRepository
import com.example.landguard.data.repository.ProfileRepositoryImpl
import com.example.landguard.data.repository.ReportRepository
import com.example.landguard.data.repository.ReportRepositoryImpl
import com.example.landguard.data.repository.SatelliteRepository
import com.example.landguard.data.repository.SatelliteRepositoryImpl
import com.example.landguard.data.repository.ZoneRepository
import com.example.landguard.data.repository.ZoneRepositoryImpl
import com.example.landguard.domain.service.RiskEngineService
import com.example.landguard.domain.service.RiskEngineServiceImpl
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
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindZoneRepository(impl: ZoneRepositoryImpl): ZoneRepository

    @Binds
    @Singleton
    abstract fun bindSatelliteRepository(impl: SatelliteRepositoryImpl): SatelliteRepository

    @Binds
    @Singleton
    abstract fun bindParcelRepository(impl: ParcelRepositoryImpl): ParcelRepository

    @Binds
    @Singleton
    abstract fun bindRiskEngineService(impl: RiskEngineServiceImpl): RiskEngineService
}
