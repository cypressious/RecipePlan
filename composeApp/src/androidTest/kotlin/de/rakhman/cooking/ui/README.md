# Screenshot Tests for RecipePlan

This directory contains screenshot tests for the RecipePlan app. These tests capture screenshots of the app's UI in various states without requiring authentication or internet access.

## Test Structure

The tests are organized as follows:

- `BaseScreenshotTest.kt`: Base class for all screenshot tests, providing common functionality.
- `PlanScreenTest.kt`: Tests for the Plan and Shop screens.
- `RecipesScreenTest.kt`: Tests for the Recipes screen.
- `AddScreenTest.kt`: Tests for the Add/Edit recipe screen.

## Test Cases

### Plan Screen Tests

- `testPlanScreen()`: Tests the Plan screen with sample recipes.
- `testShopScreen()`: Tests the Shop screen with sample recipes.

### Recipes Screen Tests

- `testRecipesScreen()`: Tests the Recipes screen with sample recipes.
- `testRecipesScreenWithFilter()`: Tests the Recipes screen with a filter applied.

### Add Screen Tests

- `testAddScreen()`: Tests the Add screen for adding a new recipe.
- `testEditScreen()`: Tests the Add screen for editing an existing recipe.
- `testAddScreenWithInitialData()`: Tests the Add screen with initial data from a URL.

## Running the Tests

These tests can be run using Android Studio or via Gradle:

```bash
./gradlew connectedAndroidTest
```

## Screenshots

Screenshots are saved to the device's internal storage in the `screenshots` directory. The path is logged to the console when the tests are run.

## Implementation Details

- The tests set the states manually using `setScreenState()` and `setRecipesState()` methods.
- Sample data is created using the `createSampleRecipes()` method.
- No authentication or internet access is required to run the tests.
- The tests use the Compose UI testing framework to capture screenshots.