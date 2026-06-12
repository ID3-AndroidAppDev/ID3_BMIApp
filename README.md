# Painting App — Compose Canvas UI

An Android drawing app built entirely with **Jetpack Compose**, featuring a neo-brutalist UI and a freehand drawing surface powered by the Compose **Canvas API** — no external graphics libraries used.

---

## Features

### Canvas
- Freehand drawing with smooth drag-gesture strokes
- Adjustable brush width slider
- Erase button to clear the canvas
- Hard-shadow, sticker-style neo-brutalist components

### Ink Lab (colour picker)
- Eight preset pop colours
- Custom colour mixing with R / G / B channel sliders
- Live colour preview swatch
- Back gesture returns to the canvas

---

## Tech stack

| Layer | Detail |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Drawing | Compose `Canvas` — `drawPath`, `drawRoundRect`, drag gestures |
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
├── MainActivity.kt          # Activity entry point
└── PaintingApp.kt           # All screens and composables
app/src/main/java/com/example/id/ui/theme/
├── Color.kt
├── Theme.kt
└── Type.kt
```

---

> Design is not just what it looks like and feels like. Design is how it works.
>
> — Steve Jobs
