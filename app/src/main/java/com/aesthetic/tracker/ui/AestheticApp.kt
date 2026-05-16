@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aesthetic.tracker.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.R
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.RecommendationPriority
import com.aesthetic.tracker.data.ScaleScreenshotImport
import com.aesthetic.tracker.domain.PlanTargets
import com.aesthetic.tracker.domain.format
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AestheticApp(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TrackerScreen.entries.forEach { screen ->
                    val label = stringResource(screen.labelRes)
                    NavigationBarItem(
                        selected = state.selectedScreen == screen,
                        onClick = { onAction(AestheticAction.SelectScreen(screen)) },
                        icon = { Icon(screen.icon(), contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Crossfade(targetState = state.selectedScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        TrackerScreen.GeneralData -> DashboardScreen(state, onAction)
                        TrackerScreen.Today -> TodayPlanScreen(state, onAction)
                        TrackerScreen.UploadResults -> MeasurementsScreen(state, onAction)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeaderCard(
                title = stringResource(R.string.week_day_title, state.planPosition.week, state.planPosition.day),
                subtitle = stringResource(R.string.plan_progress_subtitle, (state.planPosition.percentComplete * 100).toDouble().format(0)),
            ) {
                LinearProgressIndicator(
                    progress = { state.planPosition.percentComplete },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SectionTitle(stringResource(R.string.progress_from_baseline))
            MetricGrid(
                metrics = listOf(
                    stringResource(R.string.metric_weight) to "${state.progress.weightChangeKg.format(1)} kg",
                    stringResource(R.string.metric_body_fat) to "${state.progress.bodyFatChangePercent.format(1)}%",
                    stringResource(R.string.metric_muscle) to "+${state.progress.skeletalMuscleChangeKg.format(1)} kg".replace("+-", "-"),
                    stringResource(R.string.metric_pulse) to "${state.progress.pulseChange} bpm",
                ),
            )
        }
        item {
            SectionTitle(stringResource(R.string.ai_settings))
            AiSettingsCard(state, onAction)
        }
        item {
            SectionTitle(stringResource(R.string.target_range))
            InfoCard {
                Text(stringResource(R.string.target_weight, PlanTargets.TargetMinWeightKg.format(0), PlanTargets.TargetMaxWeightKg.format(0)))
                Text(stringResource(R.string.target_body_fat, PlanTargets.TargetMinBodyFatPercent.format(0), PlanTargets.TargetMaxBodyFatPercent.format(0)))
                Text(stringResource(R.string.target_skeletal_muscle, PlanTargets.TargetMuscleGainMinKg.format(0), PlanTargets.TargetMuscleGainMaxKg.format(0)))
                Text(stringResource(R.string.target_pulse, PlanTargets.InitialPulse))
            }
        }
        item {
            SectionTitle(stringResource(R.string.todays_habits))
            HabitSummary(state.currentHabit)
        }
        item {
            SectionTitle(stringResource(R.string.latest_measurement))
            state.measurements.firstOrNull()?.let { MeasurementCard(it) } ?: EmptyCard(stringResource(R.string.empty_measurements))
        }
    }
}

@Composable
private fun AiSettingsCard(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    var chatUrl by remember(state.chatUrl) { mutableStateOf(state.chatUrl) }
    InfoCard {
        Text(stringResource(R.string.chat_url_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = chatUrl,
            onValueChange = { chatUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.chat_url)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
        )
        Button(
            onClick = { onAction(AestheticAction.SaveChatUrl(chatUrl)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.save_chat_url))
        }
    }
}

@Composable
fun MeasurementsScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestMeasurement = state.measurements.firstOrNull()
    var weight by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.weightKg?.toString() ?: "70.4") }
    var bodyFat by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.bodyFatPercent?.toString() ?: "18.0") }
    var muscle by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.skeletalMuscleKg?.toString() ?: "29.0") }
    var pulse by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.pulse?.toString() ?: "102") }
    var visceral by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.visceralFat?.toString() ?: "8") }
    var water by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.waterPercent?.toString() ?: "55.0") }
    var bodyScore by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.bodyScore?.toString().orEmpty()) }
    var bmi by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.bmi?.toString().orEmpty()) }
    var fatMass by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.fatMassKg?.toString().orEmpty()) }
    var muscleMass by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.muscleMassKg?.toString().orEmpty()) }
    var muscleRate by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.muscleRatePercent?.toString().orEmpty()) }
    var bodyWater by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.bodyWaterKg?.toString().orEmpty()) }
    var mineralMass by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.mineralMassKg?.toString().orEmpty()) }
    var proteinMass by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.proteinMassKg?.toString().orEmpty()) }
    var proteinPercent by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.proteinPercent?.toString().orEmpty()) }
    var subcutaneousFat by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.subcutaneousFatPercent?.toString().orEmpty()) }
    var leanMass by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.leanBodyMassKg?.toString().orEmpty()) }
    var bmr by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.basalMetabolismKcal?.toString().orEmpty()) }
    var biologicalAge by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.biologicalAge?.toString().orEmpty()) }
    var bodyType by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.bodyType.orEmpty()) }
    var standardWeight by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.standardWeightKg?.toString().orEmpty()) }
    var weightControl by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.weightControlKg?.toString().orEmpty()) }
    var fatControl by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.fatControlKg?.toString().orEmpty()) }
    var muscleControl by remember(latestMeasurement?.date) { mutableStateOf(latestMeasurement?.muscleControlKg?.toString().orEmpty()) }
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
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeaderCard(stringResource(R.string.upload_results_overview), stringResource(R.string.upload_results_subtitle)) {
                latestMeasurement?.let { MeasurementCard(it) } ?: Text(stringResource(R.string.empty_measurements), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionTitle(stringResource(R.string.smart_scale_import))
            InfoCard {
                Text(stringResource(R.string.smart_scale_privacy))
                Button(modifier = Modifier.fillMaxWidth(), onClick = { imagePicker.launch("image/*") }) {
                    Text(stringResource(R.string.upload_scale_screenshot))
                }
            }
        }
        items(state.scaleImports) { scaleImport -> ScaleImportCard(scaleImport, onAction) }
        item {
            SectionTitle(stringResource(R.string.manual_measurement))
            InfoCard {
                NumberField(stringResource(R.string.field_weight_kg), weight) { weight = it }
                NumberField(stringResource(R.string.field_body_fat_percent), bodyFat) { bodyFat = it }
                NumberField(stringResource(R.string.field_skeletal_muscle_kg), muscle) { muscle = it }
                NumberField(stringResource(R.string.field_resting_pulse), pulse) { pulse = it }
                NumberField(stringResource(R.string.field_visceral_fat), visceral) { visceral = it }
                NumberField(stringResource(R.string.field_water_percent), water) { water = it }
                NumberField(stringResource(R.string.field_body_score_optional), bodyScore) { bodyScore = it }
                NumberField(stringResource(R.string.field_bmi_optional), bmi) { bmi = it }
                NumberField(stringResource(R.string.field_fat_mass_kg_optional), fatMass) { fatMass = it }
                NumberField(stringResource(R.string.field_muscle_mass_kg_optional), muscleMass) { muscleMass = it }
                NumberField(stringResource(R.string.field_muscle_rate_percent_optional), muscleRate) { muscleRate = it }
                NumberField(stringResource(R.string.field_body_water_kg_optional), bodyWater) { bodyWater = it }
                NumberField(stringResource(R.string.field_mineral_mass_kg_optional), mineralMass) { mineralMass = it }
                NumberField(stringResource(R.string.field_protein_mass_kg_optional), proteinMass) { proteinMass = it }
                NumberField(stringResource(R.string.field_protein_percent_optional), proteinPercent) { proteinPercent = it }
                NumberField(stringResource(R.string.field_subcutaneous_fat_percent_optional), subcutaneousFat) { subcutaneousFat = it }
                NumberField(stringResource(R.string.field_lean_body_mass_kg_optional), leanMass) { leanMass = it }
                NumberField(stringResource(R.string.field_bmr_optional), bmr) { bmr = it }
                NumberField(stringResource(R.string.field_biological_age_optional), biologicalAge) { biologicalAge = it }
                OutlinedTextField(
                    value = bodyType,
                    onValueChange = { bodyType = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.field_body_type_optional)) },
                    singleLine = true,
                )
                NumberField(stringResource(R.string.field_standard_weight_kg_optional), standardWeight) { standardWeight = it }
                NumberField(stringResource(R.string.field_weight_control_kg_optional), weightControl) { weightControl = it }
                NumberField(stringResource(R.string.field_fat_control_kg_optional), fatControl) { fatControl = it }
                NumberField(stringResource(R.string.field_muscle_control_kg_optional), muscleControl) { muscleControl = it }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onAction(
                            AestheticAction.SaveMeasurement(
                                weightKg = weight.metricDoubleOrNull() ?: PlanTargets.InitialWeightKg,
                                bodyFatPercent = bodyFat.metricDoubleOrNull() ?: PlanTargets.InitialBodyFatPercent,
                                skeletalMuscleKg = muscle.metricDoubleOrNull() ?: PlanTargets.InitialSkeletalMuscleKg,
                                pulse = pulse.metricIntOrNull() ?: PlanTargets.InitialPulse,
                                visceralFat = visceral.metricIntOrNull() ?: 0,
                                waterPercent = water.metricDoubleOrNull() ?: 0.0,
                                bodyScore = bodyScore.metricIntOrNull(),
                                bmi = bmi.metricDoubleOrNull(),
                                fatMassKg = fatMass.metricDoubleOrNull(),
                                muscleMassKg = muscleMass.metricDoubleOrNull(),
                                muscleRatePercent = muscleRate.metricDoubleOrNull(),
                                bodyWaterKg = bodyWater.metricDoubleOrNull(),
                                mineralMassKg = mineralMass.metricDoubleOrNull(),
                                proteinMassKg = proteinMass.metricDoubleOrNull(),
                                proteinPercent = proteinPercent.metricDoubleOrNull(),
                                subcutaneousFatPercent = subcutaneousFat.metricDoubleOrNull(),
                                leanBodyMassKg = leanMass.metricDoubleOrNull(),
                                basalMetabolismKcal = bmr.metricIntOrNull(),
                                biologicalAge = biologicalAge.metricIntOrNull(),
                                bodyType = bodyType,
                                standardWeightKg = standardWeight.metricDoubleOrNull(),
                                weightControlKg = weightControl.metricDoubleOrNull(),
                                fatControlKg = fatControl.metricDoubleOrNull(),
                                muscleControlKg = muscleControl.metricDoubleOrNull(),
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.save_check_in)) }
            }
        }
        item { SectionTitle(stringResource(R.string.history)) }
        items(state.measurements) { entry -> MeasurementCard(entry, onAction) }
        item { SectionTitle(stringResource(R.string.result_recommendations)) }
        items(state.recommendations) { recommendation -> RecommendationCard(recommendation) }
    }
}

