package de.rakhman.cooking.screenshots

import androidx.compose.ui.test.*
import de.rakhman.cooking.events.Events
import de.rakhman.cooking.states.*
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import kotlin.test.Test

/**
 * Base class for screenshot tests of the RecipePlan app.
 * Tests are designed to run without authentication or internet access.
 */
abstract class ScreenshotTestsBase {
    
    /**
     * Creates a mock RecipesState.Success with sample data for testing
     */
    protected fun createSampleRecipesState(): RecipesState.Success {
        val sampleRecipes = listOf(
            RecipeDto.create(
                id = 1L,
                title = "Spaghetti Carbonara",
                url = "https://example.com/carbonara",
                counter = 5L,
                tags = setOf("pasta", "italian", "quick")
            ),
            RecipeDto.create(
                id = 2L,
                title = "Chicken Tikka Masala",
                url = "https://example.com/tikka",
                counter = 3L,
                tags = setOf("indian", "chicken", "spicy")
            ),
            RecipeDto.create(
                id = 3L,
                title = "Vegetable Stir Fry",
                url = null,
                counter = 8L,
                tags = setOf("vegetarian", "healthy", "asian")
            )
        )
        
        val planRecipeIds = listOf(1L, 2L)
        val shopRecipeIds = listOf(1L)
        
        return RecipesState.Success(
            recipes = sampleRecipes,
            plan = planRecipeIds,
            shop = shopRecipeIds
        )
    }
    
    /**
     * Creates a mock state setup for testing without external dependencies
     */
    protected fun createTestStates(
        recipesState: RecipesState = createSampleRecipesState(),
        screenState: ScreenState = ScreenState.Plan
    ): States {
        val states = States()
        states[RecipesState] = recipesState
        states[ScreenState] = screenState
        return states
    }
    
    @Test
    fun testBasicSetup() {
        // Simple test to verify test infrastructure is working
        println("Basic screenshot test infrastructure is working")
    }
}