# Daily Balance

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users and purpose

People recording a personal balance or other numeric value once per calendar day.
The primary task is to record today's value quickly, then understand its change.

## Mechanism

Every change candle connects two recorded entries: the previous value is Open,
the current value is Close. No intraday highs, lows, or shadows are available.
Gaps between dates remain gaps in recording; no daily values are interpolated.

## Constraints

Offline, no account, network permission, analytics, ads, or financial advice.
Preserve the existing application ID and legacy saved data. Support English and
Persian, RTL, negative decimals, accessibility, orientation changes, and Android 6+.
Use tracked Kotlin source, Compose Material 3, Room, and the project's wrapper.
CSV import must be reviewed before applying; export uses the system file picker.

## Brand commitments

The visible name is Daily Balance. The requested identity is clean, modern,
focused, and understated, with readable data and a carefully designed dark theme.

## Evidence

The newest source snapshot at the start of this contribution is
DailyCandle_TradingView_v1.5_fixed.zip at upstream commit 9757055.
No release or license was published at audit time; CI distributes debug APKs.
Only real device/emulator captures may be documented as screenshots.

## Principles

- A saved date and decimal value are the source of truth.
- Preserve data before optimizing convenience.
- Keep everyday recording faster than navigating settings.
- Explain change without making predictions.
- Make the data readable in text as well as in the chart.
