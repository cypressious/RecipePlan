# RecipePlan - Kotlin Multiplatform Recipe Planning App

RecipePlan is a Kotlin Multiplatform (Android, JVM) app for planning meals and saving recipes using Compose Multiplatform. Data is synced with Google Sheets and can be shared with other users.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Critical Network Limitation

**IMPORTANT**: This repository currently requires access to `dl.google.com` (Google's Android repository) to build successfully. In sandboxed environments where this domain is blocked, builds will fail with dependency resolution errors for androidx components.

**Build Status**: 
- ❌ Full builds fail due to network restrictions (dl.google.com blocked)
- ✅ Project structure analysis works
- ✅ Gradle wrapper and Java 17 setup works
- ✅ Task listing works: `./gradlew tasks` (3-4 seconds)

## Working Effectively

### Prerequisites
- Java 17 (OpenJDK 17.0.16+ recommended)
- Gradle 8.14.3 (via wrapper)
- Network access to Google repositories (dl.google.com) for full builds

### Essential Commands

**NEVER CANCEL BUILDS**: All build commands may take 5-15 minutes. Set timeout to 30+ minutes minimum.

#### Quick Status Check (Always works - 3-4 seconds)
```bash
./gradlew tasks --console=plain
```

#### Expected Build Commands (Network dependent)
- **JVM JAR build**: `./gradlew :composeApp:jvmJar` - Expected time: 5-10 minutes
- **Android Debug build**: `./gradlew :composeApp:assembleDebug` - Expected time: 8-15 minutes  
- **Full build**: `./gradlew build` - Expected time: 10-20 minutes
- **Clean build**: `./gradlew clean` - Expected time: 1-2 minutes

#### Development Commands (When builds work)
- **Run JVM app**: `./gradlew run` or `./gradlew :composeApp:jvmRun`
- **Hot reload JVM**: `./gradlew hotRunJvm` 
- **Run tests**: `./gradlew test` - Expected time: 3-5 minutes

### Project Structure

#### Key Files and Directories
```
/
├── composeApp/                    # Main application module
│   ├── src/jvmMain/kotlin/        # JVM/Desktop specific code
│   │   └── de/rakhman/cooking/main.kt  # JVM entry point
│   ├── src/androidMain/kotlin/    # Android specific code
│   │   └── de/rakhman/cooking/MainActivity.kt
│   ├── src/commonMain/kotlin/     # Shared Kotlin code
│   │   └── de/rakhman/cooking/ui/ # Compose UI components
│   └── build.gradle.kts           # Module build configuration
├── gradle/libs.versions.toml      # Version catalog
├── build.gradle.kts               # Root build configuration
└── README.md                      # Project documentation
```

#### Core Components
- **Main App Entry**: `composeApp/src/jvmMain/kotlin/de/rakhman/cooking/main.kt`
- **Google Sheets Integration**: `composeApp/src/*/kotlin/de/rakhman/cooking/backend/`
- **Database Layer**: Uses SQLDelight for local storage
- **UI Layer**: Compose Multiplatform components in `ui/` directory

## Authentication Setup

The app requires Google Sheets API credentials:

### For JVM/Desktop:
1. Create a project in [Google Cloud Console](https://console.cloud.google.com)
2. Enable the Google Sheets API
3. Create a Desktop OAuth 2.0 Client ID
4. Download `credentials.json` and place in `composeApp/src/jvmMain/resources/`

### For Android:
1. Create an Android OAuth 2.0 Client ID using package name `de.rakhman.cooking`
2. Use your keystore's SHA1 fingerprint

## Build Troubleshooting

### Known Issues
1. **Android Gradle Plugin Version**: Original version `8.12.0` may not exist - use `8.5.0` or latest stable
2. **Network Access**: Builds require `dl.google.com` access for androidx dependencies
3. **Configuration Cache**: May show incubating feature warnings (safe to ignore)

### Workarounds for Network Issues
If builds fail with "dl.google.com" errors:
1. Verify network connectivity: `curl -s -o /dev/null -w "%{http_code}" https://dl.google.com`
2. Try offline mode if dependencies cached: `./gradlew --offline [task]`
3. In restricted environments, document the limitation rather than attempting fixes

### Expected Build Output
- **Success indicators**: Tasks complete with "BUILD SUCCESSFUL"
- **Timing expectations**: Initial builds 5-15 minutes, incremental 1-3 minutes
- **JAR location**: `composeApp/build/libs/composeApp-jvm.jar`
- **Android APK**: `composeApp/build/outputs/apk/debug/`

## Validation Scenarios

**After making changes, always validate through these scenarios:**

### JVM Application Validation
1. Build: `./gradlew :composeApp:jvmJar` 
2. Run: `./gradlew run`
3. Verify window opens (400x700px, titled "Recipe Plan")
4. Test basic navigation between Recipe Plan and Recipes tabs
5. Verify Google Sheets integration prompts for authentication

### Android Application Validation (When possible)
1. Build: `./gradlew :composeApp:assembleDebug`
2. Install APK on device/emulator
3. Test app launch and basic functionality
4. Verify Google authentication flow

### Code Quality Validation
- **Format check**: `./gradlew ktlintCheck` (if available)
- **Test execution**: `./gradlew test`
- **Clean build test**: `./gradlew clean build`

## CI/CD Integration

The repository uses GitHub Actions (`.github/workflows/build.yml`):
- **JVM Build**: `./gradlew :composeApp:jvmJar`
- **Android Build**: `./gradlew :composeApp:assembleDebug`
- **Java 17 required**: Uses setup-java@v4 with Temurin distribution
- **Caching**: Gradle caching enabled

## Common Development Tasks

### Adding New Dependencies
1. Edit `gradle/libs.versions.toml` for version management
2. Add to appropriate `sourceSets` in `composeApp/build.gradle.kts`
3. Consider platform-specific dependencies (`jvmMain`, `androidMain`, `commonMain`)

### Working with Compose UI
- **UI components**: Located in `composeApp/src/commonMain/kotlin/de/rakhman/cooking/ui/`
- **Hot reload**: Use `./gradlew hotRunJvm` for rapid development
- **Platform-specific UI**: Override in respective `Main` source sets

### Database Changes (SQLDelight)
- **Schema location**: Look for `.sq` files in the project
- **Generate code**: `./gradlew generateSqlDelightInterface`
- **Verify migrations**: `./gradlew verifySqlDelightMigration`

## Performance Notes

- **Initial Gradle daemon startup**: 30-60 seconds
- **Configuration cache**: Speeds up subsequent builds
- **Multiplatform compilation**: JVM builds faster than Android
- **Hot reload**: Fastest iteration for UI changes (when working)

## Limitations in Restricted Environments

- **Cannot build without Google repository access**
- **Cannot test Android builds without proper development setup**
- **Cannot validate Google Sheets integration without credentials**
- **Document limitations rather than attempting workarounds**

Always prioritize documenting what works versus what requires external dependencies or network access.