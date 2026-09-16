# ShetiSakha - Smart Agricultural Robot Control System

**ShetiSakha** (Marathi: "Sheti" = Farming, "Sakha" = Companion) is an Android application designed to control an Arduino-based agricultural robot (agribot) over Bluetooth. It enables farmers to operate farm tasks like ploughing, sowing, sprinkling, and autonomous field navigation — all from a smartphone.

Built as a Final Year Project at **Government Polytechnic, Nagpur**.

---

## Table of Contents

- [How It Works — The Big Picture](#how-it-works--the-big-picture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Core Features](#core-features)
- [Application Screens & Flow](#application-screens--flow)
- [Bluetooth Communication Protocol](#bluetooth-communication-protocol)
- [Path Drawing & Conversion Algorithm](#path-drawing--conversion-algorithm)
- [Path Memorization & Replay](#path-memorization--replay)
- [Weather Integration](#weather-integration)
- [Database Design](#database-design)
- [Settings & Preferences](#settings--preferences)
- [Build Configuration](#build-configuration)
- [Permissions](#permissions)
- [How to Build & Run](#how-to-build--run)
- [Hardware Requirements](#hardware-requirements)
- [Known Limitations](#known-limitations)

---

## How It Works — The Big Picture

```
+-------------------+       Bluetooth (HC-05)       +-------------------+
|                   |  <===========================> |                   |
|   Android App     |    Single-byte commands (F,    |   Arduino-Based   |
|   (ShetiSakha)    |    B, L, R, S, 1-8)           |   Agribot         |
|                   |                                |   (Motors, Tools) |
+-------------------+                                +-------------------+
```

1. The user pairs their phone with the HC-05 Bluetooth module mounted on the robot.
2. The app sends single-byte commands over Bluetooth SPP (Serial Port Profile).
3. The Arduino on the robot receives these bytes and controls motors, sprinklers, seeders, and ploughs accordingly.
4. The user can choose between **manual control** (D-pad), **grid-based automatic path**, or **freehand drawing automatic path**.

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Language** | Java (100%) | All application logic |
| **Platform** | Android (SDK 24–34) | Target: Android 7.0 Nougat to Android 14 |
| **Build System** | Gradle 8.0 + AGP 8.1.2 | Compilation, dependency management |
| **UI Framework** | Android Views + XML Layouts | Traditional Android view system (no Jetpack Compose) |
| **Bluetooth** | Android Bluetooth API (SPP/RFCOMM) | Communication with HC-05 module |
| **Database** | SQLite (SQLiteOpenHelper) | Storing recorded robot paths |
| **Networking** | HttpURLConnection + AsyncTask | Fetching weather data from OpenWeatherMap API |
| **Navigation** | DrawerLayout + FragmentManager | Side navigation drawer with fragment switching |
| **Custom Views** | PathDrawView, PatternLockView | Freehand path drawing and grid-based path selection |
| **Theme** | Material Design 3 (MaterialComponents) | UI styling |

---

## Project Structure

```
rough/
├── app/
│   ├── build.gradle.kts                  # App-level build config (dependencies, SDK versions)
│   ├── google-services.json              # Firebase config (declared but unused)
│   └── src/main/
│       ├── AndroidManifest.xml           # App permissions, activity declarations
│       ├── java/com/shetisakha/
│       │   ├── MainActivity3.java        # Main screen — Weather + Navigation Drawer
│       │   ├── MainActivity4.java        # Manual Remote Control (D-pad + toggles)
│       │   ├── activity_grid.java        # Automatic Grid-Pattern Path Mode
│       │   ├── draw_shape.java           # Automatic Freehand Drawing Mode
│       │   ├── SettingsActivity.java     # App settings (sensitivity, scale, background)
│       │   ├── HttpRequest.java          # HTTP GET utility for API calls
│       │   ├── bluetooth/
│       │   │   └── BluetoothController.java   # Singleton Bluetooth SPP client
│       │   ├── classes/
│       │   │   ├── PathDrawView.java     # Custom View — freehand drawing canvas
│       │   │   ├── Line2D.java           # Geometry utility (line operations)
│       │   │   ├── PatternLockView.java  # Custom View — 8x4 dot grid for path input
│       │   │   ├── GridViewAdapter.java  # Grid adapter (unused in production)
│       │   │   ├── LineDrawingView.java  # Simple line drawing view (unused)
│       │   │   └── SettingsFragment.java # Preference-based settings fragment
│       │   ├── databases/
│       │   │   └── DatabaseHelper.java   # SQLite helper for path storage
│       │   └── [Fragments]
│       │       ├── homefragment.java     # Home screen fragment
│       │       ├── UsermanualFragment.java  # User manual
│       │       ├── aboutfragment.java    # About us
│       │       └── ContactFragment.java  # Contact info
│       └── res/
│           ├── layout/                   # 21 XML layout files
│           ├── drawable/                 # Icons, images, selectors
│           ├── menu/                     # Navigation drawer menu
│           ├── navigation/               # Navigation graphs (template only)
│           ├── values/                   # Colors, strings, themes, arrays
│           └── xml/                      # Preferences XML, backup rules
├── build.gradle.kts                      # Root build config
├── settings.gradle.kts                   # Project settings
├── gradle.properties                     # Gradle JVM args, AndroidX config
└── gradle/wrapper/                       # Gradle 8.0 wrapper
```

---

## Core Features

### 1. Manual Remote Control
Full joystick-style control of the robot using toggle buttons and a D-pad interface. The farmer can manually drive the robot and operate individual farm tools.

### 2. Grid-Pattern Automatic Mode
The farmer draws a path on an 8x4 dot grid. The app converts this pattern into a sequence of forward, left, and right commands that the robot follows automatically.

### 3. Freehand Drawing Automatic Mode
The farmer draws any shape freely on a canvas. The app simplifies the drawing using the Douglas-Peucker algorithm, converts it to robot commands, and sends them sequentially.

### 4. Path Memorization & Replay
The app records every movement and action during manual operation (direction + duration), stores it in a local SQLite database, and can replay the exact same path later — essentially teaching the robot a route once and automating it thereafter.

### 5. Weather Information
Real-time weather lookup by city name using the OpenWeatherMap REST API. Displays temperature, wind, humidity, pressure, and weather conditions to help farmers plan operations.

### 6. Navigation Drawer
Side-drawer menu with Home, User Manual, About Us, and Contact sections.

---

## Application Screens & Flow

```
                        +-------------------+
                        |   MainActivity3   |
                        |  (Home / Launcher)|
                        +-------------------+
                        |  Weather Display  |
                        |  Drive Buttons    |
                        |  Navigation Drawer|
                        +-------------------+
                       /          |          \
                      /           |           \
           +---------+    +------+-------+   +----------+
           | Manual  |    |   Grid Auto  |   | Freehand |
           | Control |    |     Mode     |   |   Draw   |
           +---------+    +--------------+   +----------+
           | MainActivity4|  | activity_grid| |draw_shape|
           +---------+    +--------------+   +----------+
                |                |                  |
                v                v                  v
          Bluetooth SPP    Bluetooth SPP     Bluetooth SPP
          to HC-05         to HC-05          to HC-05
```

### Screen Descriptions

| Screen | Activity/Fragment | What It Does |
|---|---|---|
| **Home** | `MainActivity3` | Displays weather info, provides buttons to enter Manual or Automatic mode. Navigation drawer for other sections. |
| **Manual Control** | `MainActivity4` | D-pad (Forward/Backward/Left/Right/Stop), toggle buttons for Sprinkler/Sowing/Plough Up/Plough Down, Start/Stop/Play for path recording, timer display. |
| **Grid Auto** | `activity_grid` | 8x4 dot grid (PatternLockView), toggle buttons for Irrigation/Ploughing/Seeding, Connect and Send buttons. |
| **Freehand Draw** | `draw_shape` | Canvas (PathDrawView) where user draws freely, Connect/Send/Settings buttons. |
| **Settings** | `SettingsActivity` | Preferences for simplification sensitivity, auto-loop sensitivity, modification sensitivity, driving-area scale, and custom background image. |
| **User Manual** | `UsermanualFragment` | Static instructions on how to use each feature. |
| **About** | `aboutfragment` | Project description and team info. |
| **Contact** | `ContactFragment` | Social media and contact channels. |

---

## Bluetooth Communication Protocol

The app communicates with an **HC-05 Bluetooth module** using the **SPP (Serial Port Profile)** over **RFCOMM**. Commands are sent as **single ASCII bytes**.

### Robot Movement Commands

| Byte | Meaning | Direction |
|---|---|---|
| `F` | Move Forward | Front |
| `B` | Move Backward | Back |
| `L` | Turn Left | Left |
| `R` | Turn Right | Right |
| `S` | Stop | — |

### Farm Tool Commands

| Byte | Action | Tool |
|---|---|---|
| `1` | Start | Sowing/Seeding |
| `2` | Stop | Sowing/Seeding |
| `3` | Start | Plough Down |
| `4` | Stop | Plough Down |
| `5` | Start | Plough Up |
| `6` | Stop | Plough Up |
| `7` | Start | Sprinkler/Irrigation |
| `8` | Stop | Sprinkler/Irrigation |

### BluetoothController Architecture

`BluetoothController` (`com.shetisakha.bluetooth.BluetoothController.java`) is implemented as a **thread-safe singleton**:

```
BluetoothController (Singleton)
├── Instance shared across all activities
├── connect()          → Background thread, connects to HC-05
├── send(String)       → Writes bytes to OutputStream
├── disconnect()       → Closes socket
├── isConnected()      → Returns connection state
├── requestEnable()    → Prompts user to enable Bluetooth
└── ConnectionListener → onConnected / onConnectionFailed / onDisconnected
```

- Targets HC-05 by device name
- Uses UUID `00001101-0000-1000-8000-00805F9B34FB` (Standard SPP UUID)
- Checks `BLUETOOTH_CONNECT` runtime permission before operations (Android 12+)

---

## Path Drawing & Conversion Algorithm

This is the most technically complex part of the application, implemented in `PathDrawView.java` (692 lines).

### How Freehand Path Becomes Robot Commands

```
User's Finger Movement
        |
        v
[1] Raw path points captured (x, y coordinates)
        |
        v
[2] Douglas-Peucker Line Simplification
    - Reduces point count while preserving shape
    - Sensitivity controlled by PREF_LIST setting (Low/Medium/High)
        |
        v
[3] Path Validation
    - Auto-loop detection (closes path if endpoints are close)
    - Point thinning (removes too-close points)
    - Error segment detection (blinking red highlights)
        |
        v
[4] Path-to-Command Conversion (toStringList)
    - Walk through consecutive point pairs
    - Compute direction vector between points
    - Calculate turning angle from previous segment
    - If angle >= 75 degrees → emit "R:<degrees>" or "L:<degrees>"
    - Otherwise → emit "F:<scaled_distance>"
    - Distance scaled by SCALE_EDITTEXT setting (cm → pixel ratio)
        |
        v
[5] Command Replay
    - Commands sent one-by-one over Bluetooth
    - Each command has a 1-second execution tick
    - Robot moves for the specified duration
```

### Douglas-Peucker Simplification

The algorithm recursively simplifies a polyline:

1. Draw a line from the first point to the last point of the segment.
2. Find the point farthest from this line.
3. If the farthest distance exceeds a threshold (epsilon):
   - Split the segment at that point.
   - Recursively simplify both halves.
4. If the distance is within threshold, discard all intermediate points.

The **epsilon** value is controlled by the user setting:
- **Low** → High simplification (fewer points, smoother paths)
- **Medium** → Balanced
- **High** → Low simplification (more points, more accurate to original drawing)

### Geometry Utilities (Line2D.java)

`Line2D.java` (479 lines) provides computational geometry methods replicating `java.awt.geom.Line2D`:

- `ptSegDist` — distance from point to line segment
- `ptSegDistSq` — squared distance (avoids sqrt for performance)
- `relativeCCW` — determines which side of a line a point is on
- `linesIntersect` — checks if two line segments cross
- `LineToPointDistance2D` — used by PathDrawView for touch-editing (finding closest point on path to a finger tap)

---

## Path Memorization & Replay

Implemented in `MainActivity4.java` with SQLite storage via `DatabaseHelper.java`.

### Recording Flow

```
User presses "Start Recording"
        |
        v
Timer begins (1-second ticks)
        |
        v
User drives robot (D-pad) + operates tools (toggles)
        |
        v
On each direction change or tool action:
    DatabaseHelper.InsertPath(direction, runtime)
    → INSERT INTO robopath (direction, runtime) VALUES (?, ?)
        |
        v
User presses "Stop Recording"
        |
        v
Complete path saved to SQLite
```

### Replay Flow

```
User presses "Play"
        |
        v
DatabaseHelper.GetPath("order by ID")
    → SELECT * FROM robopath ORDER BY ID
        |
        v
Cursor iterates through stored commands
        |
        v
For each row:
    BluetoothController.send(direction)
    Thread.sleep(runtime_in_seconds * 1000)
        |
        v
Robot replays the exact recorded path
```

### Timer Display

The timer in Manual mode shows elapsed time in `H:MM:SS` format, incremented every second via `CountDownTimer`.

---

## Weather Integration

Implemented in `MainActivity3.java` as an inner class `weatherTask` extending `AsyncTask`.

### API Details

| Property | Value |
|---|---|
| **Provider** | OpenWeatherMap |
| **Endpoint** | `https://api.openweathermap.org/data/2.5/weather` |
| **Method** | HTTP GET |
| **Parameters** | `q={city_name}`, `units=metric`, `appid={API_KEY}` |
| **HTTP Client** | `HttpURLConnection` (via `HttpRequest.java` utility) |

### Response Parsing (JSON)

```json
{
  "main": {
    "temp": 30.5,
    "temp_min": 28.0,
    "temp_max": 33.0,
    "pressure": 1012,
    "humidity": 75
  },
  "wind": { "speed": 3.5 },
  "weather": [{ "description": "scattered clouds" }],
  "sys": { "sunrise": 1234567, "sunset": 1234999 },
  "name": "Nagpur",
  "sys": { "country": "IN" }
}
```

### Displayed Information

- Current temperature (Celsius)
- Min/Max temperature
- Weather description (e.g., "scattered clouds")
- Wind speed
- Atmospheric pressure
- Humidity percentage
- Sunrise/Sunset times
- City and Country
- Last updated timestamp

---

## Database Design

### SQLite Database: `mempath.db`

#### Table: `robopath`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `ID` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique row identifier (insertion order) |
| `direction` | TEXT | NOT NULL | Bluetooth command byte (`F`, `B`, `L`, `R`, `S`, `1`-`8`) |
| `runtime` | TEXT | NOT NULL | Duration in seconds the command should execute |

#### DatabaseHelper Methods

| Method | SQL Operation | Purpose |
|---|---|---|
| `InsertPath(direction, runtime)` | `INSERT INTO robopath VALUES (null, ?, ?)` | Save a recorded command |
| `GetPath(condition)` | `SELECT * FROM robopath {condition}` | Retrieve recorded path |
| `DeleteScheduler()` | `DELETE FROM robopath` | Clear all recorded data |
| `CreateTable()` | `CREATE TABLE robopath(...)` | Create table if not exists |

---

## Settings & Preferences

Configured via `xml/prefrences.xml` and read by `PathDrawView` at runtime:

| Preference Key | Setting Name | Options | Effect |
|---|---|---|---|
| `PREF_LIST` | Simplification Sensitivity | Low, Medium, High | Controls Douglas-Peucker epsilon value. Low = more aggressive simplification, fewer points. |
| `LOOP_LIST` | Auto-Loop Sensitivity | Off, Low, High | Controls whether the path auto-closes. Off = no loop. Low/High = different closing distances. |
| `MODIFY_LIST` | Modification Sensitivity | Off, Low, High | Controls how easily the drawn path can be edited by tapping near it. |
| `SCALE_EDITTEXT` | Driving Area Scale | Numeric (cm) | Width of the real-world driving area in centimeters. Used to convert pixel distances to real-world commands. |
| `BACK_PREF` | Background Image | Gallery picker | Sets a custom background image on the drawing canvas (e.g., a photo of the actual field). |

---

## Build Configuration

### App-Level `build.gradle.kts`

```kotlin
android {
    namespace = "com.shetisakha"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shetisakha"
        minSdk = 24           // Android 7.0 Nougat minimum
        targetSdk = 34        // Android 14
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

### Dependencies

| Library | Version | Status |
|---|---|---|
| `androidx.appcompat:appcompat` | 1.6.1 | Active — Activity/Toolbar support |
| `com.google.android.material:material` | 1.9.0 | Active — Material Design components |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Active — Layout constraints |
| `androidx.navigation:navigation-fragment` | 2.7.2 | Declared — Template nav graphs only |
| `androidx.navigation:navigation-ui` | 2.7.2 | Declared — Template nav graphs only |
| `com.google.firebase:firebase-auth-ktx` | 22.1.2 | Imported — Not actively used |
| `com.android.volley:volley` | 1.2.0 | Declared — Not used in code |
| `com.squareup.picasso:picasso` | 2.71828 | Declared — Not used in code |
| `com.google.android.gms:play-services-location` | 17.0.0 | Declared — Not used in code |

### Gradle Configuration

| Property | Value |
|---|---|
| Gradle Version | 8.0 |
| Android Gradle Plugin | 8.1.2 |
| JVM Args | `-Xmx2048m -Dfile.encoding=UTF-8` |
| AndroidX Enabled | Yes |
| Non-Transitive R Classes | Yes |
| BuildConfig Generation | Enabled |

---

## Permissions

| Permission | Purpose | When Required |
|---|---|---|
| `INTERNET` | Fetching weather data from OpenWeatherMap API | Weather screen |
| `BLUETOOTH` | Basic Bluetooth operations (pre-Android 12) | All Bluetooth features |
| `BLUETOOTH_CONNECT` | Bluetooth operations on Android 12+ | Runtime permission request |
| `BLUETOOTH_SCAN` | Scanning for nearby Bluetooth devices | Pairing with HC-05 |

---

## How to Build & Run

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 11** or higher
- **Android SDK 34** with build-tools 33.0.1
- Gradle 8.0 (included via wrapper)

### Steps

1. **Clone or copy** the project to your local machine.
2. **Regenerate `local.properties`** — delete the existing file; Android Studio will create it pointing to your local Android SDK path.
3. Open the project in **Android Studio**.
4. Let Gradle sync complete.
5. Connect an Android device (API 24+) or start an emulator.
6. Click **Run** (or `./gradlew installDebug` from terminal).

### Building the APK

```bash
./gradlew assembleDebug       # Debug APK
./gradlew assembleRelease     # Release APK (unsigned)
```

The APK will be generated at `app/build/outputs/apk/`.

---

## Hardware Requirements

The app is designed to work with a physical agricultural robot. The expected hardware setup:

| Component | Purpose |
|---|---|
| **Arduino UNO/Nano** | Microcontroller running motor control firmware |
| **HC-05 Bluetooth Module** | Wireless communication between phone and Arduino |
| **DC Motors (x4)** | Wheel drive for robot movement (tank-drive steering) |
| **Motor Driver (L298N)** | Interface between Arduino and motors |
| **Sprinkler Pump** | Water sprinkling system |
| **Servo/Motor for Seed Dispenser** | Controlled seeding mechanism |
| **Plough Mechanism** | Up/down ploughing attachment |

### Bluetooth Pairing

1. Power on the robot (HC-05 module should blink rapidly).
2. On the Android phone, go to Settings > Bluetooth and pair with "HC-05" (default PIN: `1234` or `0000`).
3. Open ShetiSakha, navigate to the desired control mode.
4. Tap **Connect** to establish the SPP connection.

---

## Known Limitations

1. **No authentication or user accounts** — Firebase Auth is declared but not implemented. The app is single-user.
2. **Hardcoded API key** — The OpenWeatherMap API key is embedded directly in `MainActivity3.java`. This should be moved to a secure configuration.
3. **AsyncTask deprecation** — The weather task uses `AsyncTask`, which is deprecated since Android API 30. Modern apps should use coroutines or `java.util.concurrent`.
4. **No Kotlin** — The entire project is in Java. Migrating to Kotlin would bring null safety, coroutines, and modern Android idioms.
5. **Unused dependencies** — Volley, Picasso, and Location Services are declared but never used.
6. **No ProGuard/R8 obfuscation** — Release builds have minification disabled.
7. **Template fragments unused** — Navigation Component graphs (`nav_graph.xml`, `nav_graph2.xml`, `nav_graph3.xml`) and their associated First/Second fragments are template code that is not wired into the app.
8. **Database asset copy** — `DatabaseHelper` attempts to copy `mempath.db` from an `assets/` folder that does not exist. The fallback `CreateTable()` handles this gracefully, but the copy code is dead.
9. **No error handling for Bluetooth disconnects** during command replay.
10. **Single Bluetooth device targeting** — Hardcoded to connect to a device named "HC-05".

---

## License

This project was developed as a Final Year Project at **Government Polytechnic, Nagpur**. For academic use only.

---

**ShetiSakha** — Bringing technology to the farmer's field.
