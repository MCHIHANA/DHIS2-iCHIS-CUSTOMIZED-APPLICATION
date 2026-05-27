package org.dhis2.commons.vitals

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object VitalDashboardRefreshBus {
    private val _refreshEvents = MutableSharedFlow<VitalDashboardRefreshEvent>(
        replay = 0,
        extraBufferCapacity = 16,
    )

    val refreshEvents: SharedFlow<VitalDashboardRefreshEvent> = _refreshEvents.asSharedFlow()

    fun notifyRefresh(event: VitalDashboardRefreshEvent = VitalDashboardRefreshEvent()) {
        _refreshEvents.tryEmit(event)
    }
}

data class VitalDashboardRefreshEvent(
    val source: RefreshSource = RefreshSource.DATA_VALUE_SAVED,
    val fieldUid: String? = null,
)

enum class RefreshSource {
    DATA_VALUE_SAVED,
    EVENT_COMPLETED,
    EVENT_ACTIVATED,
}
