@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aesthetic.tracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.R
import com.aesthetic.tracker.data.DefaultChatUrl
import com.aesthetic.tracker.domain.FoodGoal
import com.aesthetic.tracker.domain.GeneratedTodayPlan
import com.aesthetic.tracker.domain.TodayScheduleItem
import com.aesthetic.tracker.domain.TodayScheduleKind
import com.aesthetic.tracker.domain.TodayTrainingItem
import com.aesthetic.tracker.domain.buildTodayPlanPrompt

@Composable
fun TodayPlanScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    val generatedPlan = state.generatedTodayPlan
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prompt = remember(state.todayPlanInput(), state.scaleImports) { buildManualChatPrompt(state) }
    val planFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            readTextFile(context, it)?.let { content ->
                onAction(AestheticAction.ImportTodayPlan(content))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TodayHeader(
                state = state,
                generatedPlan = generatedPlan,
                onCopyPrompt = { clipboard.setText(AnnotatedString(prompt)) },
                onOpenChat = { openChat(context, state.chatUrl) },
                onImportPlan = { planFilePicker.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream")) },
            )
        }
        item {
            FoodGoalSelector(state, onAction)
        }
        generatedPlan?.let { plan ->
            item {
                ScheduleTracker(
                    plan = plan,
                    completedIds = state.completedScheduleItemIds,
                    onToggle = { onAction(AestheticAction.ToggleScheduleItem(it)) },
                )
            }
            item { TrainingPlanBlockCard(stringResource(R.string.workout), plan.training) }
            item { TodayPlanBlockCard(stringResource(R.string.nutrition_today), plan.nutrition) }
            item { TodayPlanBlockCard(stringResource(R.string.recovery_today), plan.recovery) }
            item { TodayPlanBlockCard(stringResource(R.string.execution_checkpoints), plan.checkpoints) }
        } ?: item {
            FallbackWorkoutCard(state)
        }
        item { TodaySectionTitle(stringResource(R.string.daily_habit_checklist)) }
        items(HabitKind.entries) { habit ->
            TodayHabitRow(habit, state.currentHabit.isDone(habit)) {
                onAction(AestheticAction.ToggleHabit(habit))
            }
        }
    }
}

@Composable
private fun TodayHeader(
    state: AestheticState,
    generatedPlan: GeneratedTodayPlan?,
    onCopyPrompt: () -> Unit,
    onOpenChat: () -> Unit,
    onImportPlan: () -> Unit,
) {
    TodayCard(containerVariant = true) {
        Text(stringResource(R.string.todays_plan), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.week_day_short, state.planPosition.week, state.planPosition.day), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(generatedPlan?.focus ?: state.currentPlanDay?.focus ?: stringResource(R.string.fallback_focus), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.today_generate_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.todayPlanError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onCopyPrompt, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.copy_chat_prompt))
        }
        Button(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.open_chat))
        }
        Button(onClick = onImportPlan, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_schedule_file))
        }
    }
}

