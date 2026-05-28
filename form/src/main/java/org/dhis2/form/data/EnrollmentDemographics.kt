package org.dhis2.form.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.OptionSetConfiguration
import org.dhis2.mobile.commons.model.MetadataIconData
import org.hisp.dhis.android.core.option.Option

object EnrollmentDemographics {
    const val AGE_MIN = 0
    const val AGE_MAX = 120
    const val GENDER_OPTION_SET_UID = "ENROLLMENT_DEMOGRAPHIC_GENDER_OPTIONS"
    const val AGE_FIELD_MASK = "^(?:[0-9]|[1-9][0-9]|1[01][0-9]|120)$"

    private const val AGE_VALIDATION_MESSAGE = "Age must be a number from 0 to 120."

    private val ageTokens = setOf("age")
    private val genderTokens = setOf("gender", "sex")

    fun isAgeField(field: FieldUiModel): Boolean = matchesAnyToken(field.label, ageTokens)

    fun isGenderField(field: FieldUiModel): Boolean = matchesAnyToken(field.label, genderTokens)

    fun validateAge(value: String?): String? {
        if (value.isNullOrBlank()) return null

        val age = value.toIntOrNull() ?: return AGE_VALIDATION_MESSAGE
        return if (age in AGE_MIN..AGE_MAX) null else AGE_VALIDATION_MESSAGE
    }

    fun genderOptionSetConfiguration(): OptionSetConfiguration {
        val searchEmitter = MutableStateFlow("")
        return OptionSetConfiguration(
            searchEmitter = searchEmitter,
            onSearch = { searchEmitter.value = it },
            optionFlow =
                searchEmitter.map { query ->
                    PagingData.from(
                        genderOptions()
                            .filter { option ->
                                query.isBlank() ||
                                    option.displayName()?.contains(query, ignoreCase = true) == true ||
                                    option.code()?.contains(query, ignoreCase = true) == true
                            }.map {
                                OptionSetConfiguration.OptionData(
                                    option = it,
                                    metadataIconData = MetadataIconData.defaultIcon(),
                                )
                            },
                    )
                },
        )
    }

    fun isSyntheticGenderOptionSet(optionSetUid: String): Boolean = optionSetUid == GENDER_OPTION_SET_UID

    fun ageGroupFor(age: Int): AgeGroup =
        when (age) {
            in 0..4 -> AgeGroup.UNDER_FIVE
            in 5..14 -> AgeGroup.CHILD
            in 15..24 -> AgeGroup.YOUTH
            in 25..59 -> AgeGroup.ADULT
            else -> AgeGroup.OLDER_ADULT
        }

    fun normalizedGender(value: String?): Gender? =
        when (value?.trim()?.lowercase()) {
            "male", "m" -> Gender.MALE
            "female", "f" -> Gender.FEMALE
            else -> null
        }

    fun matchesAnyToken(
        value: String?,
        tokensToMatch: Set<String>,
    ): Boolean =
        value
            ?.lowercase()
            ?.split(Regex("[^a-z0-9]+"))
            ?.filter { it.isNotBlank() }
            ?.any { it in tokensToMatch } == true

    private fun genderOptions() =
        listOf(
            Option
                .builder()
                .uid("genderMale1")
                .code("Male")
                .displayName("Male")
                .name("Male")
                .sortOrder(1)
                .build(),
            Option
                .builder()
                .uid("genderFem01")
                .code("Female")
                .displayName("Female")
                .name("Female")
                .sortOrder(2)
                .build(),
        )

    enum class AgeGroup {
        UNDER_FIVE,
        CHILD,
        YOUTH,
        ADULT,
        OLDER_ADULT,
    }

    enum class Gender {
        MALE,
        FEMALE,
    }
}
