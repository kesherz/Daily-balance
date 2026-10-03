---
name: Daily Balance
description: A restrained native Android system for recording values and reading real change.
colors:
  light-primary: "#176B59"
  light-on-primary: "#FFFFFF"
  light-primary-container: "#CEEEE0"
  light-on-primary-container: "#073A2C"
  light-secondary: "#496457"
  light-on-secondary: "#FFFFFF"
  light-secondary-container: "#D8E8DF"
  light-on-secondary-container: "#203C2F"
  light-surface: "#F8FAF7"
  light-on-surface: "#18251F"
  light-surface-variant: "#E2E9E2"
  light-on-surface-variant: "#485D52"
  light-surface-container: "#EEF2EC"
  light-surface-container-low: "#F3F6F0"
  light-surface-container-high: "#E6EDE5"
  light-outline: "#6E8074"
  light-outline-variant: "#C2CEC3"
  light-error: "#AC3120"
  light-on-error: "#FFFFFF"
  light-increase: "#12694F"
  light-decrease: "#AC3C29"
  light-unchanged: "#515F58"
  dark-primary: "#91D7C0"
  dark-on-primary: "#063A2B"
  dark-primary-container: "#184F3F"
  dark-on-primary-container: "#C1EEDC"
  dark-secondary: "#B2CCBC"
  dark-on-secondary: "#20392C"
  dark-secondary-container: "#344F40"
  dark-on-secondary-container: "#D0E5D7"
  dark-surface: "#101714"
  dark-on-surface: "#E4EEE6"
  dark-surface-variant: "#34453A"
  dark-on-surface-variant: "#B7C8BB"
  dark-surface-container: "#1B261F"
  dark-surface-container-low: "#17211A"
  dark-surface-container-high: "#26322A"
  dark-outline: "#8A9E8F"
  dark-outline-variant: "#405347"
  dark-error: "#FFB4A5"
  dark-on-error: "#651506"
  dark-increase: "#81D7B5"
  dark-decrease: "#FFB39C"
  dark-unchanged: "#B4C4BB"
typography:
  display-small:
    fontFamily: "sans-serif"
    fontSize: "34sp"
    fontWeight: 500
    fontFeature: "tnum"
  headline-small:
    fontFamily: "sans-serif"
    fontWeight: 600
  title-large:
    fontFamily: "sans-serif"
    fontWeight: 600
  title-medium:
    fontFamily: "sans-serif"
    fontWeight: 500
  body-large:
    fontFamily: "sans-serif"
    fontFeature: "tnum"
  body-medium:
    fontFamily: "sans-serif"
    fontFeature: "tnum"
  body-small:
    fontFamily: "sans-serif"
  label-large:
    fontFamily: "sans-serif"
  label-medium:
    fontFamily: "sans-serif"
  label-small:
    fontFamily: "sans-serif"
rounded:
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  extra-large: "24dp"
spacing:
  tiny: "4dp"
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  section: "24dp"
components:
  entry-dock-light:
    backgroundColor: "{colors.light-surface-container-low}"
    padding: "8dp 16dp"
  entry-dock-dark:
    backgroundColor: "{colors.dark-surface-container-low}"
    padding: "8dp 16dp"
  save-button:
    height: "48dp"
  date-button:
    height: "48dp"
  settings-choice:
    height: "56dp"
  chart:
    height: "260dp"
  app-icon:
    size: "22dp"
---

# Design System: Daily Balance

## Overview

Daily Balance is clean, modern, focused, and understated. The visual system puts saved values, dates, and changes ahead of decoration. Open sections and restrained dividers keep dense information readable without a stack of oversized cards.

This is a native Android system built with Jetpack Compose Material 3. Brand expression comes from its paired color schemes, a small set of typography overrides, and shared spacing and shapes. Geometry uses Android dp; text uses sp and follows system font scale. No custom font or separate visual metaphor is established.

**Key Characteristics:**

- Quiet neutral light surfaces and deep green dark surfaces.
- Teal primary actions with native Material controls.
- Full numeric values and textual evidence beside the chart.
- English and Persian content with Android RTL behavior.

The source of truth is `app/src/main/kotlin/com/example/dailycandle/ui/theme/BalanceTheme.kt`, together with the reused UI components. The frontmatter records that source. Unlisted Material typography, component shapes, states, and elevation inherit the project's Material 3 dependency.

## Colors

The palette combines green-tinted neutrals with a teal accent and separate readable change colors. Frontmatter keys prefixed `light-` and `dark-` describe the paired schemes; production UI resolves roles through `MaterialTheme.colorScheme` and `LocalChangeColors`.

### Primary

Deep teal in light mode and pale teal in dark mode identify primary actions and chart selection. Primary-container pairs support native elevated actions. Use each role's matching on-color for its content.

### Secondary

Muted green secondary and secondary-container pairs support Material secondary controls and selection indicators. They do not add a competing decorative accent.

### Neutral

Surface and on-surface also serve as background and on-background in each scheme. Surface-container levels distinguish native containers; the entry dock uses surface-container-low. On-surface-variant carries supporting dates, captions, and explanations. Outline and outline-variant supply control boundaries, dividers, and chart grid lines.

### Semantic change and error

Increase, decrease, and unchanged have separate light and dark colors in `LocalChangeColors`. Error roles communicate input and operation failures; decrease is a data direction, not an input error.

**The Redundant Change Rule.** Always present change with its direction label, icon, and signed value as well as its semantic color.

## Typography

**Display, body, and label font:** Android platform sans-serif through Material 3. Platform fallback handles English and Persian. There is no downloaded display face or separate monospace font.

### Hierarchy

