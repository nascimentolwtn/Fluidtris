# Plan: Persist High Score via SharedPreferences

**Backlog item:** [bug] High score resets on app close — `highScore` is in-memory only.

## Problem

`highScore` in `FluidTetrisView.kt:49` is a plain `private var` field. Updated at line 336 when `score > highScore`, never written to disk — resets to `0` every app kill.

## Assumptions

- **Field initializer timing is safe.** Kotlin field initializers run after `super()` completes. `context` in a `View` subclass is `getContext()`, which is set by the `View(context, ...)` superclass constructor. Calling `context.getSharedPreferences()` in a field initializer is therefore safe — it's not accessed before `super()` runs.
- `Context` is already imported (`import android.content.Context` at line 3), so `Context.MODE_PRIVATE` needs no new import.
- `highScore` has exactly one write site (line 335–336). Confirmed by grep — no other assignments.
- **`FluidTetrisView` cannot be instantiated in JVM unit tests** — it extends `View` and requires the Android framework. The project has no Robolectric. The correct test vehicle is **instrumented tests** (`androidTest/`) using `AndroidJUnit4` + `InstrumentationRegistry`, which are already configured in `app/build.gradle.kts`.

## Approach

`SharedPreferences` — correct tool for a single persisted integer. `FluidTetrisView` already has `context` via its `View` base class; no API surface changes needed.

**Prefs file:** `"fluidtris_prefs"` / **key:** `"high_score"`

## Files to change

`FluidTetrisView.kt` only — 2 locations.

## Changes

### 1. Load on init — `FluidTetrisView.kt:49`

```kotlin
// before
private var highScore = 0

// after
private var highScore = context
    .getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
    .getInt("high_score", 0)
```

### 2. Save on new high score — `FluidTetrisView.kt:335–336`

```kotlin
// before
if (score > highScore) {
    highScore = score
}

// after
if (score > highScore) {
    highScore = score
    context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
        .edit()
        .putInt("high_score", highScore)
        .apply()  // async — does not block the game loop
}
```

`score` resets to `0` at line 611 (restart); `highScore` is intentionally untouched there.

## What NOT to change

- Don't move logic to `MainActivity` — View owns all game state.
- Don't use `commit()` — it's synchronous; blocks the game loop.
- Don't add a reset-high-score feature — not requested.

## Tests

**File:** `app/src/androidTest/java/com/libuy/fluidtris/HighScorePersistenceTest.kt`
**Run:** `.\gradlew connectedAndroidTest` (requires device/emulator)
**Framework:** `AndroidJUnit4` + `InstrumentationRegistry` — already in the project via `androidx.test.ext:junit` and `espresso-core` transitive deps.

```kotlin
package com.libuy.fluidtris

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HighScorePersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun defaultsToZero_whenNoPrefExists() {
        val score = context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
            .getInt("high_score", 0)
        assertEquals(0, score)
    }

    @Test
    fun persistsHighScore_acrossPrefsInstances() {
        context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
            .edit().putInt("high_score", 300).commit()

        // New instance simulates a fresh app launch reading the stored value
        val stored = context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
            .getInt("high_score", 0)
        assertEquals(300, stored)
    }

    @Test
    fun updatesHighScore_whenHigherValueWritten() {
        val prefs = context.getSharedPreferences("fluidtris_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("high_score", 100).commit()
        prefs.edit().putInt("high_score", 400).commit()

        assertEquals(400, prefs.getInt("high_score", 0))
    }
}
```

**Coverage note:** The `score > highScore` guard (only write when score beats the current high) is `FluidTetrisView` internal logic — it can only be tested end-to-end via Espresso UI tests or by adding Robolectric. These three tests validate the persistence contract the implementation depends on.

## Success criteria (verifiable)

| Step | Action | Check |
|------|--------|-------|
| 1 | `.\gradlew assembleDebug` | Build exits 0, no compile errors |
| 2 | `.\gradlew connectedAndroidTest` | 3 tests pass: `defaultsToZero`, `persistsHighScore`, `updatesHighScore` |
| 3 | Install and clear one line | UI shows "High Score: 100" |
| 4 | Force-stop the app | — |
| 5 | Relaunch | "High Score: 100" is still displayed (not 0) |
| 6 | Clear another line to exceed 100 | High score updates in UI |
| 7 | Force-stop and relaunch again | New high score persists |
