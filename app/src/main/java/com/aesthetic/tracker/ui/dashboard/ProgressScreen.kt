@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aesthetic.tracker.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.data.ImportedGoal
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.ui.AestheticAction
import com.aesthetic.tracker.ui.AestheticState
import com.aesthetic.tracker.ui.theme.AestheticTrackerTheme
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    val showImportSheet = remember { mutableStateOf(false) }
    val importCollapsed = rememberSaveable { mutableStateOf(false) }
    val progress = state.dashboard.progress
    val latestMeasurement = state.measurements.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("screen_Progress"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ImportAndChatGptCard(
                importUiState = state.importUiState,
                collapsed = importCollapsed.value,
                onCollapsedChange = { importCollapsed.value = it },
                onOpen = { showImportSheet.value = true },
                onClearImportedSchedule = { onAction(AestheticAction.ClearImportedSchedule) },
                onDismissStatus = { onAction(AestheticAction.DismissImportStatus) },
            )
        }
        state.importedGoal?.let { goal ->
            item { GoalProgressCard(goal = goal, latestMeasurement = latestMeasurement) }
        }
        item { ProgressSummaryCard(progress) }
        item { WeekProgressCard(progress) }
        item { CategoryProgressCard(progress) }
        item { SectionTitle("Последние замеры") }
        items(state.measurements.take(5)) { measurement -> MeasurementSummaryCard(measurement, onAction) }
        item { SectionTitle("История импорта") }
        items(state.scaleImports) { scaleImport -> ScaleImportHistoryCard(scaleImport, onAction) }
    }

    if (showImportSheet.value) {
        ImportBottomSheet(
            state = state,
            onAction = onAction,
            onDismiss = { showImportSheet.value = false },
        )
    }
}

