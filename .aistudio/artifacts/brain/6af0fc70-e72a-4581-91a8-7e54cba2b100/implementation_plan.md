# Navigation Polish, Smooth Scrolling & Subtitle / Player Sheets Overhaul

An implementation plan to resolve navigation glitching, eliminate folder scroll stutter, enhance Hindi/Devanagari subtitle typography with Poppins, and redesign player side panels with clean auto-dismiss interactions.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The black-screen flash when switching bottom navigation tabs was caused by pure `fadeIn + fadeOut` crossfades exposing the Scaffold root layer. Switching to directional horizontal slide animations (`slideInHorizontally` + `slideOutHorizontally`) and matching background surfaces will provide the requested smooth swipe transition. Folder stutter was traced to `IntrinsicSize.Min`, `LocalConfiguration` lookups, and un-memoized vector decoding in `FolderItem`. Player side sheets currently have awkward external pill tabs and missing auto-dismiss handlers on selection and chapters.

- **Confirmed Decision 1**: Replace bottom navigation tab crossfade with directional slide transitions (`slideInHorizontally` / `slideOutHorizontally` with easing) and animated pill indicator to eliminate the black flash.
- **Confirmed Decision 2**: Remove `IntrinsicSize.Min`, cache vector drawables, and remove runtime `LocalConfiguration` lookups in `FolderItem` to make folder scrolling 60/120fps fluid.
- **Confirmed Decision 3**: Ensure Poppins font is loaded with proper readable weights (400 regular, 600 bold) for Devanagari numerals (`०-९`) and Hindi text, and verify full real-time binding with player subtitle preferences.
- **Confirmed Decision 4**: Redesign `OverlayView` side sheets across Audio, Subtitles, and Chapters: remove the clunky protruding tabs and header lines; dismiss automatically on item selection or when tapping outside.

---

## 1. Overview & Core Concept

- **Bottom Navigation**: Tab switching slides smoothly in the direction of the selected tab without visual flicker or dark flashes.
- **Folder Grid & List**: Fast, frictionless scrolling through folders with zero dropped frames.
- **Movie Subtitles**: Crisp, authentic Devanagari/Hindi rendering using Poppins font, instantly responsive to size, bold, background, and font changes from settings.
- **Player Panels**: Minimalist, floating side panels for Audio, Subtitle, and Chapter selection that close seamlessly upon choosing an option or tapping outside.

---

## 2. User Experience & Visual Design

- **Tab Navigation**: Selecting a tab to the right slides content in from the right; selecting a tab to the left slides content in from the left. The rounded bottom bar indicator moves fluidly.
- **Home Folders**: Smooth physics-based fling and scrolling with zero stuttering or layout re-measure spikes.
- **Subtitles**: Hindi and English text in movie subtitles render clearly with Poppins typography; Devanagari numbers are properly shaped and legible.
- **Player Controls & Drawers**: When tapping Audio, Subtitle, or Chapter icons in the video player, a modern sheet slides in cleanly without awkward border tabs. Tapping a track or chapter activates it and immediately dismisses the sheet. Tapping anywhere outside on the dimmed video dismisses the sheet.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Directional Slide for Top-Level Navigation**
  - *Chosen Approach*: Track initial and target destination indices; slide in from right (+X) when moving forward, and from left (-X) when moving backward.
  - *Why*: Gives a physical swipe feel and eliminates the transparent gap where black background flashed through.
- **Decision 2: Remove Intrinsic Measurements in Folder Items**
  - *Chosen Approach*: Replace `IntrinsicSize.Min` with fixed aspect ratios and static widths, and pre-cache folder vector thumbnails.
  - *Why*: Intrinsic measurements in Lazy lists force two full measurement passes for every item on every scroll frame, causing heavy frame drops.
- **Decision 3: Devanagari Range & Poppins Weight in SubtitleView**
  - *Chosen Approach*: Check standard Devanagari (`0x0900..0x097F`), Devanagari Extended (`0xA8E0..0xA8FF`), and Vedic extensions (`0x1CD0..0x1CFF`). Use weight 400 (normal) / 600 (bold) with Poppins font.
  - *Why*: Weight 300 was overly thin and rendered Hindi vowels and digits inconsistently.
- **Decision 4: Auto-Dismiss on Track & Chapter Selection**
  - *Chosen Approach*: Wire `onDismiss()` calls into `ChaptersView`, `SubtitleSelectorView`, and `AudioTrackSelectorView` upon item click, and attach `noRippleClickable(onClick = onDismiss)` to the scrim.
  - *Why*: Removes unnecessary extra taps to close menus and eliminates the awkward closing handle tabs.

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                    Top-Level Navigation                      │
├──────────────────────────────────────────────────────────────┤
│  TopLevelNavState.navigationTransition:                      │
│  Target Index > Current Index ──► Slide Left (from Right)    │
│  Target Index < Current Index ──► Slide Right (from Left)    │
│  No transparent black fadeout gap                            │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                   Home Folder Optimization                   │
├──────────────────────────────────────────────────────────────┤
│  - Remove IntrinsicSize.Min                                  │
│  - Cache folder_thumb vector                                 │
│  - Replace screenWidthDp read with fixed 88.dp width         │
│  - Memoize formatDuration and formatFileSize                 │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│               Subtitle Engine & Poppins Font                 │
├──────────────────────────────────────────────────────────────┤
│  - Poppins for Hindi, Devanagari numerals (०-९), and English │
│  - Correct font weight (400 regular / 600 bold)              │
│  - Reactive SubtitleConfiguration listener from Preferences  │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                  Player Drawer & Sheets                      │
├──────────────────────────────────────────────────────────────┤
│  - OverlayView: Remove protruding pill tabs & header lines   │
│  - Scrim tap dismiss + Selection auto-dismiss                │
│  - ChaptersView: Add onDismiss and auto-close on item tap    │
└──────────────────────────────────────────────────────────────┘
```

### Proposed Changes Across Modules

1. **`app/src/main/java/dev/anilbeesetti/nextplayer/navigation/TopLevelNavigation.kt`**:
   - Update `navigationTransition` to use directional `slideInHorizontally` + `slideOutHorizontally` based on tab indices.
   - Smooth out the bottom bar selection pill animation in `NavigationView`.

2. **`feature/videopicker/src/main/java/dev/anilbeesetti/nextplayer/feature/videopicker/composables/FolderItem.kt`**:
   - Remove `LocalConfiguration.current` dependency from item body.
   - Remove `IntrinsicSize.Min` from `FolderGridItem`.
   - Remember thumbnail icon and string formatters.

3. **`feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/SubtitleView.kt`**:
   - Expand Devanagari detection for Hindi characters and Devanagari digits (`0x0966..0x096F`).
   - Use Poppins font with 400 / 600 weight for Hindi/Devanagari text and numbers.
   - Ensure dynamic updates when settings are altered.

4. **`feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/ui/OverlayView.kt` & Selectors**:
   - Strip the protruding side handle button and header pill buttons from `OverlayView`.
   - Add clean dismiss handling.
   - Update `ChaptersView.kt` to auto-dismiss on chapter selection.
   - Ensure outside clicks on the overlay scrim immediately close the sheet.
