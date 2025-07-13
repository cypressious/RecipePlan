package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.rakhman.cooking.getColorScheme
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.States
import io.sellmair.evas.set
import kotlinx.coroutines.runBlocking

/**
 * Main class for generating screenshots of the app's screens.
 * This approach doesn't rely on JUnit or other testing frameworks,
 * making it simpler to set up and run.
 */
fun main() = application {
    // Initialize the states
    States.reset()
    
    // Create sample data
    val recipes = createSampleRecipes()
    val planIds = listOf(1L, 3L, 5L)
    val shopIds = listOf(2L, 4L)
    
    // Set up the initial state
    runBlocking {
        RecipesState.set(RecipesState.Success(recipes, planIds, shopIds))
    }
    
    // Create a window for each screen
    createScreenshotWindow("Plan Screen", ScreenState.Plan) {
        PlanScreen(Modifier.padding(16.dp), false)
    }
    
    createScreenshotWindow("Shop Screen", ScreenState.Shop) {
        PlanScreen(Modifier.padding(16.dp), true)
    }
    
    createScreenshotWindow("Recipes Screen", ScreenState.Recipes) {
        RecipesScreen(Modifier.padding(16.dp))
    }
    
    createScreenshotWindow("Add Screen", ScreenState.Add(ScreenState.Recipes)) {
        AddScreen(Modifier.padding(16.dp), null, null)
    }
}

/**
 * Create a window for a specific screen.
 * @param title The window title.
 * @param screenState The screen state to set.
 * @param content The composable content to render.
 */
@Composable
private fun ApplicationScope.createScreenshotWindow(
    title: String,
    screenState: ScreenState,
    content: @Composable () -> Unit
) {
    // Set the screen state
    runBlocking {
        ScreenState.set(screenState)
    }
    
    // Create the window
    Window(
        onCloseRequest = ::exitApplication,
        title = title,
        state = rememberWindowState(width = 400.dp, height = 800.dp)
    ) {
        MaterialTheme(getColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * Create a list of sample recipes for testing.
 * @return A list of RecipeDto instances.
 */
private fun createSampleRecipes(): List<RecipeDto> {
    return listOf(
        createRecipe(1, "Pasta Carbonara", counter = 5, tags = setOf("Italian", "Pasta")),
        createRecipe(2, "Chicken Curry", counter = 3, tags = setOf("Indian", "Spicy")),
        createRecipe(3, "Caesar Salad", counter = 2, tags = setOf("Salad", "Healthy")),
        createRecipe(4, "Beef Stir Fry", counter = 1, tags = setOf("Asian", "Quick")),
        createRecipe(5, "Vegetable Soup", counter = 4, tags = setOf("Soup", "Vegetarian"))
    )
}

/**
 * Create a sample recipe for testing.
 * @param id The recipe ID.
 * @param title The recipe title.
 * @param url The recipe URL (optional).
 * @param counter The recipe counter.
 * @param tags The recipe tags.
 * @return A RecipeDto instance.
 */
private fun createRecipe(
    id: Long,
    title: String,
    url: String? = null,
    counter: Long = 0,
    tags: Set<String> = emptySet()
): RecipeDto {
    return RecipeDto.create(id, title, url, counter, tags)
}