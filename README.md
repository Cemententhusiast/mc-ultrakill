# UltraStyle

A client-first Minecraft Java **26.3** mod built with **Fabric** and **Java 25**.
It adds an ULTRAKILL-inspired style display and boss bars, with optional real XP rewards in single-player. It uses original, code-drawn UI and Minecraft's font; no ULTRAKILL assets are bundled.

## Install and play

Install Fabric Loader 0.20.1 or newer for Minecraft 26.3, and Fabric API for that version.
Put `ultrastyle-0.1.0.jar` from `build/libs/` into the client's `mods/` directory alongside Fabric API. Do not install the `-sources.jar`.

The mod does not need to be installed on multiplayer servers. It is explicitly client-only and should not be installed on a dedicated server.

## Features

- Right-side rank, progress meter, session score, multikill count, and fading action feed.
- The entire style panel tilts upward toward the right by 4 degrees by default. Its rotated bounds stay inside the screen.
- Ranks D, C, B, A, S, SS, SSS, and ULTRAKILL.
- Hostile mob kills award 100 points; other mobs award 25.
- Consecutive kills within four seconds add 50 bonus points per combo step, capped at 200 bonus points per kill.
- Ordinary blocks award 1 point; blocks whose registry names end in `_ore`, plus ancient debris, award 20.
- Mining contributes at most 40 points per ten seconds. Creative/spectator mining and kills do not award points.
- Damage removes up to 100 style and ends the combo. Death clears style and combo, preserving the session total.
- Style decays by 10 per second after three seconds without a scoring action. Session totals never decay.
- Boss bars have thick outlines, server-provided colors and segments, a percentage, and a delayed damage trail. Multiple bars stack with an overflow count.
- F1 hides both overlays. Pausing single-player pauses score decay.

## Commands and configuration

These commands run locally and do not require operator permissions:

| Command | Effect |
| --- | --- |
| `/ultrastyle` | Show score, XP setting, and command hints |
| `/ultrastyle hud` | Toggle the style panel |
| `/ultrastyle tilt -4` | Set the score panel angle immediately (−15 to 15 degrees); use 0 for a level panel |
| `/ultrastyle bosses` | Toggle replacement boss bars; disabling restores vanilla bars |
| `/ultrastyle xp 5` | Enable **single-player only** rewards at 5 XP per awarded point |
| `/ultrastyle xp 0` | Disable XP rewards |
| `/ultrastyle reset` | Clear the local score session; already earned XP remains |
| `/ultrastyle reload` | Reload the configuration file |

XP rewards are **disabled by default**. They award actual XP points, not levels, on the integrated server thread. A 100-point kill with `/ultrastyle xp 5` grants 500 extra XP. Fractional multipliers work, and the accepted range is 0–100. Only accepted positive score awards convert to XP, so the mining cap also limits mining XP. Damage never removes XP. Rewards are additive to vanilla XP drops and cannot change XP on remote servers.

`config/ultrastyle.json` is created on first launch:

```json
{
  "hudEnabled": true,
  "bossBarsEnabled": true,
  "singleplayerXp": false,
  "xpPerPoint": 1.0,
  "hudScale": 1.0,
  "hudTiltDegrees": -4.0,
  "rightMargin": 12,
  "verticalOffset": 0
}
```

Use `hudScale` (0.5–2), `rightMargin`, and `verticalOffset` to position the panel around other HUD mods or the server scoreboard. Scores are session-only: reconnecting or returning to the title screen resets them. Changing dimension preserves score but clears pending observations.

## Accuracy and current limitations

Single-player uses integrated-server block, damage, and death events. Kill credit includes vanilla's recent-player credit for indirect kills. Only the local player earns UltraStyle rewards, including in a world opened to LAN.

On remote servers, scoring is cosmetic and best effort. The client tracks damage packets attributed to you and requires an observed death notification within five seconds. It drops credit when a different known attacker damages the target. Disappearing entities do not count as kills. Some indirect kills, damage during invulnerability cooldowns, and server-specific combat mechanics may be missed or attributed differently from server statistics.

Remote block breaks wait half a second and require the block position to be air. This filters ordinary rejected breaks but cannot guarantee correctness on high-latency servers; waterlogged breaks that leave water are currently omitted. Multiplayer scores are not suitable for competitive verification.

Boss bars restyle the server's existing boss-bar data; they do not reveal extra entity health. Other mods that replace the same boss overlay may conflict; disable UltraStyle boss bars in that case. The style panel has manual positioning rather than automatic scoreboard avoidance.

## Develop

The Gradle wrapper is included; use JDK 25.

```sh
./gradlew build       # Compile, run unit tests, package the mod
./gradlew runClient   # Launch an isolated development client in run/
./gradlew genSources  # Generate Minecraft sources for IDE inspection
```

On Windows, use `gradlew.bat`. Generated output, development worlds, logs, caches, and local credentials are excluded by `.gitignore`; wrapper files remain trackable.

The pure Java scoring engine lives in `StyleState`. Client event handling, configuration and commands live in `UltraStyleClient`. `StyleHud` and `BossBars` draw the UI. Two client mixins observe vanilla damage/death packets and replace boss-bar drawing. No custom network protocol is needed.

## Manual playtest checklist

1. Enter a survival world, break blocks and ore, and verify the ten-second mining cap.
2. Kill mobs with melee and projectiles; make two kills within four seconds, then wait and repeat.
3. Take damage, die, respawn, change dimension, and reconnect; check combo and score reset behavior.
4. Enable `/ultrastyle xp 5`, kill a hostile mob, and check that the extra 500 XP is granted once. Disable rewards with `/ultrastyle xp 0`.
5. In a cheats-enabled test world, use `/bossbar add ultrastyle:test "TEST BOSS"` and `/bossbar set ultrastyle:test players @s`. Change its value to inspect the damage trail; add a second bar to inspect stacking.
6. Resize the window, change GUI scale, toggle F1, and toggle both overlays using the client commands.
7. Join an unmodified multiplayer server. Check local scoring and verify that enabling the XP option does not grant server XP.

Unit tests cover scoring and award amounts. Runtime rendering, server compatibility and gameplay need the manual checks above.
