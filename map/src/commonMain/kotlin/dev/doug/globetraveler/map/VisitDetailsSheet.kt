package dev.doug.globetraveler.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.doug.globetraveler.design.GlobeTheme
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailsSheet(
    details: RegionDetails,
    onSave: (LocalDate?, String?) -> Unit,
    onRemoveVisit: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        VisitDetailsSheetContent(
            regionName = details.region.name,
            isVisited = details.visit != null,
            initialVisitedAt = details.visit?.visitedAt,
            initialNotes = details.visit?.notes.orEmpty(),
            onSave = onSave,
            onRemoveVisit = onRemoveVisit,
        )
    }
}

@Composable
fun VisitDetailsSheetContent(
    regionName: String,
    isVisited: Boolean,
    initialVisitedAt: LocalDate?,
    initialNotes: String,
    onSave: (LocalDate?, String?) -> Unit,
    onRemoveVisit: () -> Unit,
) {
    var visitedAt by remember { mutableStateOf(initialVisitedAt) }
    var notes by remember { mutableStateOf(initialNotes) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(regionName, style = MaterialTheme.typography.headlineSmall)

        OutlinedButton(onClick = { showDatePicker = true }) {
            Text(visitedAt?.toString() ?: "Add visit date")
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(visitedAt, notes) }, modifier = Modifier.weight(1f)) {
                Text(if (isVisited) "Save" else "Mark visited")
            }
            if (isVisited) {
                TextButton(onClick = onRemoveVisit) {
                    Text("Remove visit")
                }
            }
        }
    }

    if (showDatePicker) {
        VisitDatePickerDialog(
            initialDate = visitedAt,
            onConfirm = { picked ->
                visitedAt = picked
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitDatePickerDialog(
    initialDate: LocalDate?,
    onConfirm: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDayIn(TimeZone.UTC)
            ?.toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        pickerState.selectedDateMillis?.let { millis ->
                            Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC).date
                        },
                    )
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Preview
@Composable
private fun Preview_VisitDetailsSheetContent_Visited() {
    GlobeTheme {
        VisitDetailsSheetContent(
            regionName = "Nevada",
            isVisited = true,
            initialVisitedAt = LocalDate(2025, 3, 9),
            initialNotes = "Reno weekend",
            onSave = { _, _ -> },
            onRemoveVisit = {},
        )
    }
}

@Preview
@Composable
private fun Preview_VisitDetailsSheetContent_NewVisit() {
    GlobeTheme {
        VisitDetailsSheetContent(
            regionName = "Ohio",
            isVisited = false,
            initialVisitedAt = null,
            initialNotes = "",
            onSave = { _, _ -> },
            onRemoveVisit = {},
        )
    }
}
