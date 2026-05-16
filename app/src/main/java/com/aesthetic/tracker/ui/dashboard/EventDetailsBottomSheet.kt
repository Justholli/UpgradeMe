@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aesthetic.tracker.ui.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDetailsBottomSheet(
    event: ScheduleEventUi,
    onDismiss: () -> Unit,
    onToggle: (ScheduleEventUi) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val selectedExercise = remember { mutableStateOf<WorkoutExerciseUi?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eventDetailsSheet")
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EventTypeIcon(event.type, event.title)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(event.timeRange(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            EventStatusChip(event.status)
            if (event.canToggleCompletion) {
                Button(
                    modifier = Modifier.fillMaxWidth().testTag("toggleEventDoneButton"),
                    onClick = {
                        onToggle(event)
                        onDismiss()
                    },
                ) {
                    Crossfade(targetState = event.isCompleted, label = "toggleEventDoneButtonText") { completed ->
                        Text(if (completed) "Снять отметку" else "Отметить выполненным")
                    }
                }
            }
            Text(event.description.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            event.mealRecommendation?.let { meal ->
                Text("Рекомендованная еда", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(meal.title, fontWeight = FontWeight.SemiBold)
                meal.restaurant?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                meal.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                val nutrition = listOfNotNull(
                    meal.estimatedCalories?.let { "$it ккал" },
                    meal.estimatedProteinG?.let { "белок $it г" },
                    meal.estimatedFatG?.let { "жиры $it г" },
                    meal.estimatedCarbsG?.let { "углеводы $it г" },
                    meal.priceRub?.let { "$it ₽" },
                    meal.weightG?.let { "$it г" },
                ).joinToString(" · ")
                if (nutrition.isNotBlank()) Text(nutrition, color = MaterialTheme.colorScheme.onSurfaceVariant)
                meal.fallback?.let { Text("Если недоступно: $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                meal.foodUrl?.let { url ->
                    Button(
                        modifier = Modifier.fillMaxWidth().testTag("openFoodUrlButton"),
                        onClick = { uriHandler.openUri(url) },
                    ) {
                        Text("Открыть блюдо")
                    }
                }
            }
            if (event.type == ScheduleEventType.Workout && event.exercises.isNotEmpty()) {
                Text("Упражнения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                event.exercises.forEach { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onOpen = { selectedExercise.value = exercise },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    selectedExercise.value?.let { exercise ->
        ModalBottomSheet(
            onDismissRequest = { selectedExercise.value = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ExerciseDetailsContent(
                exercise = exercise,
                onOpenUrl = uriHandler::openUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            )
        }
    }
}

@Composable
private fun ExerciseDetailsContent(
    exercise: WorkoutExerciseUi,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("exerciseDetailsSheet"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(exercise.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(exercise.shortLoadText(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        exercise.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (exercise.target.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.target.forEach { target -> AssistChip(onClick = {}, label = { Text(target) }) }
            }
        }
        ExerciseListSection("Техника", exercise.techniqueSteps)
        ExerciseListSection("Частые ошибки", exercise.commonMistakes)
        exercise.sourceUrl?.let { url ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenUrl(url) },
            ) {
                Text("Открыть источник")
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: WorkoutExerciseUi, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exerciseCard_${exercise.exerciseId}")
                .semantics {
                    onClick {
                        onOpen()
                        true
                    }
                }
                .clickable(onClick = onOpen)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(exercise.title, fontWeight = FontWeight.SemiBold)
            exercise.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(exercise.shortLoadText(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExerciseListSection(title: String, values: List<String>) {
    if (values.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        values.forEachIndexed { index, value ->
            Text("${index + 1}. $value", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun WorkoutExerciseUi.shortLoadText(): String = listOfNotNull(
    sets?.let { sets -> reps?.let { "$sets x $it" } ?: "${sets} подхода" },
    durationSec?.let { "$it сек" },
    restSec?.let { "отдых $it сек" },
    rpe?.let { "RPE $it" },
).joinToString(" · ").ifBlank { "Описание нагрузки не задано" }