| Material role | App treatment and use |
| --- | --- |
| displaySmall | The custom display token presents the latest full balance with medium weight and tabular numerals. |
| headlineSmall | Semibold emphasis for the empty-state headline. |
| titleLarge | Semibold values in `AmountText`; also the inherited Material screen-title role. |
| titleMedium | Medium section headings, selected dates, and history values. |
| bodyLarge / bodyMedium | Native reading text; these two roles request tabular numerals. |
| bodySmall | Supporting dates, hints, state explanations, and captions. |
| labelLarge / labelMedium / labelSmall | Direction and field labels, small contextual labels, and chart axes. |

Only the source overrides appear as numeric typography tokens. Other font sizes, line heights, and letter spacing remain the defaults of `Typography()`; do not create a second hand-picked type ramp in screens.

**The Full Value Rule.** Keep stored amounts selectable, unwrapped, and horizontally scrollable when needed; do not shorten or truncate the value to fit a layout.

Decimal input explicitly uses left-to-right text direction. Surrounding translated content follows Android layout direction. Display formatting is locale-aware, and all user-facing copy belongs in Android string resources.

## Layout

Use the shared `Space` scale. Home and Settings use the large content inset and section gaps; related details use tiny, small, or medium gaps. History uses open rows and dividers rather than separate cards. Keep content scrollable when orientation, the keyboard, or larger text reduces the available space.

Window width determines navigation: below (600 dp), use the bottom navigation bar; at or above it, use a navigation rail. The root applies IME padding and the scaffold uses safe-drawing insets. Preserve the native top app bar, system Back behavior, and saveable screen state.

The Balance entry dock remains in the scaffold's bottom area, above compact navigation. Its scrollable form is limited to half of the available root height. The inline field/save arrangement requires compact mode, at least (340 dp) of form width, and font scale at most (1.3); otherwise the controls stack vertically.

Selected Open and Close details share a row only at widths of at least (320 dp) and font scale at most (1.4). Otherwise they stack. These are native dp and font-scale conditions, not CSS breakpoints.

## Elevation & Depth

Depth comes from Material surface tones, standard component elevation, and restrained dividers. There is no custom shadow vocabulary. Keep the entry dock on the low container tone; let native dialogs, the History FAB, and navigation retain their Material behavior.

**The Native Depth Rule.** Use Material surface roles and component elevation instead of introducing arbitrary drop shadows or ornamental layers.

Navigation crossfades with the source's (180 ms) tween. Summary disclosure and history item changes use Compose's built-in animation APIs; no separate custom easing or timing scale is defined.

## Shapes

The theme supplies small, medium, large, and extra-large rounded steps from the frontmatter. Material components resolve their appropriate shapes from the theme and component defaults. Preserve their native silhouettes rather than applying a single radius to every control.

Chart bodies retain simple rectangular geometry. Selection adds a fine outline, dashed guide, and quiet tonal column. No decorative frame is required around the chart.

## Components

### Buttons and actions

Filled Material buttons handle Save and retry; tonal buttons handle export; outlined buttons handle the entry date and import; text buttons handle secondary disclosure and dialog actions. Date and Save height tokens are minimum heights, not fixed heights. Inline Save raises its minimum to (56 dp). Keep Material focus, ripple, disabled, and error treatment.

The History FAB has the single Add entry action. Standard app glyphs use the app-icon size token; their touch targets are provided by native buttons, not by the glyph bounds. Supply content descriptions for icon-only actions.

### Entry field and date control

Use `OutlinedTextField` with a persistent value label, decimal keyboard, Done action, and sign toggle. Show a supporting hint or error in the full form; the compact dock presents errors below the row. Busy state disables edits and saves. The date control opens the native Material DatePicker.

Use “Entry date” for the editable draft and “Recorded on” for a saved value. Keep this distinction in both locales.

### Navigation and settings

Balance, History, and Settings use labeled Material navigation items. Active and inactive treatment stays with Material; size selects bar or rail as described in Layout. Settings choices are full-width selectable radio rows, with the settings-choice token defining their minimum height.

### Open sections and dialogs

Section headings use `SectionTitle` with heading semantics. Amount/date groups and History rows rely on spacing and restrained dividers. Use native AlertDialog for editing, deletion confirmation, and import review; use Snackbar for transient feedback and delete Undo.

### Change details

`ChangeDetails` groups a direction icon and label, full signed absolute change, and the available percentage. If the percentage cannot be calculated, show the textual explanation. `ValueDetail` pairs a small contextual label with the full amount and optional date caption.

### Body-only chart

The chart-height token defines the current canvas height. Each candle connects the previous recorded value (Open) to the current recorded value (Close). Unchanged values use a horizontal mark. No highs, lows, wicks, or interpolated daily entries exist.

Keep selected dates, Open, Close, and change available as text below the canvas. Tap selects, pinch/pan changes the viewport, and Reset returns to the latest data. Previous/next native buttons and the chart accessibility reset action provide alternatives to gestures. Axis labels use measured text; an explicit relative-value caption explains any axis offset.

## Do's and Don'ts

### Do

- **Do** resolve color through Material theme roles and the current change-color provider.
- **Do** keep full values, dates, Open, Close, and change readable in text.
- **Do** preserve native dp/sp sizing, safe-drawing and IME insets, font-scale adaptation, and RTL resources.
- **Do** use the shared type roles, spacing, shapes, and native Material interaction states.

### Don't

- **Don't** add gradients, flashy effects, decorative imagery, or oversized card stacks.
- **Don't** rely on color alone to explain direction or selection.
- **Don't** invent high/low values, wicks, missing daily values, or market predictions.
- **Don't** label an unsaved draft date as a recorded value.
