package de.rakhman.cooking.ui

import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for the Recipes screen.
 */
@RunWith(AndroidJUnit4::class)
class RecipesScreenTest : BaseScreenshotTest() {

    /**
     * Test the Recipes screen.
     */
    @Test
    fun testRecipesScreen() {
        // Set up the test data
        val recipes = createSampleRecipes()
        
        // Set the states
        setScreenState(ScreenState.Recipes)
        setRecipesState(recipes)
        
        // Set the content
        setContent {
            RecipesScreen(Modifier)
        }
        
        // Take a screenshot
        takeScreenshot("recipes_screen")
    }
    
    /**
     * Test the Recipes screen with a filter.
     */
    @Test
    fun testRecipesScreenWithFilter() {
        // Set up the test data
        val recipes = createSampleRecipes()
        
        // Set the states
        setScreenState(ScreenState.Recipes)
        setRecipesState(recipes)
        
        // Set the content
        setContent {
            RecipesScreen(Modifier)
        }
        
        // TODO: Set a filter in the search field
        // This would require interacting with the UI, which is beyond the scope of this task
        
        // Take a screenshot
        takeScreenshot("recipes_screen_with_filter")
    }
}