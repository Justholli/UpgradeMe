@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aesthetic.tracker.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aesthetic.tracker.data.HabitEntry
import com.aesthetic.tracker.data.MeasurementEntry
import com.aesthetic.tracker.data.Recommendation
import com.aesthetic.tracker.data.RecommendationPriority
import com.aesthetic.tracker.domain.PlanTargets
import com.aesthetic.tracker.domain.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AestheticApp(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aesthetic Tracker", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TrackerScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = state.selectedScreen == screen,
                        onClick = { onAction(AestheticAction.SelectScreen(screen)) },
                        icon = { Icon(screen.icon(), contentDescription = screen.label) },
                        label = { Text(screen.label) },
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
                when (state.selectedScreen) {
                    TrackerScreen.Dashboard -> DashboardScreen(state)
                    TrackerScreen.TodayPlan -> TodayPlanScreen(state, onAction)
                    TrackerScreen.Measurements -> MeasurementsScreen(state, onAction)
                    TrackerScreen.Recommendations -> RecommendationsScreen(state)
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(state: AestheticState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeaderCard(
                title = "Week ${state.planPosition.week}, Day ${state.planPosition.day}",
                subtitle = "12-week recomposition plan • ${(state.planPosition.percentComplete * 100).format(0)}% complete",
            ) {
                LinearProgressIndicator(
                    progress = { state.planPosition.percentComplete },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SectionTitle("Progress from baseline")
            MetricGrid(
                metrics = listOf(
                    "Weight" to "${state.progress.weightChangeKg.format(1)} kg",
                    "Body fat" to "${state.progress.bodyFatChangePercent.format(1)}%",
                    "Muscle" to "+${state.progress.skeletalMuscleChangeKg.format(1)} kg".replace("+-", "-"),
                    "Pulse" to "${state.progress.pulseChange} bpm",
                ),
            )
        }
        item {
            SectionTitle("Target range")
            InfoCard {
                Text("Weight: ${PlanTargets.TargetMinWeightKg.format(0)}-${PlanTargets.TargetMaxWeightKg.format(0)} kg")
                Text("Body fat: ${PlanTargets.TargetMinBodyFatPercent.format(0)}-${PlanTargets.TargetMaxBodyFatPercent.format(0)}%")
                Text("Skeletal muscle: +${PlanTargets.TargetMuscleGainMinKg.format(0)}-${PlanTargets.TargetMuscleGainMaxKg.format(0)} kg")
                Text("Pulse: lower resting trend from ${PlanTargets.InitialPulse} bpm")
            }
        }
        item {
            SectionTitle("Today's habits")
            HabitSummary(state.currentHabit)
        }
        item {
            SectionTitle("Latest measurement")
            state.measurements.firstOrNull()?.let { MeasurementCard(it) } ?: EmptyCard("No measurements yet. Add today's check-in to begin tracking trends.")
        }
    }
}

@Composable
fun TodayPlanScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeaderCard("Today's plan", "Week ${state.planPosition.week} • Day ${state.planPosition.day}") {
                Text(state.currentPlanDay?.focus ?: "Build consistency", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionTitle(state.currentPlanDay?.title ?: "Workout")
            InfoCard {
                state.currentPlanDay?.exercises.orEmpty().forEachIndexed { index, exercise ->
                    Text("${index + 1}. $exercise")
                }
            }
        }
        item { SectionTitle("Daily habit checklist") }
        items(HabitKind.entries) { habit ->
            HabitRow(habit, state.currentHabit.isDone(habit)) {
                onAction(AestheticAction.ToggleHabit(habit))
            }
        }
    }
}

@Composable
fun MeasurementsScreen(state: AestheticState, onAction: (AestheticAction) -> Unit) {
    var weight by remember { mutableStateOf(state.measurements.firstOrNull()?.weightKg?.toString() ?: "70.4") }
    var bodyFat by remember { mutableStateOf(state.measurements.firstOrNull()?.bodyFatPercent?.toString() ?: "18.0") }
    var muscle by remember { mutableStateOf(state.measurements.firstOrNull()?.skeletalMuscleKg?.toString() ?: "29.0") }
    var pulse by remember { mutableStateOf(state.measurements.firstOrNull()?.pulse?.toString() ?: "102") }
    var visceral by remember { mutableStateOf(state.measurements.firstOrNull()?.visceralFat?.toString() ?: "8") }
    var water by remember { mutableStateOf(state.measurements.firstOrNull()?.waterPercent?.toString() ?: "55.0") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("Today's measurement")
            InfoCard {
                NumberField("Weight (kg)", weight) { weight = it }
                NumberField("Body fat (%)", bodyFat) { bodyFat = it }
                NumberField("Skeletal muscle (kg)", muscle) { muscle = it }
                NumberField("Resting pulse (bpm)", pulse) { pulse = it }
                NumberField("Visceral fat", visceral) { visceral = it }
                NumberField("Water (%)", water) { water = it }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onAction(
                            AestheticAction.SaveMeasurement(
                                weightKg = weight.toDoubleOrNull() ?: PlanTargets.InitialWeightKg,
                                bodyFatPercent = bodyFat.toDoubleOrNull() ?: PlanTargets.InitialBodyFatPercent,
                                skeletalMuscleKg = muscle.toDoubleOrNull() ?: PlanTargets.InitialSkeletalMuscleKg,
                                pulse = pulse.toIntOrNull() ?: PlanTargets.InitialPulse,
                                visceralFat = visceral.toIntOrNull() ?: 0,
                                waterPercent = water.toDoubleOrNull() ?: 0.0,
                            ),
                        )
                    },
                ) { Text("Save check-in") }
            }
        }
        item { SectionTitle("History") }
        items(state.measurements) { entry -> MeasurementCard(entry) }
    }
}

