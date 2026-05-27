package org.dhis2.form.ui.provider.inputfield

import android.content.Intent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import org.dhis2.form.R
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.dhis2.form.ui.dialog.SensorConnectionBottomSheet
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.input.pointer.pointerInput
import org.hisp.dhis.mobile.ui.designsystem.theme.TextColor
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ButtonStyle
import org.hisp.dhis.mobile.ui.designsystem.component.InputShellState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.form.data.EventRepository.Companion.EVENT_ORG_UNIT_UID
import org.dhis2.form.extensions.autocompleteList
import org.dhis2.form.extensions.inputState
import org.dhis2.form.extensions.legend
import org.dhis2.form.extensions.supportingText
import org.dhis2.form.model.EnrollmentDetail
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.UiRenderType
import org.dhis2.form.ui.event.RecyclerViewUiEvents
import org.dhis2.form.ui.intent.FormIntent
import org.dhis2.form.ui.keyboard.keyboardAsState
import org.dhis2.form.ui.provider.onFieldFocusChanged
import org.dhis2.sensor.config.SensorAvailabilityManager
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.mobile.ui.designsystem.component.InputEmail
import org.hisp.dhis.mobile.ui.designsystem.component.InputInteger
import org.hisp.dhis.mobile.ui.designsystem.component.InputLetter
import org.hisp.dhis.mobile.ui.designsystem.component.InputLink
import org.hisp.dhis.mobile.ui.designsystem.component.InputLongText
import org.hisp.dhis.mobile.ui.designsystem.component.InputNegativeInteger
import org.hisp.dhis.mobile.ui.designsystem.component.InputNotSupported
import org.hisp.dhis.mobile.ui.designsystem.component.InputNumber
import org.hisp.dhis.mobile.ui.designsystem.component.InputOrgUnit
import org.hisp.dhis.mobile.ui.designsystem.component.InputPercentage
import org.hisp.dhis.mobile.ui.designsystem.component.InputPhoneNumber
import org.hisp.dhis.mobile.ui.designsystem.component.InputPositiveInteger
import org.hisp.dhis.mobile.ui.designsystem.component.InputPositiveIntegerOrZero
import org.hisp.dhis.mobile.ui.designsystem.component.InputStyle
import org.hisp.dhis.mobile.ui.designsystem.component.model.RegExValidations
import androidx.compose.ui.platform.LocalContext
import org.dhis2.form.ui.FormViewModel
import androidx.compose.runtime.collectAsState
import org.dhis2.form.ui.sensor.SensorFieldResolver
import org.dhis2.form.ui.sensor.SensorStatusText

