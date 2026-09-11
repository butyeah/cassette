# Cover

Cover is Cassette's design-system module: reusable style tokens and Compose components, kept
independent of the rest of the app. It has no dependency on `:app`, `:data`, or `:domain` — only
`:app` depends on it, never the other way around. That's the whole point: nothing in here should
ever need to know what a "day," an "album," or a "playlist" is.

## Status

This is the initial extraction — the style tokens and the two components that existed when this
module was created (the animated background and the frosted-glass card, both first built for the
Daily screen). More of the app's UI can move in here over time; this isn't a full migration.

## Getting started

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":cover"))
}
```

Wrap your screen's content in `CoverTheme` (Cassette's own `CassetteTheme`, in `:app`, is just a
thin wrapper around this):

```kotlin
CoverTheme {
    // your content
}
```

## Style tokens

All under `com.ruidoespontaneo.cassette.cover.theme`.

| Token | What it's for |
|---|---|
| `Spacing` | Padding and gaps between elements — `Spacing.small`, `.medium`, `.large`, etc. Use instead of hardcoding `.dp` literals, so spacing stays consistent across screens. |
| `TextSize` | Raw font sizes for one-off `Text` composables that don't go through a `MaterialTheme.typography` slot. Prefer `MaterialTheme.typography` for anything styled as a proper heading/body/label — `TextSize` is the escape hatch, not the default. |
| `Typography` | The `androidx.compose.material3.Typography` Cover's `MaterialTheme` is built with. |
| `CoverTheme`'s color scheme | Light/dark Material3 `ColorScheme`, with dynamic color on Android 12+. Pull colors from `MaterialTheme.colorScheme` (e.g. `.primary`, `.surface`) rather than reaching for a token here directly — that's how both components below get their colors, and it's what keeps them dark-mode-correct for free. |

`IconSize` (album-art sizing) intentionally stays in `:app` — it's a domain-specific token
("albumArt", "albumArtLarge"), not a design-system one. Cover can grow generic icon-size tokens
later if more components need them.

## Components

All under `com.ruidoespontaneo.cassette.cover.components`.

### `AnimatedGradientBackground`

Three soft, blurred color blobs drifting in slow, independent orbits — a Gemini-style "living"
gradient background, meant to sit full-screen behind other content.

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    AnimatedGradientBackground(Modifier.matchParentSize())
    // your content, on top
}
```

Colors come from `MaterialTheme.colorScheme` (`primary`/`secondary`/`tertiary`), so it adapts to
light/dark and dynamic color automatically. Each blob's animated angle is read inside its `Canvas`
draw lambda rather than in the composable body, so every animation frame only triggers a redraw —
not a recomposition of whatever it sits behind.

### `CoverCard`

A frosted-glass "crystal" card — real-time backdrop blur of whatever's marked as its source,
typically `AnimatedGradientBackground`. Built on [Haze](https://github.com/chrisbanes/haze), since
Compose has no backdrop-filter of its own.

```kotlin
val hazeState = rememberHazeState()

Box(modifier = Modifier.fillMaxSize()) {
    AnimatedGradientBackground(Modifier.matchParentSize().hazeSource(hazeState))
    CoverCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Text("Frosted glass content")
    }
}
```

`hazeState` must be the **same instance** passed to both `Modifier.hazeSource(...)` on the
background and `CoverCard`'s `hazeState` param — that's how Haze knows what to blur.

`tint`, `tintAlpha`, and `blurRadius` have sane defaults (`MaterialTheme.colorScheme.surface` at
50% alpha, 20.dp blur) but are all overridable.

**Gotcha:** `CoverCard` only clears its own background. Any Material component placed inside it
that paints its own opaque background — `ListItem` is the one that's bitten us — needs an explicit
transparent `containerColor` too, or it'll hide the blur wherever it sits:

```kotlin
ListItem(
    // ...
    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
)
```