@Composable
private fun FoodGoalSelector(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TodaySectionTitle(stringResource(R.string.food_goal))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodGoal.entries.forEach { goal ->
                val label = stringResource(goal.labelRes)
                FilterChip(
                    selected = state.selectedFoodGoal == goal,
                    onClick = { onAction(AestheticAction.SelectFoodGoal(goal)) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun ScheduleTracker(
    plan: GeneratedTodayPlan,
    completedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TodaySectionTitle(stringResource(R.string.today_schedule))
        TodayCard {
            plan.schedule.forEach { item ->
                ScheduleItemRow(
                    item = item,
                    checked = item.id in completedIds,
                    onToggle = { onToggle(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ScheduleItemRow(item: TodayScheduleItem, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.kind.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.time.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(item.title, fontWeight = FontWeight.SemiBold)
            }
            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(
                onClick = {},
                label = { Text(stringResource(if (item.isFixed) R.string.today_schedule_fixed else R.string.today_schedule_generated)) },
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun TodayPlanBlockCard(title: String, lines: List<String>) {
    TodayCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        lines.forEachIndexed { index, line ->
            Text("${index + 1}. $line", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrainingPlanBlockCard(title: String, items: List<TodayTrainingItem>) {
    TodayCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        items.forEachIndexed { index, item ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${index + 1}. ${item.title}", fontWeight = FontWeight.SemiBold)
                if (item.description.isNotBlank()) {
                    Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FallbackWorkoutCard(state: AestheticState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TodaySectionTitle(state.currentPlanDay?.title ?: stringResource(R.string.workout))
        TodayCard {
            state.currentPlanDay?.exercises.orEmpty().forEachIndexed { index, exercise ->
                Text("${index + 1}. $exercise")
            }
            if (state.measurements.isEmpty()) {
                Text(stringResource(R.string.today_plan_needs_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TodayHabitRow(habit: HabitKind, checked: Boolean, onToggle: () -> Unit) {
    val label = stringResource(habit.labelRes)
    Card(
        onClick = onToggle,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(habit.icon(), contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun TodaySectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun TodayCard(containerVariant: Boolean = false, content: @Composable () -> Unit) {
    Card(
        border = if (containerVariant) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (containerVariant) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.fillMaxWidth().animateContentSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

private fun TodayScheduleKind.icon(): ImageVector = when (this) {
    TodayScheduleKind.Sleep -> Icons.Default.MonitorHeart
    TodayScheduleKind.Meal -> Icons.Default.Restaurant
    TodayScheduleKind.Workout -> Icons.Default.FitnessCenter
    TodayScheduleKind.Recovery -> Icons.Default.SelfImprovement
    TodayScheduleKind.CheckIn -> Icons.Default.Insights
    TodayScheduleKind.Habit -> Icons.Default.WaterDrop
}

private fun HabitKind.icon(): ImageVector = when (this) {
    HabitKind.Water -> Icons.Default.WaterDrop
    HabitKind.Steps -> Icons.AutoMirrored.Filled.DirectionsWalk
    HabitKind.Protein -> Icons.Default.Restaurant
    HabitKind.Workout -> Icons.Default.FitnessCenter
    HabitKind.Posture -> Icons.Default.SelfImprovement
    HabitKind.Sleep -> Icons.Default.MonitorHeart
}

private fun buildManualChatPrompt(state: AestheticState): String {
    val scaleHint = state.scaleImports.firstOrNull()?.let {
        "Картинка весов сохранена локально: ${it.localPath}. Я приложу ее в чат отдельным файлом/изображением, если нужно уточнить значения."
    } ?: "Картинки весов в приложении пока нет; используй последние ручные/распознанные замеры."
    return """
        ${buildTodayPlanPrompt(state.todayPlanInput())}

        $scaleHint

        Верни только JSON без Markdown. Формат недельного файла расписания и актуальных данных:
        {
          "weekStartDate": "${state.today}",
          "measurement": {
            "date": "${state.today}",
            "bodyScore": 79,
            "weightKg": 70.4,
            "bodyFatPercent": 18.0,
            "fatMassKg": 12.7,
            "skeletalMuscleKg": 29.0,
            "muscleMassKg": 54.7,
            "muscleRatePercent": 77.7,
            "pulse": 102,
            "visceralFat": 7,
            "waterPercent": 56.8,
            "bodyWaterKg": 40.0,
            "bmi": 22.0,
            "mineralMassKg": 3.0,
            "proteinMassKg": 14.0,
            "proteinPercent": 19.9,
            "subcutaneousFatPercent": 4.3,
            "leanBodyMassKg": 57.7,
            "basalMetabolismKcal": 1616,
            "biologicalAge": 21,
            "bodyType": "Спортивное",
            "standardWeightKg": 69.3,
            "weightControlKg": -1.1,
            "fatControlKg": -2.3,
            "muscleControlKg": 1.2
          },
          "days": [
            {
              "date": "${state.today}",
              "focus": "строка",
              "schedule": [
                {
                  "id": "sleep-start",
                  "time": "02:00",
                  "title": "Сон 02:00-10:00",
                  "description": "что делать",
                  "kind": "Sleep",
                  "isFixed": true,
                  "notificationText": "текст уведомления"
                }
              ],
              "training": [
                {
                  "title": "Жим гантелей лежа",
                  "description": "4x8-10, отдых 90 сек, RPE 7. Техника: лопатки сведены, движение контролируемое, без отбива."
                }
              ],
              "nutrition": ["строка"],
              "recovery": ["строка"],
              "checkpoints": ["строка"]
            }
          ]
        }

        Нужны 7 дней подряд начиная с ${state.today}. Приложение импортирует из файла текущий день по полю date.
        measurement необязательный: если картинки весов нет или значение не видно, не добавляй это поле или пропусти конкретный ключ.
        training должен быть массивом объектов: title = упражнение или блок, description = подходы x повторы или длительность, отдых, RPE/интенсивность и короткая подсказка по технике.
        Обязательные fixed anchors каждый день: сон 02:00-10:00, завтрак 12:00, обед 15:00, ужин 20:00.
        kind строго один из: Sleep, Meal, Workout, Recovery, CheckIn, Habit.
    """.trimIndent()
}

private fun openChat(context: Context, url: String) {
    val safeUrl = url.normalizedChatUrl()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
    context.startActivity(intent)
}

private fun String.normalizedChatUrl(): String {
    val value = trim().ifBlank { DefaultChatUrl }
    return if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
}

private fun readTextFile(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }.getOrNull()
