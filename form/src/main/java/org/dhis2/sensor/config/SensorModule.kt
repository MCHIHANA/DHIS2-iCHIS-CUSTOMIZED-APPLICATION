package org.dhis2.sensor.config

import org.koin.dsl.module
import org.koin.core.module.Module

val sensorModule: Module = module {
    single { SensorConfigApi(get()) }
    single { SensorConfigRepository(get(), get()) }
}
