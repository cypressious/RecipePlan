package de.rakhman.cooking.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import de.rakhman.cooking.getColorScheme
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.States
import io.sellmair.evas.set
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import java.io.File
import java.io.FileOutputStream

/**
 * Base class for screenshot tests.
 * Provides common functionality for all screenshot tests.
 */
abstract class BaseScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Set up the test environment before each test.
     */
    @Before
    fun setUp() {
        // Reset the states
        runBlocking {
            States.reset()
        }
    }

    /**
     * Set the screen state for testing.
     * @param screenState The screen state to set.
     */
    protected fun setScreenState(screenState: ScreenState) {
        runBlocking {
            ScreenState.set(screenState)
        }
    }

    /**
     * Set the recipes state for testing.
     * @param recipes List of recipes to set.
     * @param plan List of recipe IDs in the plan.
     * @param shop List of recipe IDs in the shop.
     */
    protected fun setRecipesState(
        recipes: List<RecipeDto>,
        plan: List<Long> = emptyList(),
        shop: List<Long> = emptyList()
    ) {
        runBlocking {
            RecipesState.set(RecipesState.Success(recipes, plan, shop))
        }
    }

    /**
     * Set up the UI content for testing.
     * @param content The composable content to render.
     */
    protected fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent {
            MaterialTheme(getColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }

    /**
     * Take a screenshot of the current screen.
     * @param fileName The name of the screenshot file.
     */
    protected fun takeScreenshot(fileName: String) {
        // Get the bitmap from the compose test rule
        val bitmap = composeTestRule.onRoot().captureToImage()
        
        // Get the context
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create the screenshots directory if it doesn't exist
        val screenshotsDir = File(context.filesDir, "screenshots")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        
        // Save the bitmap to a file
        val file = File(screenshotsDir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        
        // Log the screenshot path
        println("Screenshot saved to: ${file.absolutePath}")
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
    protected fun createRecipe(
        id: Long,
        title: String,
        url: String? = null,
        counter: Long = 0,
        tags: Set<String> = emptySet()
    ): RecipeDto {
        return RecipeDto.create(id, title, url, counter, tags)
    }

    /**
     * Create a list of sample recipes for testing.
     * @return A list of RecipeDto instances.
     */
    protected fun createSampleRecipes(): List<RecipeDto> {
        return listOf(
            createRecipe(1, "Pasta Carbonara", counter = 5, tags = setOf("Italian", "Pasta")),
            createRecipe(2, "Chicken Curry", counter = 3, tags = setOf("Indian", "Spicy")),
            createRecipe(3, "Caesar Salad", counter = 2, tags = setOf("Salad", "Healthy")),
            createRecipe(4, "Beef Stir Fry", counter = 1, tags = setOf("Asian", "Quick")),
            createRecipe(5, "Vegetable Soup", counter = 4, tags = setOf("Soup", "Vegetarian"))
        )
    }
}