@Composable
fun FieldProvider(
    modifier: Modifier,
    inputStyle: InputStyle = InputStyle.DataInputStyle(),
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    resources: ResourceManager,
    focusManager: FocusManager,
    sensorStatus: String?,
    isScanning: Boolean,
    onConnectToSensor: (String) -> Unit,
    onNextClicked: () -> Unit,
    onFileSelected: (String) -> Unit,
    reEvaluateCustomIntentRequestParameters: Boolean,
    viewModel: FormViewModel? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    var visibleArea by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()
    val keyboardState by keyboardAsState()

    var modifierWithFocus =
        modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged { intSize ->
                visibleArea =
                    Rect(
                        size = Size(intSize.width.toFloat(), intSize.height.toFloat()),
                        offset = Offset(0f, 200f),
                    )
            }.onFocusChanged {
                if (it.isFocused && !fieldUiModel.focused) {
                    scope.launch {
                        fieldUiModel.onItemClick()
                        delay(10)
                        bringIntoViewRequester.bringIntoView(visibleArea)
                    }
                }
            }

    if (!fieldUiModel.needKeyboard()) {
        modifierWithFocus =
            modifierWithFocus
                .focusRequester(focusRequester)
                .focusable()
    }

    LaunchedEffect(keyboardState) {
        if (fieldUiModel.focused) {
            bringIntoViewRequester.bringIntoView(visibleArea)
        }
    }

    // Wrap everything in SensorButtonWrapper so the Connect Sensor button
    // appears on any field that has a sensor config, regardless of field type.
    SensorButtonWrapper(
        fieldUiModel = fieldUiModel,
        intentHandler = intentHandler,
        sensorStatus = sensorStatus,
        isScanning = isScanning,
        onConnectToSensor = onConnectToSensor,
        viewModel = viewModel,
    ) {
        when {
            fieldUiModel.optionSet != null && fieldUiModel.valueType != ValueType.MULTI_TEXT ->
                ProvideByOptionSet(
                    modifier = modifierWithFocus,
                    inputStyle = inputStyle,
                    fieldUiModel = fieldUiModel,
                    intentHandler = intentHandler,
                    fetchOptions = {
                        intentHandler(
                            FormIntent.FetchOptions(
                                fieldUiModel.uid,
                                fieldUiModel.optionSet!!,
                                value = fieldUiModel.value,
                            ),
                        )
                    },
                )

            fieldUiModel.customIntent != null ->
                ProvideCustomIntentInput(
                    fieldUiModel = fieldUiModel,
                    intentHandler = intentHandler,
                    uiEventHandler = uiEventHandler,
                    resources = resources,
                    inputStyle = inputStyle,
                    reEvaluateRequestParams = reEvaluateCustomIntentRequestParameters,
                    modifier = modifierWithFocus,
                )

            fieldUiModel.eventCategories != null ->
                ProvideCategorySelectorInput(
                    modifier = modifierWithFocus,
                    inputStyle = inputStyle,
                    fieldUiModel = fieldUiModel,
                )

            else ->
                ProvideByValueType(
                    modifier = modifierWithFocus,
                    inputStyle = inputStyle,
                    fieldUiModel = fieldUiModel,
                    intentHandler = intentHandler,
                    uiEventHandler = uiEventHandler,
                    resources = resources,
                    focusRequester = focusRequester,
                    onNextClicked = onNextClicked,
                    focusManager = focusManager,
                    onFileSelected = onFileSelected,
                )
        }
    }
}

@Composable
fun SensorButtonWrapper(
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    sensorStatus: String?,
    isScanning: Boolean,
    onConnectToSensor: (String) -> Unit,
    viewModel: FormViewModel? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val bleAvailable = remember { SensorAvailabilityManager.isBleSupported(context) }

    val sensorConfig = viewModel?.sensorConfigRepository?.getConfigByDataElement(fieldUiModel.uid)

    // Known sensor field UIDs — hardcoded as a guaranteed fallback when DataStore
    // hasn't loaded yet or viewModel is null (e.g. search screen).
    val knownSensorUids = SensorFieldResolver.knownSensorFieldUids

    val isSensorField = when {
        sensorConfig != null -> true                          // DataStore match
        fieldUiModel.uid in knownSensorUids -> true           // hardcoded UID match
        else -> {
            // Label keyword fallback
            val label = fieldUiModel.label.lowercase()
            label.contains("temperature") ||
                label.contains("weight") ||
                label.contains("heart rate") ||
                label.contains("heartrate") ||
                label.contains("systolic") ||
                label.contains("diastolic") ||
                label.contains("blood pressure") ||
                label.contains("spo2") ||
                label.contains("sp02") ||
                label.contains("oxygen") ||
                label.contains("saturation") ||
                label.contains("pulse") ||
                label.contains("bpm")
        }
    }

    if (bleAvailable && isSensorField && fieldUiModel.editable) {
        val hasCompletedReading = SensorFieldResolver.hasCompletedReading(sensorStatus)
        val actionLabel = if (hasCompletedReading) "Retake Measurement" else "Connect Sensor"

        Column(modifier = Modifier.fillMaxWidth()) {
            // Row layout: [Field Content] [Spacer] [Connect Sensor Button]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isScanning) {
                                Modifier
                                    .graphicsLayer(alpha = 0.5f)
                                    .pointerInput(Unit) {} // Consume touch events
                            } else {
                                Modifier
                            }
                        )
                ) {
                    content()
                }

                // Spacer between field and button
                Spacer(modifier = Modifier.width(4.dp))

                org.dhis2.form.ui.sensor.SensorConnectButton(
                    text = actionLabel,
                    onClick = { onConnectToSensor(fieldUiModel.uid) }
                )
            }

            // Status text below the field row
            if (isScanning || !sensorStatus.isNullOrEmpty()) {
                Text(
                    text = sensorStatus ?: SensorStatusText.WAITING_FOR_DATA,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        hasCompletedReading -> Color(0xFF4CAF50)
                        SensorStatusText.isConnected(sensorStatus) -> Color(0xFF4CAF50)
                        SensorStatusText.isFailure(sensorStatus) ||
                            sensorStatus?.contains("No connections", ignoreCase = true) == true -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.padding(horizontal = Spacing.Spacing16, vertical = Spacing.Spacing4)
                )
            }
        }
    } else {
        content()
    }
}

