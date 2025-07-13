package de.rakhman.cooking.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.States
import io.sellmair.evas.set
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import kotlin.test.BeforeTest

/**
 * Base class for screenshot tests.
 * Provides common functionality for all screenshot tests.
 */
abstract class BaseScreenshotTest {
    
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        showSystemUi = false
    )
    
    /**
     * Set up the test environment before each test.
     * This method is called before each test method.
     */
    @BeforeTest
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
}