@Composable
private fun ScaleImportCard(scaleImport: ScaleScreenshotImport, onAction: (AestheticAction) -> Unit) {
    InfoCard {
        Text(stringResource(R.string.local_screenshot), fontWeight = FontWeight.Bold)
        Text(scaleImport.localPath, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scaleImport.isParsed) {
            Text(stringResource(R.string.parsed_values), fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParsedChip(stringResource(R.string.parsed_weight), scaleImport.parsedWeightKg?.let { "${it.format(1)} kg" })
                ParsedChip(stringResource(R.string.parsed_body_fat), scaleImport.parsedBodyFatPercent?.let { "${it.format(1)}%" })
                ParsedChip(stringResource(R.string.parsed_bmi), scaleImport.parsedBmi?.let { it.format(1) })
                ParsedChip(stringResource(R.string.parsed_muscle_mass), scaleImport.parsedMuscleMassKg?.let { "${it.format(1)} kg" })
                ParsedChip(stringResource(R.string.parsed_skeletal_muscle), scaleImport.parsedSkeletalMuscleKg?.let { "${it.format(1)} kg" })
                ParsedChip(stringResource(R.string.parsed_water), scaleImport.parsedWaterPercent?.let { "${it.format(1)}%" })
                ParsedChip(stringResource(R.string.parsed_protein), scaleImport.parsedProteinPercent?.let { "${it.format(1)}%" })
                ParsedChip(stringResource(R.string.parsed_visceral_fat), scaleImport.parsedVisceralFat?.toString())
                ParsedChip(stringResource(R.string.parsed_bmr), scaleImport.parsedBasalMetabolismKcal?.let { "$it kcal" })
                ParsedChip(stringResource(R.string.parsed_bio_age), scaleImport.parsedBiologicalAge?.toString())
                ParsedChip(stringResource(R.string.parsed_pulse), scaleImport.parsedPulse?.let { "$it bpm" })
            }
            Button(onClick = { onAction(AestheticAction.ConfirmParsedScaleImport(scaleImport)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.confirm_parsed_entry))
            }
        } else {
            Button(onClick = { onAction(AestheticAction.ParseScaleImport(scaleImport)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.send_to_ai_parser))
            }
        }
        Button(onClick = { onAction(AestheticAction.DeleteScaleImport(scaleImport)) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete_local_photo))
        }
    }
}

