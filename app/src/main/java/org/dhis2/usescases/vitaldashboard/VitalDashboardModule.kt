package org.dhis2.usescases.vitaldashboard

import dagger.Module
import dagger.Provides
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.repository.VitalDashboardRepository
import org.dhis2.usescases.vitaldashboard.repository.VitalSignConfig
import org.hisp.dhis.android.core.D2

/**
 * Dagger Module for Vital Signs Dashboard.
 *
 * [VitalSignConfig] is fully offline — no SensorConfigRepository or DataStore
 * dependency. [Dispatcher] is resolved from the @Singleton binding in DispatcherModule.
 */
@Module
class VitalDashboardModule {

    @Provides
    @PerFragment
    fun provideVitalSignConfig(): VitalSignConfig = VitalSignConfig()

    @Provides
    @PerFragment
    fun provideVitalDashboardRepository(
        d2: D2,
        dispatchers: Dispatcher,
        vitalSignConfig: VitalSignConfig,
    ): VitalDashboardRepository = VitalDashboardRepository(d2, dispatchers, vitalSignConfig)

    @Provides
    @PerFragment
    fun provideVitalDashboardViewModelFactory(
        repository: VitalDashboardRepository,
        dispatchers: Dispatcher,
    ): VitalDashboardViewModelFactory = VitalDashboardViewModelFactory(repository, dispatchers)
}
