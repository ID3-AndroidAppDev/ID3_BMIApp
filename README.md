# BMI App — Compose Canvas UI

An Android BMI calculator built entirely with **Jetpack Compose**, featuring a live dashboard drawn with the Compose **Canvas API** — no charting libraries used.

---

## Features

### Calculator
- Metric and Imperial unit toggle
- Arc gauge that animates to the current BMI value
- Category label and description update in real time
- Save result with one tap — stores BMI + timestamp to SharedPreferences

### Dashboard (shown once at least one record is saved)
- **Stats row** — MIN / AVG / MAX chips, each coloured by their BMI category
- **BMI Trend** — line chart with time-based x-axis; points are spaced by actual elapsed time, x-axis labels use clean round intervals (1 min / 5 min / 30 min / 1 h…)
- **Distribution** — donut chart showing the share of each BMI category, with record count in the centre hole and a colour-coded legend

### History
- Records grouped by BMI category
- Within-category trend arrows (▼ improving, ▲ worsening)
- Full timestamp on every entry
- Clear All button

---

## Tech stack

| Layer | Detail |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Charts | Compose `Canvas` — `drawArc`, `drawLine`, `drawPath`, `drawText` |
| Persistence | `SharedPreferences` + `JSONArray` |
| Language | Kotlin |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## Getting started

1. Clone the repo and open the project root in **Android Studio Ladybug** or later.
2. Let Gradle sync finish.
3. Run on a device or emulator (API 24+).

No API keys or external services required.

---

## Project structure

```
app/src/main/java/com/example/id/
└── MainActivity.kt          # All screens and composables
app/src/main/java/com/example/id/ui/theme/
├── Color.kt
├── Theme.kt
└── Type.kt
```

---

> Design is not just what it looks like and feels like. Design is how it works.
>
> — Steve Jobs
