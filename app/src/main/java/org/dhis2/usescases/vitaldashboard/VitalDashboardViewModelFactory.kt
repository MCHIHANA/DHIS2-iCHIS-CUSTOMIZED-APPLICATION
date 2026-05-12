package org.dhis2.usescases.vitaldashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dhis2.commons.data.Dispatcher
import org.dhis2.usescases.vitaldashboard.repository.VitalDashboardRepository

/**
 * ViewModel Factory for Vital Dashboard
 * 
 * Creates VitalDashboardViewModel instances with required dependencies
 * 
 * @author Shadreck Mkandawire
 */
class VitalDashboardViewModelFactory(
    private val repository: VitalDashboardRepository,
    private val dispatchers: Dispatcher
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VitalDashboardViewModel::class.java)) {
            return VitalDashboardViewModel(repository, dispatchers) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
