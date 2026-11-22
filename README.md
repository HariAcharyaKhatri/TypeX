# TYPEX — Master Every Keystroke

TYPEX is a high-performance, offline-first desktop typing tutor and arcade trainer built with Java 17 and Java Swing. It features a complete learning curriculum, realistic virtual keyboard emulation, an extensive text library, arcade typing games, gamification with XP progression, live analytics charts, and customizable themes.

---

## 🚀 Key Highlights & Features

### 1. 100-Lesson Structured Curriculum
- **5 Mastery Tiers**: Beginner (Home Row & foundation), Elementary (Top & Bottom rows), Intermediate (Capital letters, numbers & punctuation), Advanced (Complex vocabulary & endurance), and Professional (Technical transcription & high-speed speedruns).
- **20 Distinct Lessons Per Tier**: Step-by-step finger progression, objective descriptions, minimum WPM and accuracy gating, attempt counts, and persistent personal bests.

### 2. 12 Categorized Typing Test Libraries & Custom Creator
- Over 60+ pre-seeded passages across 12 realistic categories:
  1. **Common English**: Everyday conversational vocabulary and high-frequency word sequences.
  2. **Tech & Programming**: Real source syntax across Java, Python, JavaScript, HTML, CSS, and SQL.
  3. **Business & Corporate**: Professional correspondence, quarterly reports, and executive memos.
  4. **Quotes & Speeches**: Historic oratorical passages and timeless wisdom.
  5. **Classic Literature**: Passages from world-renowned novels and classic prose.
  6. **Science & Nature**: Technical biology, physics, astronomical descriptions, and chemistry.
  7. **History & Law**: Constitutional excerpts, judicial opinions, and historical events.
  8. **Medical & Health**: Anatomical, pharmaceutical, and healthcare terminology.
  9. **Tongue Twisters**: High-frequency phonetic obstacle courses for finger agility.
  10. **Numeric & Symbols**: Financial accounts, mathematical expressions, dates, and currency symbols.
  11. **Philosophy**: Deep epistemological and ethical discussions.
  12. **Poetry & Lyrics**: Rhythmic verses, stanza breaks, and stylized punctuation.
- **Custom Typing Test Creator**: Allows users to input their own custom passages with difficulty and target duration settings.

### 3. 8 Interactive Arcade Typing Games
- **Word Rush**: High-speed streak challenge with cascading word multipliers.
- **Falling Words**: Sky-fall matrix where words drop toward the danger threshold.
- **Typing Survival**: Race against a depleting vitality meter; correct words add time, mistakes penalize.
- **Letter Invaders**: Retro space defense where typing letter beacons launches defense lasers.
- **Sentence Sprint**: Drag-race time trial racing against a ghost pace marker.
- **Key Master**: Motor reflex trainer prompting individual layout keys with reaction time measurement.
- **Code Runner**: Fast-paced programming syntax speedrun with heavy bracket and symbol usage.
- **Number Ninja**: Rapid-fire numeric slicing for currency ($1,250.00), percentages, and formulas.

### 4. Layout-Aware Virtual Keyboard & Trainer
- **6 Physical Keyboard Layouts**:
  - `US QWERTY` (Standard US ANSI)
  - `UK QWERTY` (UK ISO layout with British symbols)
  - `German QWERTZ` (QWERTZ layout with Z/Y swap)
  - `French AZERTY` (AZERTY layout with shifted numeric row)
  - `Dvorak Simplified` (Ergonomic layout optimized for hand alternation)
  - `Colemak` (Modern ergonomic layout preserving common shortcut positions)
- **Automatic Layout Resolution**: Robust resolution matching layout IDs, display names, and common aliases with a safe fallback to US QWERTY.
- **Dynamic Layout Drills**: Home Row, Top Row, Bottom Row, Left/Right Hand, and individual finger drills dynamically query layout key definitions rather than relying on hardcoded QWERTY strings.
- **Opposite-Hand Shift Guidance**: Visual keyboard and instruction banners dynamically instruct typing with the active finger plus opposite-hand Shift (e.g. `Left Index Finger + Right Shift` for uppercase `F`).
- **Real-Time Heatmap**: Toggle between live finger guide and personal error frequency heatmap based on character mistake records.

