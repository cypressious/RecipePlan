package de.rakhman.cooking.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaPixmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.States
import kotlinx.coroutines.runBlocking
import org.junit.Before
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

/**
 * Utility class for creating screenshot tests.
 */
abstract class ScreenshotTestUtil {

    @Before
    fun setUp() {
        // Initialize the states
        States.reset()
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

    /**
     * Take a screenshot of the given composable.
     * @param name The name of the screenshot.
     * @param content The composable to screenshot.
     */
    @OptIn(ExperimentalTestApi::class)
    protected fun takeScreenshot(name: String, content: @Composable () -> Unit) {
        runComposeUiTest {
            setContent {
                content()
            }

            // Create the screenshots directory if it doesn't exist
            val screenshotsDir = File("build/screenshots")
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs()
            }

            // Take the screenshot
            val bitmap = captureToImage()
            
            // Save the screenshot
            val file = File(screenshotsDir, "$name.png")
            saveBitmap(bitmap, file)
        }
    }

    /**
     * Save the bitmap to a file.
     * @param bitmap The bitmap to save.
     * @param file The file to save to.
     */
    private fun saveBitmap(bitmap: ImageBitmap, file: File) {
        val pixmap = bitmap.asSkiaPixmap()
        val data = pixmap.encodeToData()
        Files.write(Paths.get(file.toURI()), data!!.bytes)
    }
}