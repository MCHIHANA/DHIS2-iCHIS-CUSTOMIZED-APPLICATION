package org.dhis2.usescases.vitaldashboard

import dagger.Subcomponent
import org.dhis2.commons.di.dagger.PerFragment

/**
 * Dagger Component for Vital Signs Dashboard
 * 
 * Defines the dependency injection component for the vital signs dashboard feature.
 * 
 * @author Shadreck Mkandawire
 */
@PerFragment
@Subcomponent(modules = [VitalDashboardModule::class])
interface VitalDashboardComponent {
    fun inject(fragment: VitalDashboardFragment)
}
