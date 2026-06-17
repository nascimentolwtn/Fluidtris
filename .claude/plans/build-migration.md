# Build Migration Plan: AGP 10.0 Compatibility

## Current State
- **AGP Version**: 9.2.1
- **Gradle Wrapper**: 9.4.1
- **Kotlin Version**: 2.2.10
- **Current Flags**: `android.builtInKotlin=false` and `android.newDsl=false`
- **Issue**: Both flags trigger deprecation warnings about APIs being removed in AGP 10.0
- **Test Status**: 170+ unit tests passing; build succeeds with warnings

## Deprecation Warnings Being Generated
When `android.builtInKotlin=false`, AGP uses the legacy variant API interfaces:
- `applicationVariants` (deprecated)
- `testVariants` (deprecated)
- `unitTestVariants` (deprecated)

These APIs are accessed internally by AGP's build infrastructure when the built-in Kotlin plugin support is disabled.

## Root Cause of Previous Failure
When attempting to set both flags to `true` simultaneously, there's a conflict in plugin registration:
- `android.builtInKotlin=true` attempts to automatically apply the Kotlin plugin via AGP's internal mechanism
- The explicit `alias(libs.plugins.kotlin.android)` in `build.gradle.kts` creates a duplicate plugin application
- This causes: "Cannot add extension with name 'kotlin'" error

---

## Step-by-Step Migration Plan

### Phase 1: Preparation & Safety (No Changes)
**Objective**: Establish baseline and safety measures

1. **Establish baseline metrics**
   - Confirm all 170+ unit tests pass with current configuration
   - Document current build output capturing deprecation warnings
   - Verify `./gradlew clean assembleDebug` succeeds

2. **Create git branch for this work**
   - Branch name: `build/agp-10-compat`
   - Ensures easy rollback if needed

3. **Document expected changes**
   - No source code changes needed
   - Only `gradle.properties` and `app/build.gradle.kts` modifications
   - All changes are additive (plugin registration, not removal)

**Risk Level**: ZERO — no code changes, read-only exploration

---

### Phase 2: Step 1 — Enable Built-In Kotlin Plugin Support
**Objective**: Migrate from external Kotlin plugin to AGP's built-in support

**Changes Required**:
1. **gradle.properties**: Change `android.builtInKotlin=false` → `android.builtInKotlin=true`
2. **app/build.gradle.kts**: Remove the explicit `alias(libs.plugins.kotlin.android)` plugin application
3. **Keep** the Kotlin plugin defined in `libs.versions.toml` (for AGP to reference)

**Files to Modify**:
- `gradle.properties` (line 28)
- `app/build.gradle.kts` (line 3: remove `alias(libs.plugins.kotlin.android)`)

**Verification Steps**:
- Run: `./gradlew clean assembleDebug`
- Run: `./gradlew testDebugUnitTest` (all 170+ tests must pass)
- Verify: No "Cannot add extension with name 'kotlin'" error
- Check: Kotlin compilation still works

**Rollback Point**: Revert the two file changes

**Risk Level**: MEDIUM — Plugin registration change; well-documented by Google

---

### Phase 3: Step 2 — Enable New DSL (AndroidComponentsExtension)
**Objective**: Migrate to new variant API while built-in Kotlin is active

**Changes Required**:
1. **gradle.properties**: Change `android.newDsl=false` → `android.newDsl=true`
2. **No source code changes needed** — AGP handles variant infrastructure internally

**Files to Modify**:
- `gradle.properties` (line 29)

**Verification Steps**:
- Run: `./gradlew clean assembleDebug`
- Run: `./gradlew testDebugUnitTest` (all 170+ tests must pass)
- Verify: No deprecation warnings in build output
- Inspect: `gradle build --info` should show 0 deprecation warnings

**Rollback Point**: Revert the single flag change

**Risk Level**: LOW — Flag-only change; feature is stable in AGP 9.2

---

### Phase 4: Verification & Cleanup
**Objective**: Confirm migration success and update documentation

1. **Full build verification**
   - `./gradlew clean` (wipe caches)
   - `./gradlew assembleDebug` (build APK)
   - `./gradlew testDebugUnitTest` (170+ tests pass)

2. **Deprecation warning audit**
   - Run: `./gradlew assembleDebug --info` and verify 0 deprecation warnings

3. **Documentation updates**
   - Update `.claude/napkin.md`: Move build migration from Backlog to Done
   - Update commit message with rationale

**Risk Level**: ZERO — Verification only

---

## Detailed File-by-File Changes

### 1. `gradle.properties`

**Line 28**: Change from:
```properties
android.builtInKotlin=false
```
To:
```properties
android.builtInKotlin=true
```

**Line 29**: Change from:
```properties
android.newDsl=false
```
To:
```properties
android.newDsl=true
```

---

### 2. `app/build.gradle.kts`

**Line 3**: Remove:
```kotlin
alias(libs.plugins.kotlin.android)
```

**Before**:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
```

**After**:
```kotlin
plugins {
    alias(libs.plugins.android.application)
}
```

**Rationale**:
- When `android.builtInKotlin=true`, AGP automatically applies the Kotlin plugin
- Explicit `alias(libs.plugins.kotlin.android)` creates duplicate registration → conflict
- Removing the explicit line avoids the "Cannot add extension with name 'kotlin'" error

---

## Critical Sequencing

```
Step 1: Enable builtInKotlin (gradle.properties)
  ↓
Step 2: Remove explicit kotlin plugin (app/build.gradle.kts)
  ↓
Step 3: Enable newDsl (gradle.properties)
  ↓
Step 4: Verify all tests pass + deprecation warnings gone
```

---

## Risk Assessment

| Step | Risk | Mitigation | Rollback |
|------|------|-----------|----------|
| Step 1 | Medium | Make one change at a time; test immediately | Revert both files |
| Step 2 | Low | AGP 9.2 has stable new DSL | Revert `gradle.properties` line 29 |
| Tests | Low | Already passing | Rollback changes, rebuild |

---

## Success Criteria

1. ✓ All 170+ unit tests pass after migration
2. ✓ Zero deprecation warnings about variant APIs in build output
3. ✓ APK builds successfully: `./gradlew assembleDebug`
4. ✓ Only `gradle.properties` and `app/build.gradle.kts` changed
5. ✓ AGP 10.0 compatible (no deprecated APIs in use)

---

## Expected Outcomes Post-Migration

- **Gradle Deprecation Warnings**: Eliminated
- **AGP 10.0 Ready**: No blockers for future upgrade
- **Build Time**: Likely unchanged or slightly faster
- **Functionality**: Identical; no game logic changes
- **Tests**: All 170+ pass; no regressions