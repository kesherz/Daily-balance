# Contributing to Daily Balance

Start from the latest upstream `main` and work on a contribution branch in your fork. Keep pull requests focused on recording and understanding a daily value. The app stays offline, private, and small.

## Local workflow

Use JDK 17, SDK platform 37.0, build tools 36.0.0, and the committed Gradle wrapper. Build and verification commands are in the [README](README.md).

Before submitting:

```sh
./gradlew clean lintDebug testDebugUnitTest assembleDebug
./gradlew connectedDebugAndroidTest  # with an emulator/device
git diff --check
```

Include actual validation results and any unavailable checks in the PR. Do not disable lint or tests to make a change pass. CI checks the tree directly; it must never extract a source ZIP or patch build files during a run.

## Code and data conventions

- Kotlin, four-space indentation, UTF-8, and LF. XML/YAML use two spaces; `.editorconfig` records this.
- Keep domain calculations and parsers independent of Android so boundary cases can be tested locally.
- Use `LocalDate` for calendar dates and `BigDecimal` for values. Convert only normalized chart coordinates to floating point.
- Room's date key enforces one entry per day. Existing-date changes must remain explicit, with stale-write checks where applicable.
- Preserve user data. Database schema changes need a tested Room migration and a committed schema. Never add a destructive migration fallback or remove raw legacy data without a deliberate recovery policy.
- Import changes need malformed-input, duplicate-policy, and transaction tests. Fatal parsing errors must not apply a partial file.
- Keep file I/O on a background dispatcher and use the document picker. Do not add broad storage or network permissions.
- Use Android resources for user-facing messages, including error states, content descriptions, and plurals. Update English and Persian together. Keep canonical file/storage formats independent of the display locale.
- Reuse the theme and spacing tokens in `ui/theme`; prefer native Material controls and accessible 48 dp targets. Chart values must remain available as text, with cues beyond color.

## Manual checks for UI/data changes

Exercise a fresh installation and an upgrade with legacy preferences using the same signing certificate. Check one/two entries, negative/decimal/zero values, repeated dates, history edit/delete/undo, CSV preview/export/import, chart selection/pan/pinch/double-tap reset, and rotation/recreation. Inspect English and Persian RTL, both themes, small/large windows, the keyboard, and increased font size.

Use synthetic values for screenshots and identify them as demonstration data. Commit screenshots only when they come from the running app. Never commit generated build output, APKs, `local.properties`, IDE files, keystores, credentials, or machine paths.

## Compatibility and scope

Retain `com.example.dailycandle` unless the maintainer explicitly plans a breaking installation/data transition. Upgrade compatibility also depends on the APK's signing certificate. Keep Android 6.0 support unless a supported-version decision is made explicitly.

Use stable compatible dependencies. Explain meaningful upgrades and keep versions in the catalog. Avoid speculative layers, cloud accounts, market integrations, predictions, or tracking.

The versioned ZIP archives are historical snapshots only. Edit the tracked tree. The repository currently has no license; choosing one belongs to its maintainer.