@Composable
private fun ParsedChip(label: String, value: String?) {
    AssistChip(onClick = {}, label = { Text(stringResource(R.string.parsed_chip, label, value ?: stringResource(R.string.parsed_unknown))) })
}

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

@Composable
private fun HeaderCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.animateContentSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun MetricGrid(metrics: List<Pair<String, String>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.forEach { (label, value) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HabitSummary(habit: HabitEntry) {
    val done = HabitKind.entries.count { habit.isDone(it) }
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(progress = { done / HabitKind.entries.size.toFloat() }, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.habit_complete_count, done, HabitKind.entries.size), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.habit_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MeasurementCard(entry: MeasurementEntry, onAction: ((AestheticAction) -> Unit)? = null) {
    InfoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.date.toString(), fontWeight = FontWeight.Bold)
            Text("${entry.weightKg.format(1)} kg", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entry.bodyScore?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.body_score_chip, it)) }) }
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.fat_chip, entry.bodyFatPercent.format(1))) })
            entry.fatMassKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.fat_mass_chip, it.format(1))) }) }
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.muscle_chip, entry.skeletalMuscleKg.format(1))) })
            entry.muscleMassKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.muscle_mass_chip, it.format(1))) }) }
            entry.muscleRatePercent?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.muscle_rate_chip, it.format(1))) }) }
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.pulse_chip, entry.pulse)) })
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.visceral_chip, entry.visceralFat)) })
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.water_chip, entry.waterPercent.format(1))) })
            entry.bodyWaterKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.body_water_kg_chip, it.format(1))) }) }
            entry.bmi?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.bmi_chip, it.format(1))) }) }
            entry.mineralMassKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.mineral_mass_chip, it.format(1))) }) }
            entry.proteinMassKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.protein_mass_chip, it.format(1))) }) }
            entry.proteinPercent?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.protein_chip, it.format(1))) }) }
            entry.subcutaneousFatPercent?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.subcutaneous_fat_chip, it.format(1))) }) }
            entry.leanBodyMassKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.lean_body_mass_chip, it.format(1))) }) }
            entry.basalMetabolismKcal?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.bmr_chip, it)) }) }
            entry.biologicalAge?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.bio_age_chip, it)) }) }
            entry.bodyType?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.body_type_chip, it)) }) }
            entry.standardWeightKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.standard_weight_chip, it.format(1))) }) }
            entry.weightControlKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.weight_control_chip, it.format(1))) }) }
            entry.fatControlKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.fat_control_chip, it.format(1))) }) }
            entry.muscleControlKg?.let { AssistChip(onClick = {}, label = { Text(stringResource(R.string.muscle_control_chip, it.format(1))) }) }
        }
        entry.scalePhotoPath?.let { Text(stringResource(R.string.photo_stored_locally, it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        onAction?.let {
            Button(onClick = { it(AestheticAction.DeleteMeasurement(entry)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete_parsed_entry)) }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: Recommendation) {
    val color = when (recommendation.priority) {
        RecommendationPriority.High -> MaterialTheme.colorScheme.error
        RecommendationPriority.Medium -> MaterialTheme.colorScheme.tertiary
        RecommendationPriority.Low -> MaterialTheme.colorScheme.primary
    }
    Card(
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.animateContentSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(recommendation.priority.label(), color = color, fontWeight = FontWeight.Bold)
            }
            Text(recommendation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(recommendation.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyCard(text: String) = InfoCard { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().animateContentSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

fun HabitEntry.isDone(kind: HabitKind): Boolean = when (kind) {
    HabitKind.Water -> waterDone
    HabitKind.Steps -> stepsDone
    HabitKind.Protein -> proteinDone
    HabitKind.Workout -> workoutDone
    HabitKind.Posture -> postureDone
    HabitKind.Sleep -> sleepDone
}

private fun String.metricDoubleOrNull(): Double? =
    replace(',', '.')
        .replace(Regex("[^0-9.+-]"), "")
        .takeIf { it.isNotBlank() && it != "+" && it != "-" && it != "." }
        ?.toDoubleOrNull()

private fun String.metricIntOrNull(): Int? = metricDoubleOrNull()?.toInt()

private fun TrackerScreen.icon(): ImageVector = when (this) {
    TrackerScreen.GeneralData -> Icons.Default.Insights
    TrackerScreen.Today -> Icons.Default.FitnessCenter
    TrackerScreen.UploadResults -> Icons.Default.MonitorHeart
}

@Composable
private fun RecommendationPriority.label(): String = when (this) {
    RecommendationPriority.High -> stringResource(R.string.priority_high)
    RecommendationPriority.Medium -> stringResource(R.string.priority_medium)
    RecommendationPriority.Low -> stringResource(R.string.priority_low)
}
