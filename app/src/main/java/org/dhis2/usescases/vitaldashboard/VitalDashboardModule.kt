package org.dhis2.usescases.vitaldashboard

import dagger.Module
import dagger.Provides
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.repository.VitalDashboardRepository
import org.dhis2.usescases.vitaldashboard.repository.VitalSignConfig
import org.hisp.dhis.android.core.D2

/**
 * Dagger Module for Vital Signs Dashboard
 *
 * Provides all dependencies for the vital signs dashboard feature.
 * [Dispatcher] is constructed directly here because it is a plain data class
 * with no @Inject constructor and is not bound anywhere else in the Dagger graph.
 *
 * @author Shadreck Mkandawire
 */
@Module
class VitalDashboardModule {

    @Provides
    @PerFragment
    fun provideDispatcher(): Dispatcher = Dispatcher()

    @Provides
    @PerFragment
    fun provideVitalSignConfig(): VitalSignConfig = VitalSignConfig()

    @Provides
    @PerFragment
    fun provideVitalDashboardRepository(
        d2: D2,
        dispatcher: Dispatcher,
        vitalSignConfig: VitalSignConfig,
    ): VitalDashboardRepository = VitalDashboardRepository(d2, dispatcher, vitalSignConfig)

    @Provides
    @PerFragment
    fun provideVitalDashboardViewModelFactory(
        repository: VitalDashboardRepository,
        dispatcher: Dispatcher,
    ): VitalDashboardViewModelFactory = VitalDashboardViewModelFactory(repository, dispatcher)
}
