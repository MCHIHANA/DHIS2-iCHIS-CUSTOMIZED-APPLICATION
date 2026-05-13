package org.dhis2.usescases.vitaldashboard

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.repository.VitalDashboardRepository
import org.dhis2.usescases.vitaldashboard.repository.VitalSignConfig
import org.hisp.dhis.android.core.D2

/**
 * Dagger Module for Vital Signs Dashboard
 * 
 * Provides dependencies for the vital signs dashboard feature.
 * 
 * @author Shadreck Mkandawire
 */
@Module
class VitalDashboardModule {

    @Provides
    @PerFragment
    fun provideDispatcher(): Dispatcher {
        return Dispatcher(
            io = Dispatchers.IO,
            main = Dispatchers.Main,
            default = Dispatchers.Default
        )
    }

    @Provides
    @PerFragment
    fun provideVitalSignConfig(): VitalSignConfig {
        return VitalSignConfig()
    }

    @Provides
    @PerFragment
    fun provideVitalDashboardRepository(
        d2: D2,
        dispatchers: Dispatcher,
        vitalSignConfig: VitalSignConfig
    ): VitalDashboardRepository {
        return VitalDashboardRepository(d2, dispatchers, vitalSignConfig)
    }

    @Provides
    @PerFragment
    fun provideVitalDashboardViewModelFactory(
        repository: VitalDashboardRepository,
        dispatchers: Dispatcher
    ): VitalDashboardViewModelFactory {
        return VitalDashboardViewModelFactory(repository, dispatchers)
    }
}
