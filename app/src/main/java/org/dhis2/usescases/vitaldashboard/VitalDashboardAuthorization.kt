package org.dhis2.usescases.vitaldashboard

import org.dhis2.data.dhislogic.AUTH_ALL
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.helpers.UidsHelper
import timber.log.Timber

private val authorizedRoleNames =
    setOf(
        "Doctor",
        "Clinician",
        "Administrator",
        "Admin",
        "System Administrator",
        "Superuser",
        "Super User",
        "Physician",
        "Medical Officer",
        "Nurse",
        "Healthcare Worker",
    )

private val authorizedAuthorities =
    setOf(
        AUTH_ALL,
        "F_VITAL_SIGNS_DASHBOARD",
    )

object VitalDashboardAuthorization {

    fun hasAccess(d2: D2): Boolean =
        try {
            val hasAuthorizedAuthority =
                d2.userModule()
                    .authorities()
                    .byName()
                    .`in`(*authorizedAuthorities.toTypedArray())
                    .one()
                    .blockingExists()

            if (hasAuthorizedAuthority) {
                Timber.d("Vital dashboard access granted via authority match")
                true
            } else {
                val userRoleUids = d2.userModule().user().blockingGet()?.userRoles().orEmpty()
                val roles =
                    if (userRoleUids.isEmpty()) {
                        emptyList()
                    } else {
                        d2.userModule().userRoles()
                            .byUid().`in`(UidsHelper.getUidsList(userRoleUids))
                            .blockingGet()
                    }

                val roleNames = roles.mapNotNull { it.name() }.filter { it.isNotBlank() }
                val matchedRole =
                    roleNames.firstOrNull { roleName ->
                        authorizedRoleNames.any { authorizedName ->
                            roleName.contains(authorizedName, ignoreCase = true)
                        }
                    }

                Timber.d(
                    "Vital dashboard role check. User roles=%s, matchedRole=%s",
                    roleNames.joinToString(),
                    matchedRole ?: "none",
                )

                matchedRole != null
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Error checking Vital Signs Dashboard authorization")
            false
        }
}
