package org.dhis2.usescases.main

import dagger.Subcomponent
import org.dhis2.commons.di.dagger.PerActivity
import org.dhis2.usescases.troubleshooting.TroubleshootingComponent
import org.dhis2.usescases.troubleshooting.TroubleshootingModule
import org.dhis2.usescases.vitaldashboard.VitalDashboardComponent
import org.dhis2.usescases.vitaldashboard.VitalDashboardModule

@PerActivity
@Subcomponent(modules = [MainModule::class])
interface MainComponent {
    fun inject(mainActivity: MainActivity)

    fun plus(troubleShootingModule: TroubleshootingModule): TroubleshootingComponent
    
    fun plus(vitalDashboardModule: VitalDashboardModule): VitalDashboardComponent
}
