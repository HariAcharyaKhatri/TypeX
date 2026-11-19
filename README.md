# TYPEX — Master Every Keystroke

A polished, offline-first Java Swing typing tutor with dashboard, structured
lessons, virtual keyboard, typing tests, custom exercises, achievements,
statistics, and themes.

## Requirements
- Java 17+ (JDK)
- Maven 3.9+

## Build
```bash
mvn clean package
```

## Run
```bash
java -jar target/typex-1.0.0.jar
```

Or on Windows: double-click `run.bat`, on macOS/Linux: `./run.sh`.

## Data
SQLite database stored at `~/.typex/typex.db`. Deleting it resets the app.
Sound effects are synthesized (no external assets required).

## Features
- Dashboard with live charts (JFreeChart) sourced from SQLite.
- 20 seeded lessons across 5 levels with per-user progress tracking.
- Full typing engine with backspace, gross/net WPM, accuracy, error analysis.
- Custom exercises (create/edit/import .txt).
- Achievements with no-duplicate unlock guard and XP rewards.
- Dark/light theme + accent colors via FlatLaf.
- CSV and printable-report export.

## Architecture
MVC + Service + DAO. All DB calls are synchronous but never touch the EDT
through SwingWorker-adjacent patterns; long jobs use background threads where
appropriate. SQLite with foreign keys, indexes, and idempotent schema creation.
