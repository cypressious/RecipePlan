# RecipePlan - Kotlin Multiplatform Recipe Planning App

RecipePlan is a Kotlin Multiplatform (Android, JVM) app for planning meals and saving recipes using Compose Multiplatform. Data is synced with Google Sheets and can be shared with other users.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Critical Network Limitation

**IMPORTANT**: This repository currently requires access to `dl.google.com` (Google's Android repository) to build successfully. In sandboxed environments where this domain is blocked, builds will fail with dependency resolution errors for androidx components.

**Build Status in Restricted Environments**: 
- ❌ Full builds fail due to network restrictions (dl.google.com blocked)
- ✅ Project structure analysis works
- ✅ Gradle wrapper and Java 17 setup works
- ✅ Task listing works: `./gradlew tasks` (3-4 seconds)
- ✅ Clean operations work: `./gradlew clean` (< 1 second)

## Working Effectively

### Prerequisites
- **Java 17**: OpenJDK 17.0.16+ (already configured in environment)
- **Gradle 8.14.3**: Via wrapper (`./gradlew`)
- **Network access**: Requires dl.google.com for full builds
- **Google Cloud credentials**: Required for Google Sheets integration

### Essential Commands That Always Work

**Quick validation commands (use these to verify setup):**
```bash
# Check Java version (should be 17+)
java --version

# Check Gradle wrapper (downloads if needed, ~30 seconds first time)
./gradlew --version

# List all available tasks (3-4 seconds)
./gradlew tasks --console=plain

# Clean build artifacts (< 1 second)
./gradlew clean --console=plain

# Get help for specific task
./gradlew help --task <taskname>
```

### Build Commands (Network Dependent)

**NEVER CANCEL BUILDS**: All build commands may take 5-15 minutes. Set timeout to 30+ minutes minimum.

```bash
# JVM JAR build (5-10 minutes) - CI target
./gradlew :composeApp:jvmJar --console=plain

# Android Debug build (8-15 minutes) - CI target  
./gradlew :composeApp:assembleDebug --console=plain

# Full build with tests (10-20 minutes)
./gradlew build --console=plain

# Run JVM application (requires credentials.json)
./gradlew run

# Hot reload development mode (fastest iteration)
./gradlew hotRunJvm --auto

# Run tests (3-5 minutes)
./gradlew test --console=plain
```

### Hot Reload Development

**Primary development workflow** (when builds work):
```bash
# Start hot reload with auto-recompilation
./gradlew hotRunJvm --auto

# In another terminal, make code changes
# Changes automatically reload in running app
```

Available hot reload options:
- `--auto`: Automatic recompilation on file changes  
- `--debug-jvm`: Enable debugging on port 5005
- `--mainClass=...`: Override main class if needed

## Project Structure

### Repository Layout
```
RecipePlan/
├── .github/
│   ├── workflows/build.yml       # CI configuration
│   └── copilot-instructions.md   # This file
├── composeApp/                   # Main application module
│   ├── src/
│   │   ├── commonMain/kotlin/de/rakhman/cooking/
│   │   │   ├── ui/              # Compose UI components
│   │   │   │   ├── App.kt       # Root app component
│   │   │   │   ├── PlanScreen.kt # Meal planning screen
│   │   │   │   └── RecipesScreen.kt # Recipe management
│   │   │   ├── states/          # State management
│   │   │   └── events/          # Event handling
│   │   ├── commonMain/sqldelight/de/rakhman/cooking/
│   │   │   ├── Recipes.sq       # Recipe database schema
│   │   │   ├── Plan.sq          # Meal plan database
│   │   │   └── Settings.sq      # App settings
│   │   ├── jvmMain/kotlin/de/rakhman/cooking/
│   │   │   ├── main.kt          # JVM app entry point
│   │   │   ├── backend/         # Google Sheets integration  
│   │   │   └── database/        # JVM SQLite driver
│   │   └── androidMain/kotlin/de/rakhman/cooking/
│   │       ├── MainActivity.kt  # Android entry point
│   │       └── background/      # Background sync workers
│   └── build.gradle.kts         # Module configuration
├── gradle/
│   ├── libs.versions.toml       # Dependency version catalog
│   └── wrapper/                 # Gradle wrapper files
├── build.gradle.kts             # Root build configuration
├── settings.gradle.kts          # Project settings
└── README.md                    # Project documentation
```

### Key Application Components

**Main Application Flow:**
1. **JVM Entry**: `main.kt` creates window, initializes database
2. **Authentication**: Google OAuth for Sheets API access
3. **UI Layer**: Compose Multiplatform with navigation between:
   - Recipe Plan screen (meal planning)
   - Recipes screen (recipe management) 
   - Add/Edit screens (recipe entry)
4. **Data Layer**: SQLDelight local database + Google Sheets sync
5. **Background Sync**: Android WorkManager for periodic updates

**Database Schema** (SQLDelight):
- `recipe`: id, title, url, counter, tags
- `plan`: meal planning data
- `settings`: app configuration including sheet ID

## Authentication Setup

### For JVM/Desktop Development:
1. Create project in [Google Cloud Console](https://console.cloud.google.com)
2. Enable Google Sheets API
3. Create Desktop OAuth 2.0 Client ID
4. Download `credentials.json` → place in `composeApp/src/jvmMain/resources/`
5. Run app: `./gradlew run`

### For Android Development:
1. Create Android OAuth 2.0 Client ID
2. Use package name: `de.rakhman.cooking`
3. Add keystore SHA1 fingerprint
4. Configure in Android project

## Validation Scenarios

**Always test through these scenarios after making changes:**

### 1. JVM Application Validation
```bash
# Build JVM JAR
time ./gradlew :composeApp:jvmJar --console=plain

# Run application  
./gradlew run

# Expected behavior:
# - Window opens: 400x700px, titled "Recipe Plan"
# - Always on top, positioned at (1100, 50)
# - Navigation between "Plan" and "Recipes" tabs  
# - Google Sheets authentication prompt (if no credentials)
# - Recipe list loads (if authenticated)
```

### 2. Hot Reload Development Validation
```bash
# Start hot reload
./gradlew hotRunJvm --auto

# In editor: Make UI change in App.kt or PlanScreen.kt
# Expected: Change reflects immediately in running app
# Test: Modify text, colors, layout - should update live
```

### 3. Database Validation  
```bash
# Check SQLDelight code generation
./gradlew generateSqlDelightInterface

# Verify database migrations
./gradlew verifySqlDelightMigration

# Expected: No errors, generated Database.kt in build/
```

### 4. Android Validation (When Network Allows)
```bash
# Build Android APK
time ./gradlew :composeApp:assembleDebug --console=plain

# Expected output:
# - APK at: composeApp/build/outputs/apk/debug/composeApp-debug.apk
# - Install and test basic app functionality
```

## Build Troubleshooting

### Network Access Issues
```bash
# Test Google repository access
curl -s -o /dev/null -w "%{http_code}" https://dl.google.com

# If 000: Network blocked, builds will fail
# If 200: Network accessible, builds should work
```

**Known Error Pattern:**
```
Could not GET 'https://dl.google.com/dl/android/maven2/...'
> dl.google.com: No address associated with hostname
```

**Solution**: Document limitation, cannot build in restricted environments.

### Version Compatibility Issues
- **Original AGP**: `8.12.0` (future version, may not exist)
- **Working AGP**: `8.5.0` or latest stable
- **Fix**: Update `agp = "8.5.0"` in `gradle/libs.versions.toml`

### Configuration Warnings (Safe to Ignore)
```
w: Usage of Internal Kotlin Gradle Plugin Properties Detected
Parallel Configuration Cache is an incubating feature.
```

## Performance Characteristics

### Timing Expectations
- **Gradle daemon startup**: 30-60 seconds (first time)
- **Task listing**: 3-4 seconds  
- **Clean**: < 1 second
- **JVM build**: 5-10 minutes (full), 1-2 minutes (incremental)
- **Android build**: 8-15 minutes (full), 2-3 minutes (incremental)
- **Tests**: 3-5 minutes
- **Hot reload**: < 5 seconds per change

### Development Performance Tips
- Use `./gradlew --console=plain` for cleaner output
- Enable hot reload: `./gradlew hotRunJvm --auto` for fastest iteration
- Use configuration cache (enabled by default)
- Parallel builds enabled in `gradle.properties`

## CI/CD Integration

**GitHub Actions** (`.github/workflows/build.yml`):
- **Triggers**: Push to main, PRs to main
- **Java**: 17 (Temurin distribution)
- **Caching**: Gradle dependencies cached
- **Build targets**:
  - JVM: `./gradlew :composeApp:jvmJar`
  - Android Debug: `./gradlew :composeApp:assembleDebug`

**Publishing** (`.github/workflows/publish-android.yml`):
- **Triggers**: Push to main
- **Keystore**: Base64 encoded in secrets
- **Output**: Google Play Store (alpha track)

## Common Development Tasks

### Adding Dependencies
1. **Edit version catalog**: `gradle/libs.versions.toml`
2. **Add to build file**: `composeApp/build.gradle.kts`
3. **Choose source set**: `commonMain`, `jvmMain`, or `androidMain`

Example:
```kotlin
commonMain.dependencies {
    implementation(libs.new.dependency)
}
```

### Working with UI Components
- **Location**: `composeApp/src/commonMain/kotlin/de/rakhman/cooking/ui/`
- **Entry point**: `App.kt` 
- **Navigation**: Bottom bar switches between screens
- **Theming**: `Theme.kt` defines Material3 colors
- **State management**: Uses Evas library (events/states)

### Database Development
- **Schema files**: `composeApp/src/commonMain/sqldelight/de/rakhman/cooking/*.sq`
- **Generate code**: `./gradlew generateSqlDelightInterface`
- **Access pattern**: Database → Queries → Flows → UI

### Google Sheets Integration
- **Backend**: `composeApp/src/jvmMain/kotlin/de/rakhman/cooking/backend/`
- **Entry point**: `SheetsQuickstart.kt`
- **Authentication**: OAuth2 flow with local credentials
- **Sync pattern**: Bi-directional sync between local DB and Sheets

## Limitations in Restricted Environments

**Cannot do without network access:**
- Build any targets (JVM, Android)
- Download dependencies
- Test authentication flows
- Validate Google Sheets integration

**Can do without network:**
- Analyze code structure
- Understand project architecture  
- Review database schemas
- Plan development changes
- Document findings

**Always document limitations rather than attempting workarounds that won't work in restricted environments.**