@Composable
fun RecommendationsScreen(state: AestheticState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeaderCard("Coach recommendations", "Generated from pulse, fat loss rate, body-fat trend, and muscle trend") {
                Text("Update measurements weekly for more accurate trend advice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.recommendations) { recommendation -> RecommendationCard(recommendation) }
    }
}

@Composable
private fun HeaderCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
                Text("$done/${HabitKind.entries.size} complete", fontWeight = FontWeight.Bold)
                Text("Water • Steps • Protein • Workout • Posture • Sleep", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HabitRow(habit: HabitKind, checked: Boolean, onToggle: () -> Unit) {
    Card(onClick = onToggle, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(habit.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(habit.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun MeasurementCard(entry: MeasurementEntry) {
    InfoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.date.toString(), fontWeight = FontWeight.Bold)
            Text("${entry.weightKg.format(1)} kg", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Fat ${entry.bodyFatPercent.format(1)}%") })
            AssistChip(onClick = {}, label = { Text("Muscle ${entry.skeletalMuscleKg.format(1)} kg") })
            AssistChip(onClick = {}, label = { Text("Pulse ${entry.pulse}") })
            AssistChip(onClick = {}, label = { Text("Visceral ${entry.visceralFat}") })
            AssistChip(onClick = {}, label = { Text("Water ${entry.waterPercent.format(1)}%") })
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
    Card(border = BorderStroke(1.dp, color.copy(alpha = 0.5f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(recommendation.priority.name, color = color, fontWeight = FontWeight.Bold)
            }
            Text(recommendation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(recommendation.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyCard(text: String) = InfoCard { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable
private fun InfoCard(content: @Composable Column.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
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

private fun HabitEntry.isDone(kind: HabitKind): Boolean = when (kind) {
    HabitKind.Water -> waterDone
    HabitKind.Steps -> stepsDone
    HabitKind.Protein -> proteinDone
    HabitKind.Workout -> workoutDone
    HabitKind.Posture -> postureDone
    HabitKind.Sleep -> sleepDone
}

private fun TrackerScreen.icon(): ImageVector = when (this) {
    TrackerScreen.Dashboard -> Icons.Default.Insights
    TrackerScreen.TodayPlan -> Icons.Default.FitnessCenter
    TrackerScreen.Measurements -> Icons.Default.MonitorHeart
    TrackerScreen.Recommendations -> Icons.Default.CheckCircle
}

private fun HabitKind.icon(): ImageVector = when (this) {
    HabitKind.Water -> Icons.Default.WaterDrop
    HabitKind.Steps -> Icons.Default.DirectionsWalk
    HabitKind.Protein -> Icons.Default.Restaurant
    HabitKind.Workout -> Icons.Default.FitnessCenter
    HabitKind.Posture -> Icons.Default.SelfImprovement
    HabitKind.Sleep -> Icons.Default.MonitorHeart
}
