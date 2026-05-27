package org.dhis2.sensors.oximeter.dhis2

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for DHIS2 API endpoints.
 */
interface Dhis2ApiService {

    /**
     * Retrieves current user information including assigned organisation units.
     *
     * @param fields Comma-separated list of fields to retrieve
     * @return Response containing user information
     */
    @GET("api/me")
    suspend fun getCurrentUser(
        @Query("fields") fields: String = "organisationUnits[id]"
    ): Response<UserResponse>

    /**
     * Submits data values to DHIS2.
     *
     * @param dataValueSet The data value set to submit
     * @return Response containing submission result
     */
    @POST("api/dataValueSets")
    suspend fun submitDataValues(
        @Body dataValueSet: DataValueSet
    ): Response<DataValueSetResponse>
}

/**
 * Response model for user information.
 */
data class UserResponse(
    val organisationUnits: List<OrganisationUnit>
)

/**
 * Organisation unit model.
 */
data class OrganisationUnit(
    val id: String
)

/**
 * Data value set for submission.
 */
data class DataValueSet(
    val dataValues: List<DataValue>
)

/**
 * Individual data value.
 */
data class DataValue(
    val dataElement: String,
    val value: String,
    val period: String,
    val orgUnit: String
)

/**
 * Response from data value set submission.
 */
data class DataValueSetResponse(
    val status: String? = null,
    val description: String? = null,
    val importCount: ImportCount? = null,
    val conflicts: List<Conflict>? = null
)

/**
 * Import count statistics.
 */
data class ImportCount(
    val imported: Int = 0,
    val updated: Int = 0,
    val ignored: Int = 0,
    val deleted: Int = 0
)

/**
 * Conflict information.
 */
data class Conflict(
    val `object`: String? = null,
    val value: String? = null
)
