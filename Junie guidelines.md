# Junie Guidelines for RecipePlan

## Project Overview

RecipePlan is a Kotlin Multiplatform application (Android, JVM) designed for meal planning and recipe management. The application uses Compose Multiplatform for its UI and provides seamless synchronization with Google Sheets, allowing users to share their recipe data with others.

### Key Features

- **Cross-platform support**: Works on Android devices and desktop (JVM)
- **Google Sheets synchronization**: Sync and share recipe data across devices and users
- **Google Account authentication**: Secure access to your recipe data
- **URL recipe extraction**: Automatically fetch recipe names from URLs
- **Bring integration**: Share ingredients to the Bring shopping list app
- **Meal planning**: Organize your meals with an intuitive planning interface
- **Recipe management**: Save and categorize your favorite recipes

## Project Structure

The project follows the Kotlin Multiplatform structure:

- `/composeApp`: Contains all application code
  - `/commonMain`: Shared code for all platforms
  - `/androidMain`: Android-specific code
  - `/jvmMain`: Desktop (JVM) specific code
- Build configuration: Gradle with Kotlin DSL

## Setup Instructions

### Prerequisites

- JDK 11 or higher
- Android Studio or IntelliJ IDEA
- Google Cloud Console project with Sheets API enabled

### Google Cloud Setup

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com)
2. Enable the Google Sheets API for your project

### Desktop (JVM) Setup

1. Create a Desktop OAuth 2.0 Client ID in Google Cloud Console
2. Download the credentials.json file
3. Place the credentials.json in `composeApp/src/jvmMain/resources/`
4. Open the project in IntelliJ IDEA
5. Run `composeApp/src/jvmMain/kotlin/de/rakhman/cooking/main.kt`

### Android Setup

1. Create an Android OAuth 2.0 Client ID using:
   - Package name: `de.rakhman.cooking`
   - Your keystore's SHA1 fingerprint ([tutorial](https://developers.google.com/android/guides/client-auth))
2. Open the project in IntelliJ IDEA or Android Studio
3. Run the application on an Android device or emulator

## Development Workflow

### Environment Setup

1. Clone the repository
2. Set up Google Cloud credentials as described above
3. Import the project into your IDE

### Building the Project

- Use Gradle to build the project: `./gradlew build`
- For Android: `./gradlew :composeApp:assembleDebug`
- For Desktop: `./gradlew :composeApp:run`

### Hot Reload

The project supports Compose Hot Reload for faster development:
- Make changes to your Compose UI code
- The changes will be applied without restarting the application

## Architecture

The application follows a clean architecture approach:

- **UI Layer**: Compose UI components in the `ui` package
- **State Management**: State classes in the `states` package
- **Repositories**: Data access in the `repositories` package
- **Database**: SQLDelight for local storage
- **Backend**: Google Sheets integration in the `backend` package

## Testing

- Run unit tests: `./gradlew test`
- Run Android instrumentation tests: `./gradlew connectedAndroidTest`

## Troubleshooting

### Authentication Issues

- Verify that your credentials.json file is correctly placed
- Ensure that the Google Sheets API is enabled in your Google Cloud project
- Check that your OAuth 2.0 Client ID is configured correctly

### Build Problems

- Clean and rebuild the project: `./gradlew clean build`
- Verify Gradle version compatibility
- Check for dependency conflicts in build.gradle.kts files

### Sync Issues

- Verify internet connectivity
- Ensure you have the correct permissions for the Google Sheets document
- Check the application logs for specific error messages

## Contributing

### Code Style

- Follow Kotlin coding conventions
- Use meaningful names for functions and variables
- Write documentation for public APIs

### Pull Request Process

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Resources

- [Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Google Sheets API](https://developers.google.com/sheets/api)
- [Compose Hot Reload](https://github.com/JetBrains/compose-hot-reload)