# JourneyOfJeph

A libGDX action game following the journey of **Jeph**, a high-tech cyborg tortoise on a mission to defend his territory against an endless onslaught of evil hares.

## Game Overview

In **Journey of Jeph**, you take control of a cybernetically enhanced tortoise. Each level, you must clear the map of hostile hares to unlock the path to the next stage. As you progress, the hares become stronger and more numerous, with powerful Boss Hares appearing every 5 levels.

Collect coins from fallen enemies to purchase additional health from the Shopkeeper or upgrade your weapons' damage at the Upgrader NPC.

## Controls

### Movement
- **WASD** or **Arrow Keys**: Move Jeph around the map.

### Combat
- **Space Bar**: Perform a **Chomp** attack. (Also used to interact with NPCs and the Shop).
- **E Key**: Fire a **Missile** (requires missile ammo).
- **Q Key**: **Spin Lunge**. Hold the key down to charge Jeph's spin; holding for 2+ seconds will result in a double-damage lunge.

### General
- **C Key**: View the **Credits** screen (from the Start menu).
- **R Key**: **Restart** the journey (from the Game Over screen).

## Tech Stack

- **Framework**: [libGDX](https://libgdx.com/)
- **Language**: Java
- **Platforms**: Desktop (LWJGL3)
- **Asset Creation**: Claude Code (Anthropic)
- **Development Assistant**: Gemini (Google)

## Project Structure

- `core/`: Main game logic and shared application code.
- `lwjgl3/`: Desktop-specific launcher and configuration.
- `assets/`: 8-bit sprites, TMX maps, and SFX (Chomp, Explosion, Spin).
- `docs/`: Design documents, issue tracking, and AI interaction logs.

## Development

This project follows strict development guidelines defined in `AGENTS.md`. All AI interactions are logged in `docs/ai-log.md` for auditability and project tracking.

## Gradle

Use the included Gradle wrapper to build and run the project:

- `lwjgl3:run`: Starts the application.
- `lwjgl3:jar`: Builds a runnable JAR file in `lwjgl3/build/libs`.
- `clean`: Removes build folders.