@Composable
fun ProvideByValueType(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    resources: ResourceManager,
    focusRequester: FocusRequester,
    onNextClicked: () -> Unit,
    focusManager: FocusManager,
    onFileSelected: (String) -> Unit,
) {
    when (fieldUiModel.valueType) {
        ValueType.TEXT -> {
            ProvideInputsForValueTypeText(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                uiEventHandler = uiEventHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.INTEGER_POSITIVE -> {
            ProvideIntegerPositive(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.INTEGER_ZERO_OR_POSITIVE -> {
            ProvideIntegerPositiveOrZero(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.PERCENTAGE -> {
            ProvidePercentage(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.NUMBER -> {
            ProvideNumber(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.INTEGER_NEGATIVE -> {
            ProvideIntegerNegative(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.LONG_TEXT -> {
            ProvideLongText(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.LETTER -> {
            ProvideLetter(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.INTEGER -> {
            ProvideInteger(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.ORGANISATION_UNIT -> {
            ProvideOrgUnitInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                uiEventHandler = uiEventHandler,
                intentHandler = intentHandler,
            )
        }

        ValueType.UNIT_INTERVAL -> {
            ProvideUnitIntervalInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.EMAIL -> {
            ProvideEmail(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                uiEventHandler = uiEventHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.FILE_RESOURCE -> {
            ProvideInputFileResource(
                modifier = modifier,
                fieldUiModel = fieldUiModel,
                resources = resources,
                onFileSelected = onFileSelected,
                uiEventHandler = uiEventHandler,
            )
        }

        ValueType.URL -> {
            ProvideInputLink(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                uiEventHandler = uiEventHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.BOOLEAN -> {
            when (fieldUiModel.renderingType) {
                UiRenderType.HORIZONTAL_CHECKBOXES,
                UiRenderType.VERTICAL_CHECKBOXES,
                -> {
                    ProvideYesNoCheckBoxInput(
                        modifier = modifier,
                        inputStyle = inputStyle,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                        resources = resources,
                    )
                }

                else -> {
                    ProvideYesNoRadioButtonInput(
                        modifier = modifier,
                        inputStyle = inputStyle,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                        resources = resources,
                    )
                }
            }
        }

        ValueType.TRUE_ONLY -> {
            when (fieldUiModel.renderingType) {
                UiRenderType.TOGGLE -> {
                    ProvideYesOnlySwitchInput(
                        modifier = modifier,
                        inputStyle = inputStyle,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                    )
                }

                else -> {
                    ProvideYesOnlyCheckBoxInput(
                        modifier = modifier,
                        inputStyle = inputStyle,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                    )
                }
            }
        }

        ValueType.PHONE_NUMBER -> {
            ProvideInputPhoneNumber(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                uiEventHandler = uiEventHandler,
                focusManager = focusManager,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.DATE,
        ValueType.DATETIME,
        ValueType.TIME,
        -> {
            when (fieldUiModel.periodSelector) {
                null -> {
                    ProvideInputDate(
                        modifier = modifier,
                        inputStyle = inputStyle,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                        onNextClicked = onNextClicked,
                    )
                }

                else -> {
                    ProvidePeriodSelector(
                        modifier = Modifier,
                        inputStyle = inputStyle,
                        fieldUiModel = fieldUiModel,
                        focusRequester = focusRequester,
                        uiEventHandler = uiEventHandler,
                    )
                }
            }
        }

        ValueType.IMAGE -> {
            when (fieldUiModel.renderingType) {
                UiRenderType.CANVAS -> {
                    ProvideInputSignature(
                        modifier = modifier,
                        fieldUiModel = fieldUiModel,
                    )
                }

                else -> {
                    ProvideInputImage(
                        modifier = modifier,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                        uiEventHandler = uiEventHandler,
                        resources = resources,
                        onFileSelected = onFileSelected,
                    )
                }
            }
        }

        ValueType.COORDINATE -> {
            when (fieldUiModel.renderingType) {
                UiRenderType.POLYGON, UiRenderType.MULTI_POLYGON -> {
                    ProvidePolygon(
                        modifier = modifier,
                        fieldUiModel = fieldUiModel,
                    )
                }

                else -> {
                    ProvideInputCoordinate(
                        modifier = modifier,
                        fieldUiModel = fieldUiModel,
                        intentHandler = intentHandler,
                        uiEventHandler = uiEventHandler,
                        resources = resources,
                    )
                }
            }
        }

        ValueType.AGE -> {
            ProvideInputAge(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
                resources = resources,
                onNextClicked = onNextClicked,
            )
        }

        ValueType.MULTI_TEXT -> {
            ProvideMultiSelectionInput(
                modifier = modifier,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
            )
        }

        ValueType.REFERENCE,
        ValueType.GEOJSON,
        ValueType.USERNAME,
        ValueType.TRACKER_ASSOCIATE,
        null,
        -> {
            InputNotSupported(title = fieldUiModel.label)
        }
    }
}

@Composable
fun ProvideByOptionSet(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    fetchOptions: () -> Unit,
) {
    when (fieldUiModel.renderingType) {
        UiRenderType.HORIZONTAL_RADIOBUTTONS,
        UiRenderType.VERTICAL_RADIOBUTTONS,
        -> {
            ProvideRadioButtonInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
            )
        }

        UiRenderType.HORIZONTAL_CHECKBOXES,
        UiRenderType.VERTICAL_CHECKBOXES,
        -> {
            ProvideCheckBoxInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
            )
        }

        UiRenderType.MATRIX -> {
            ProvideMatrixInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
            )
        }

        UiRenderType.SEQUENCIAL -> {
            ProvideSequentialInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                intentHandler = intentHandler,
            )
        }

        // "Remaining option sets" are in fun getLayoutForOptionSet

        else -> {
            ProvideDropdownInput(
                modifier = modifier,
                inputStyle = inputStyle,
                fieldUiModel = fieldUiModel,
                fetchOptions = fetchOptions,
            )
        }
    }
}

@Composable
private fun ProvideIntegerPositive(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }
    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputPositiveInteger(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideIntegerPositiveOrZero(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputPositiveIntegerOrZero(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvidePercentage(
    modifier: Modifier = Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputPercentage(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideNumber(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputNumber(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        notation = RegExValidations.BRITISH_DECIMAL_NOTATION,
        autoCompleteList = fieldUiModel.autocompleteList(),
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideIntegerNegative(
    modifier: Modifier = Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }
    var value by remember(fieldUiModel.value) {
        mutableStateOf(
            TextFieldValue(
                fieldUiModel.value?.replace("-", "") ?: "",
                savedTextSelection,
            ),
        )
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputNegativeInteger(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideLongText(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputLongText(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        imeAction = ImeAction.Default,
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideLetter(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputLetter(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideInteger(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputInteger(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideEmail(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    intentHandler: (FormIntent) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputEmail(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onEmailActionCLicked = {
            uiEventHandler.invoke(
                RecyclerViewUiEvents.OpenChooserIntent(
                    Intent.ACTION_SENDTO,
                    value.text,
                    fieldUiModel.uid,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideInputPhoneNumber(
    fieldUiModel: FieldUiModel,
    inputStyle: InputStyle,
    intentHandler: (FormIntent) -> Unit,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputPhoneNumber(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onCallActionClicked = {
            uiEventHandler.invoke(
                RecyclerViewUiEvents.OpenChooserIntent(
                    Intent.ACTION_DIAL,
                    value.text,
                    fieldUiModel.uid,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideInputLink(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    intentHandler: (FormIntent) -> Unit,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    focusManager: FocusManager,
    onNextClicked: () -> Unit,
) {
    var savedTextSelection by remember {
        mutableStateOf(
            TextRange(if (fieldUiModel.value != null) fieldUiModel.value!!.length else 0),
        )
    }

    var value by remember(fieldUiModel.value) {
        mutableStateOf(TextFieldValue(fieldUiModel.value ?: "", savedTextSelection))
    }

    var clickedOnNext by remember {
        mutableStateOf(false)
    }

    var lostFocus by remember {
        mutableStateOf(false)
    }

    InputLink(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputTextFieldValue = value,
        isRequiredField = fieldUiModel.mandatory,
        onNextClicked = {
            clickedOnNext = true
            onNextClicked()
        },
        onValueChanged = {
            value = it ?: TextFieldValue()
            savedTextSelection = it?.selection ?: TextRange.Zero
            intentHandler(
                FormIntent.OnTextChange(
                    fieldUiModel.uid,
                    value.text,
                    fieldUiModel.valueType,
                ),
            )
        },
        onLinkActionCLicked = {
            uiEventHandler.invoke(
                RecyclerViewUiEvents.OpenChooserIntent(
                    Intent.ACTION_VIEW,
                    value.text,
                    fieldUiModel.uid,
                ),
            )
        },
        onFocusChanged = { isFocused ->
            lostFocus = lostFocus == true && isFocused == false
            onFieldFocusChanged(
                fieldUid = fieldUiModel.uid,
                value = value.text,
                valueType = fieldUiModel.valueType,
                lostFocus = lostFocus,
                onNextClicked = clickedOnNext,
                intentHandler = intentHandler,
            )
        },
        autoCompleteList = fieldUiModel.autocompleteList(),
        autoCompleteItemSelected = {
            focusManager.clearFocus()
        },
    )
}

@Composable
private fun ProvideOrgUnitInput(
    modifier: Modifier,
    inputStyle: InputStyle,
    fieldUiModel: FieldUiModel,
    uiEventHandler: (RecyclerViewUiEvents) -> Unit,
    intentHandler: (FormIntent) -> Unit,
) {
    var inputFieldValue by remember(
        fieldUiModel,
    ) {
        mutableStateOf(fieldUiModel.displayName)
    }

    val showResetButton = (fieldUiModel.uid != EnrollmentDetail.ORG_UNIT_UID.name && fieldUiModel.uid != EVENT_ORG_UNIT_UID)

    InputOrgUnit(
        modifier = modifier.fillMaxWidth(),
        inputStyle = inputStyle,
        title = fieldUiModel.label,
        state = fieldUiModel.inputState(),
        supportingText = fieldUiModel.supportingText(),
        legendData = fieldUiModel.legend(),
        inputText = inputFieldValue ?: "",
        isRequiredField = fieldUiModel.mandatory,
        onValueChanged = {
            inputFieldValue = it
            intentHandler(
                FormIntent.OnSave(
                    fieldUiModel.uid,
                    it,
                    fieldUiModel.valueType,
                ),
            )
        },
        onOrgUnitActionCLicked = {
            uiEventHandler.invoke(
                RecyclerViewUiEvents.OpenOrgUnitDialog(
                    uid = fieldUiModel.uid,
                    label = fieldUiModel.label,
                    value = fieldUiModel.value,
                    orgUnitSelectorScope = fieldUiModel.orgUnitSelectorScope,
                ),
            )
        },
        showResetButton = showResetButton,
    )
}

private fun FieldUiModel.needKeyboard() =
    optionSet == null &&
        valueType?.let { it.isText || it.isNumeric || it.isDate } ?: false
