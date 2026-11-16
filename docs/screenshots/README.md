# Screenshot provenance

These are unedited Minecraft captures of UltraStyle Beta 0.1, taken with Fabric's client game-test screenshot API. The arena and combat sequence are staged in a disposable single-player world; the mod renders every overlay and records real player-attributed damage and entity deaths. No stats or UI were composited into the images.

Regenerate with Java 25:

```sh
./gradlew runClientGameTest
```

`ShowcaseGameTest` builds the arena, fights a Wither and six husks, then captures the boss results and player death screen. It clears chat and advancement toasts for visibility. The generated PNGs are in `build/run/clientGameTest/screenshots/`:

- `showcase-style-boss.png` → `style-and-boss-hud.png`
- `showcase-boss-results.png` → `boss-results.png`
- `showcase-life-results.png` → `life-summary.png`

Copy the three images here after visually checking them. This test code is excluded from the distributable mod JAR.