### 5. Gamification, Personal Bests & Time Accounting
- **Monotonic System Timing**: Uses `System.nanoTime()` with strict exclusion of paused durations, ensuring pause intervals never artificially degrade WPM or inflate elapsed session time.
- **Atomic Database Transactions**: All game session records, XP additions, and personal best updates are committed atomically within SQLite transactions via `DatabaseManager.executeTransaction(...)`.
- **Independent Personal-Best Metrics**: High Score, Highest Level, Best WPM, and Best Accuracy are tracked independently via SQL `MAX(...)` updates, ensuring WPM records are never discarded on lower-scoring runs.
- **Dynamic Favorite Game Calculation**: Deterministic calculation identifying the most practiced arcade mode with tie-breaking rules and "No games yet" zero-state handling.
- **XP Progression & Leveling**: Centralized XP reward system powering level-ups, milestone achievements, and streak trackers.

### 6. Icon System & UI/UX
- **Centralized Vector Icon Manager (`IconManager`)**: 100% custom Java2D vector icon drawing, completely eliminating missing glyphs, missing resource assets, or scaling artifacts on high-DPI displays.
- **Modern Light/Dark Theme Support**: Professional light theme and high-contrast dark theme powered by FlatLaf with customizable accent colors.
- **Live Analytics**: Detailed WPM, accuracy, consistency, error distribution, and activity charts rendered via JFreeChart.

---

## 🛠 Technology Stack

- **Language**: Java 17 (JDK)
- **UI Framework**: Java Swing
- **Build Tool**: Apache Maven
- **Database**: SQLite 3 (WAL mode, foreign key enforcement, thread-safe connection pooling)
- **Look and Feel**: FlatLaf 3.4.1
- **Charting**: JFreeChart 1.5.4
- **Testing**: JUnit 5 (59 unit, integration, and quality regression tests)

---

## 🏗 Architecture

TYPEX follows a strict **Model-View-Controller (MVC) + DAO + Service** architecture:
- `com.typex.engine`: Monotonic keystroke processor, WPM/accuracy calculators, rhythm & pause mechanics.
- `com.typex.dao`: Data access objects with thread-safe SQLite connection management and atomic transactions.
- `com.typex.database`: Database initialization, WAL configuration, idempotent table migrations, and curriculum seeding.
- `com.typex.model`: Immutable models and domain enumerations (`GameType`, `KeyboardLayout`, `User`, `GameSession`, `Lesson`).
- `com.typex.service`: Business logic for gamification, recommendations, statistics, and dynamic keyboard drills.
- `com.typex.game`: Abstract game foundations (`BaseGamePanel`) and 8 interactive game implementations.
- `com.typex.ui`: Swing views, navigation routing, virtual keyboard components, dialogs, and panels.
- `com.typex.util`: Vector icon rendering (`IconManager`), synthesized audio (`SoundManager`), and text generation.

---

## 💻 Prerequisites & Setup

### Requirements
- **Java Development Kit (JDK)**: Java 17 or higher
- **Apache Maven**: Version 3.9+

### Build & Package
To compile, run test suites, and generate the standalone shaded JAR:
```bash
mvn clean package
```

### Run Application
Run the packaged executable JAR:
```bash
java -jar target/typex-1.0.0.jar
```

Alternatively, use the convenience launch scripts:
- **Windows**: `run.bat`
- **Linux / macOS**: `./run.sh`

---

## 🧪 Automated Test Suite

TYPEX includes 59 automated test cases covering core engine calculations, database persistence, layout resolution, dynamic drills, game pause accounting, independent personal bests, and UI navigation:
```bash
mvn test
```

| Test Class | Focus Area |
| :--- | :--- |
| `TypingEngineTest` | Keystroke tracking, gross/net WPM, accuracy, error correction |
| `Phase51QualityTest` | Layout resolution, dynamic drills, pause accounting, atomic XP, independent PBs |
| `Phase5ExpansionTest` | Vector icon rendering, 100 lessons, 12 test categories, 8 game instances |
| `DatabasePersistenceTest` | User profiles, settings, test results, personal bests, and streak counts |
| `GamificationServiceTest` | XP progression, level milestones, daily goal verification |
| `KeyboardTrainerTest` | Virtual keyboard highlights and finger mapping |
| `Phase4DesignAndLayoutTest` | UI typography, contrast tokens, layout switching, font sizing |
| `StatisticsServiceTest` | Historical aggregates, accuracy percentages, speed trends |
| `RecommendationServiceTest` | Algorithmic practice recommendations based on error patterns |
| `TextGeneratorTest` | Deterministic passage generation and vocabulary synthesis |
| `UIAndNavigationTest` | TopBar, navigation state, and card switching integration |

---

## 📁 User Data Storage

All user profiles, progress, settings, typing sessions, and personal bests are saved locally in an offline SQLite database:
- **Location**: `~/.typex/typex.db`
- **Audio**: Sound effects are generated synthetically using Java Sound API (no external MP3/WAV files required).