@Composable
internal fun ImportAndChatGptCard(
    importUiState: ImportUiState,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onClearImportedSchedule: () -> Unit,
    onDismissStatus: () -> Unit,
) {
    DashboardCard(elevated = true) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Импорт и ChatGPT",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                modifier = Modifier.size(36.dp).testTag("importCardCollapseButton"),
                onClick = { onCollapsedChange(!collapsed) },
            ) {
                Icon(
                    imageVector = if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (collapsed) "Развернуть" else "Свернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!collapsed) {
            Text("Откройте один лист, чтобы подготовить данные в ChatGPT, вставить JSON, выбрать JSON-файл или загрузить скриншот весов.")
            when (importUiState) {
                ImportUiState.Idle -> Text("Готово к импорту.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ImportUiState.Loading -> Text("Импортируем...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                is ImportUiState.Success -> AssistChip(onClick = onDismissStatus, label = { Text(importUiState.message) })
                is ImportUiState.Error -> AssistChip(onClick = onDismissStatus, label = { Text(importUiState.message) })
            }
            Button(modifier = Modifier.fillMaxWidth().testTag("openImportSheetButton"), onClick = onOpen) {
                Text("Открыть импорт и ChatGPT")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().testTag("clearImportedScheduleButton"),
                onClick = onClearImportedSchedule,
            ) {
                Text("Сбросить импортированное расписание")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportBottomSheet(state: AestheticState, onAction: (AestheticAction) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var json by remember { mutableStateOf("") }
    val jsonFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            readTextFile(context, it)?.let { fileText ->
                onAction(AestheticAction.SubmitJsonImport(fileText))
                onDismiss()
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            copyScaleScreenshot(context, it)?.let { path ->
                onAction(
                    AestheticAction.SaveScaleImport(
                        ScaleScreenshotImport(
                            id = UUID.randomUUID().toString(),
                            createdAtEpochMillis = System.currentTimeMillis(),
                            localPath = path,
                        ),
                    ),
                )
                onDismiss()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        ImportSheetContent(
            importUiState = state.importUiState,
            json = json,
            onJsonChange = { json = it },
            onSubmitJson = { onAction(AestheticAction.SubmitJsonImport(json)) },
            onPickJsonFile = { jsonFilePicker.launch("*/*") },
            onPickImage = { imagePicker.launch("image/*") },
            onOpenChatGpt = { uriHandler.openUri("https://chatgpt.com/") },
            onImportStarted = onDismiss,
        )
    }
}

@Composable
internal fun ImportSheetContent(
    importUiState: ImportUiState,
    json: String,
    onJsonChange: (String) -> Unit,
    onSubmitJson: () -> Unit,
    onPickJsonFile: () -> Unit,
    onPickImage: () -> Unit,
    onOpenChatGpt: () -> Unit,
    onImportStarted: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("importBottomSheet")
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Импорт и ChatGPT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Откройте ChatGPT, чтобы подготовить JSON, затем вставьте его вручную или выберите файл.")
        Button(
            modifier = Modifier.fillMaxWidth().testTag("openChatGptButton"),
            onClick = onOpenChatGpt,
        ) {
            Text("Открыть ChatGPT")
        }
        OutlinedTextField(
            value = json,
            onValueChange = onJsonChange,
            modifier = Modifier.fillMaxWidth().testTag("measurementJsonInput"),
            minLines = 5,
            label = { Text("JSON замера или расписания") },
            placeholder = { Text("""{"weightKg":70.1,"bodyFatPercent":17.8,"pulse":86}""") },
        )
        Button(
            modifier = Modifier.fillMaxWidth().testTag("importJsonButton"),
            onClick = {
                onSubmitJson()
                onImportStarted()
            },
            enabled = importUiState != ImportUiState.Loading,
        ) {
            Text("Импортировать JSON")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().testTag("importJsonFileButton"),
            onClick = {
                onPickJsonFile()
            },
        ) {
            Text("Импортировать из файла")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().testTag("importImageButton"),
            onClick = {
                onPickImage()
            },
        ) {
            Text("Загрузить скриншот весов")
        }
        when (importUiState) {
            ImportUiState.Idle -> Text("Ожидание", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ImportUiState.Loading -> Text("Загружаем данные импорта...", color = MaterialTheme.colorScheme.primary)
            is ImportUiState.Success -> Text(importUiState.message, color = MaterialTheme.colorScheme.primary)
            is ImportUiState.Error -> Text(importUiState.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun ProgressSummaryCard(progress: ProgressDashboardUi) {
    DashboardCard {
        Text("Общие результаты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(percent = progress.weeklyProgressPercent, modifier = Modifier.size(86.dp))
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Неделя закрыта на ${progress.weeklyProgressPercent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(progress.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Средний день", "${progress.averageCompletionPercent}%")
            MetricTile("Успешных дней", progress.successfulDays.toString())
            MetricTile("Серия", "${progress.streakDays} дн.")
            MetricTile("Выполнено", progress.completedEvents.toString())
            MetricTile("Пропущено", progress.missedEvents.toString())
        }
    }
}

@Composable
private fun GoalProgressCard(goal: ImportedGoal, latestMeasurement: MeasurementEntry?) {
    val weightPercent = closenessPercent(latestMeasurement?.weightKg, goal.targetWeightKg)
    val bodyFatPercent = bodyFatGoalPercent(latestMeasurement?.bodyFatPercent, goal.targetBodyFatPercentRange)

    DashboardCard(modifier = Modifier.testTag("goalProgressCard"), elevated = true) {
        Text("Конечная цель", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(goal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        goal.visualReference?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Текущее состояние", fontWeight = FontWeight.SemiBold)
            GoalMetricProgress(
                title = "Вес",
                current = latestMeasurement?.weightKg?.formatKg() ?: "нет замера",
                target = goal.targetWeightKg?.formatKg() ?: "не задана",
                progressPercent = weightPercent,
            )
            GoalMetricProgress(
                title = "Жир",
                current = latestMeasurement?.bodyFatPercent?.formatPercent() ?: "нет замера",
                target = goal.targetBodyFatPercentRange?.let { "$it%" } ?: "не задана",
                progressPercent = bodyFatPercent,
            )
        }
        GoalChipGroup("Фокус", goal.focusMuscles)
        GoalChipGroup("Тренировки", goal.trainingPrinciples.take(4))
        GoalChipGroup("Питание", goal.nutritionPrinciples.take(4))
    }
}

@Composable
private fun GoalMetricProgress(title: String, current: String, target: String, progressPercent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title: $current / цель $target")
        LinearProgressIndicator(
            progress = { progressPercent.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeekProgressCard(progress: ProgressDashboardUi) {
    DashboardCard {
        Text("Прогресс недели", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (progress.week.isEmpty()) {
            Text("Данных по неделе пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                progress.week.forEach { day ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(day.dayOfWeek, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            ProgressRing(percent = day.progressPercent, modifier = Modifier.size(50.dp))
                            Text("${day.completedCount}/${day.totalCount}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressCard(progress: ProgressDashboardUi) {
    DashboardCard {
        Text("Категории", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (progress.categories.isEmpty()) {
            Text("Данных по категориям пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                progress.categories.forEach { category ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProgressRing(percent = category.progressPercent, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(category.title, fontWeight = FontWeight.SemiBold)
                                Text("${category.completed}/${category.total}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalChipGroup(title: String, values: List<String>) {
    if (values.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value -> AssistChip(onClick = {}, label = { Text(value) }) }
        }
    }
}

@Composable
private fun MeasurementSummaryCard(entry: MeasurementEntry, onAction: (AestheticAction) -> Unit) {
    DashboardCard {
        Text(entry.date.toString(), fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Вес ${entry.weightKg.formatKg()}") })
            AssistChip(onClick = {}, label = { Text("Жир ${entry.bodyFatPercent.formatPercent()}") })
            AssistChip(onClick = {}, label = { Text("Мышцы ${entry.skeletalMuscleKg.formatKg()}") })
            AssistChip(onClick = {}, label = { Text("Пульс ${entry.pulse}") })
        }
        Button(onClick = { onAction(AestheticAction.DeleteMeasurement(entry)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Удалить замер")
        }
    }
}

@Composable
private fun ScaleImportHistoryCard(scaleImport: ScaleScreenshotImport, onAction: (AestheticAction) -> Unit) {
    DashboardCard {
        Text("Локальный скриншот", fontWeight = FontWeight.Bold)
        Text(scaleImport.localPath, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scaleImport.isParsed) {
            Text("Распознанные значения готовы к подтверждению.")
            Button(onClick = { onAction(AestheticAction.ConfirmParsedScaleImport(scaleImport)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Подтвердить замер")
            }
        } else {
            Button(onClick = { onAction(AestheticAction.ParseScaleImport(scaleImport)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Распознать скриншот")
            }
        }
        OutlinedButton(onClick = { onAction(AestheticAction.DeleteScaleImport(scaleImport)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Удалить скриншот")
        }
    }
}

private fun closenessPercent(current: Double?, target: Double?): Int {
    if (current == null || target == null || target <= 0.0) return 0
    return (100 - (abs(current - target) / target * 100)).roundToInt().coerceIn(0, 100)
}

private fun bodyFatGoalPercent(current: Double?, range: String?): Int {
    if (current == null || range.isNullOrBlank()) return 0
    val bounds = range.split("-", "–").mapNotNull { it.trim().toDoubleOrNull() }
    if (bounds.isEmpty()) return 0
    val upper = bounds.maxOrNull() ?: return 0
    if (current <= upper) return 100
    return (100 - ((current - upper) / current * 100)).roundToInt().coerceIn(0, 100)
}

private val RussianLocale: Locale = Locale.forLanguageTag("ru")

private fun Double.formatKg(): String = String.format(RussianLocale, "%.1f кг", this)

private fun Double.formatPercent(): String = String.format(RussianLocale, "%.1f%%", this)

private fun readTextFile(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
}.getOrNull()

private fun copyScaleScreenshot(context: Context, uri: Uri): String? {
    val dir = File(context.filesDir, "scale-screenshots").apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg")
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
private fun ProgressScreenPreview() {
    AestheticTrackerTheme {
        ProgressScreen(
            state = AestheticState(isLoading = false).let { it.copy(dashboard = DashboardUiMapper.map(it)) },
            onAction = {},
        )
    }
}
