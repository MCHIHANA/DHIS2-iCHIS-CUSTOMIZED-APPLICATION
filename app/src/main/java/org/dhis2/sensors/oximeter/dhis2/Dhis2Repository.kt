package org.dhis2.sensors.oximeter.dhis2

import org.dhis2.sensors.oximeter.data.OximeterReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/**
 * Repository interface for DHIS2 operations.
 */
interface Dhis2Repository {
    /**
     * Retrieves the organisation unit UID for the current user.
     *
     * @return Organisation unit UID
     * @throws Dhis2Exception if retrieval fails
     */
    suspend fun getCurrentUserOrgUnit(): String

    /**
     * Submits oximeter readings to DHIS2.
     *
     * @param reading The oximeter reading to submit
     * @param orgUnitUid The organisation unit UID
     * @return Result indicating success or failure
     */
    suspend fun submitReadings(reading: OximeterReading, orgUnitUid: String): Result<String>
}

/**
 * Implementation of Dhis2Repository using Retrofit.
 */
class Dhis2RepositoryImpl(
    private val apiService: Dhis2ApiService
) : Dhis2Repository {

    companion object {
        // DHIS2 Data Element UIDs
        private const val SPO2_DATA_ELEMENT = "gAFXupYQDOb"
        private const val HEART_RATE_DATA_ELEMENT = "VqwQWWDmYLn"
    }

    override suspend fun getCurrentUserOrgUnit(): String {
        return try {
            val response = apiService.getCurrentUser()
            
            if (response.isSuccessful) {
                val orgUnits = response.body()?.organisationUnits
                if (orgUnits.isNullOrEmpty()) {
                    throw Dhis2Exception("No organisation units assigned to user")
                }
                
                // Return the first organisation unit
                val orgUnitId = orgUnits.first().id
                Timber.d("Retrieved org unit: $orgUnitId")
                orgUnitId
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to get user org unit: ${response.code()} - $errorBody")
                
                when (response.code()) {
                    401 -> throw Dhis2Exception("Authentication failed. Please check credentials.")
                    403 -> throw Dhis2Exception("Access denied. Insufficient permissions.")
                    404 -> throw Dhis2Exception("API endpoint not found.")
                    else -> throw Dhis2Exception("Failed to retrieve organisation unit: ${response.code()}")
                }
            }
        } catch (e: Dhis2Exception) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error getting user org unit")
            throw Dhis2Exception("Network error: ${e.message}", e)
        }
    }

    override suspend fun submitReadings(
        reading: OximeterReading,
        orgUnitUid: String
    ): Result<String> {
        return try {
            val period = getCurrentPeriod()
            
            val dataValueSet = DataValueSet(
                dataValues = listOf(
                    DataValue(
                        dataElement = SPO2_DATA_ELEMENT,
                        value = reading.spo2.toString(),
                        period = period,
                        orgUnit = orgUnitUid
                    ),
                    DataValue(
                        dataElement = HEART_RATE_DATA_ELEMENT,
                        value = reading.heartRateBpm.toString(),
                        period = period,
                        orgUnit = orgUnitUid
                    )
                )
            )
            
            Timber.d("Submitting data: SpO2=${reading.spo2}, HR=${reading.heartRateBpm}, period=$period, orgUnit=$orgUnitUid")
            
            val response = apiService.submitDataValues(dataValueSet)
            
            if (response.isSuccessful) {
                val result = response.body()
                val imported = result?.importCount?.imported ?: 0
                val updated = result?.importCount?.updated ?: 0
                
                Timber.d("Submission successful: imported=$imported, updated=$updated")
                Result.success("Successfully submitted: $imported imported, $updated updated")
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Submission failed: ${response.code()} - $errorBody")
                
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed. Please re-login."
                    403 -> "Access denied. Insufficient permissions."
                    409 -> "Duplicate value. Data may have already been submitted."
                    422 -> "Invalid data format. Please check the readings."
                    else -> "Submission failed: ${response.code()}"
                }
                
                Result.failure(Dhis2Exception(errorMessage))
            }
        } catch (e: Dhis2Exception) {
            Timber.e(e, "DHIS2 error during submission")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Error submitting readings")
            
            // Retry logic could be implemented here
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Request timeout. Please check your connection."
                e.message?.contains("unable to resolve host", ignoreCase = true) == true -> 
                    "Cannot reach server. Please check your internet connection."
                else -> "Network error: ${e.message}"
            }
            
            Result.failure(Dhis2Exception(errorMessage, e))
        }
    }

    /**
     * Gets the current period in DHIS2 format (yyyyMMdd).
     */
    private fun getCurrentPeriod(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        return dateFormat.format(Date())
    }
}

/**
 * Custom exception for DHIS2 operations.
 */
class Dhis2Exception(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
