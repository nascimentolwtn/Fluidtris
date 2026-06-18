# Plan: Background Music + Toggle Button

## Context
Fluidtris has three SFX (line clear, piece lock, level-up) but no ambient background music. The request is to add a looping background music track and a dedicated toggle button so the player can silence it independently from the SFX mute.

## Prerequisite: Audio File
An audio file must be placed at:
```
app/src/main/res/raw/bg_music.mp3
```
The code references `R.raw.bg_music`. MediaPlayer supports both OGG and MP3 formats; the resource ID resolves based on filename in `res/raw/`. Place your audio file in the directory above.

---

## Step 1 — SoundManager.kt

Add background-music state and four methods. No changes to existing SFX paths.

```kotlin
// New state (top of class, alongside existing `var enabled = true`)
var bgMusicEnabled = true
private var bgMusicPlayer: MediaPlayer? = null
private var bgMusicPaused = false

fun startBgMusic(context: Context) {
    stopBgMusic()
    if (!bgMusicEnabled) return
    bgMusicPlayer = MediaPlayer.create(context, R.raw.bg_music)?.apply {
        isLooping = true
        start()
    }
    bgMusicPaused = false
}

fun pauseBgMusic() {
    bgMusicPlayer?.takeIf { it.isPlaying }?.pause()
    bgMusicPaused = true
}

fun resumeBgMusic() {
    if (!bgMusicEnabled || !bgMusicPaused) return
    bgMusicPlayer?.start()
    bgMusicPaused = false
}

fun stopBgMusic() {
    bgMusicPlayer?.apply { stop(); release() }
    bgMusicPlayer = null
    bgMusicPaused = false
}

fun toggleBgMusic(context: Context) {
    bgMusicEnabled = !bgMusicEnabled
    if (bgMusicEnabled) startBgMusic(context) else pauseBgMusic()
}
```

`startBgMusic` always calls `stopBgMusic` first so calling it on "New Game" never leaks a player.

---

## Step 2 — FluidTetrisView.kt

### 2a. Button rendering
The existing sound toggle occupies `y: 170–270`. Place the BG music toggle directly below it at `y: 280–380`, same x bounds (`10–280`).

Add to `onDraw()` (after the existing sound toggle block):
```kotlin
// BG music toggle button
paint.color = Color.argb(200, 80, 120, 150)
canvas.drawRect(10f, 280f, 280f, 380f, paint)
paint.color = Color.argb(255, 200, 240, 230)
paint.textSize = 48f
val bgText = if (soundManager.bgMusicEnabled) "🎵" else "🎵🔇"
canvas.drawText(bgText, 60f, 345f, paint)
```

Do the same inside the `if (engine.isPaused)` redraw block so the button stays visible over the pause overlay.

### 2b. Touch handling
In `onTouchEvent ACTION_DOWN`, immediately after the existing sound-toggle check (`event.y in 170f..270f`), add:
```kotlin
if (event.x in 10f..280f && event.y in 280f..380f) {
    soundManager.toggleBgMusic(context)
    invalidate()
    return true
}
```

### 2c. Lifecycle wiring
| Where in FluidTetrisView | Call |
|---|---|
| `init` block, after `handler.post(updateRunnable)` | `soundManager.startBgMusic(context)` |
| Touch handler — pause button tap | `soundManager.pauseBgMusic()` |
| Touch handler — Resume button tap | `soundManager.resumeBgMusic()` |
| Touch handler — New Game button tap | `soundManager.startBgMusic(context)` |
| Touch handler — Exit button tap | `soundManager.stopBgMusic()` |
| `onDetachedFromWindow()` (add override if missing) | `soundManager.stopBgMusic()` |

`startBgMusic` safely restarts on New Game because it calls `stopBgMusic` internally first.

---

## Step 3 — Napkin Update
Add to Backlog (then mark Done once shipped):
```
[2026-06-17] Feature: background music + toggle button
```

---

## Files Changed
- `app/src/main/java/com/libuy/fluidtris/SoundManager.kt` — new bg music methods
- `app/src/main/java/com/libuy/fluidtris/FluidTetrisView.kt` — button render, touch, lifecycle
- `app/src/main/res/raw/bg_music.ogg` — user-supplied audio file
- `.claude/napkin.md` — backlog item

**GameConstants.kt and GameEngine.kt are not touched.**

---

## Verification
1. `./gradlew assembleDebug` — clean build
2. `./gradlew installDebug` — install on all devices
3. Manual checks (user verifies visually):
   - Music starts when app launches
   - BG toggle (🎵 / 🎵🔇) silences/restores music independently from SFX toggle
   - Music pauses when pause overlay opens; resumes when Resume tapped
   - Music restarts (from beginning) when New Game tapped
   - Music stops when Exit tapped
   - Toggling off then New Game → music stays off (respects `bgMusicEnabled = false`)