# Aesthetic Tracker

Aesthetic Tracker is a Kotlin + Jetpack Compose Android app for a 12-week body recomposition plan. It uses a reducer-based MVI UI layer and Room persistence with a multi-module-friendly package layout (`data`, `domain`, and `ui`).

## Features

- Dashboard with current 12-week plan position and progress from the baseline values.
- Today plan with a generated week/day workout and daily habits.
- Measurements screen for weight, body-fat percentage, skeletal muscle, pulse, visceral fat, and water percentage.
- Recommendations generated from pulse, weekly weight-loss rate, body-fat trend, and skeletal-muscle trend.
- Dark Material 3 Compose UI.

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Coroutines and Flow
- Reducer-based MVI state management

## Build

```bash
gradle :app:assembleDebug --no-daemon
```
