package de.rakhman.cooking.ui

import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for the Add screen.
 */
@RunWith(AndroidJUnit4::class)
class AddScreenTest : BaseScreenshotTest() {

    /**
     * Test the Add screen for adding a new recipe.
     */
    @Test
    fun testAddScreen() {
        // Set up the test data
        val recipes = createSampleRecipes()
        
        // Set the states
        setScreenState(ScreenState.Add(ScreenState.Recipes))
        setRecipesState(recipes)
        
        // Set the content
        setContent {
            AddScreen(Modifier, null, null)
        }
        
        // Take a screenshot
        takeScreenshot("add_screen")
    }
    
    /**
     * Test the Add screen for editing an existing recipe.
     */
    @Test
    fun testEditScreen() {
        // Set up the test data
        val recipes = createSampleRecipes()
        val recipeToEdit = recipes.first()
        
        // Set the states
        setScreenState(ScreenState.Add(ScreenState.Recipes, recipeToEdit))
        setRecipesState(recipes)
        
        // Set the content
        setContent {
            AddScreen(Modifier, recipeToEdit, null)
        }
        
        // Take a screenshot
        takeScreenshot("edit_screen")
    }
    
    /**
     * Test the Add screen with initial data from a URL.
     * Note: In a real test, this might trigger a network call to fetch the title from the URL.
     * For screenshot testing purposes, we're using a mock URL that won't actually be fetched.
     */
    @Test
    fun testAddScreenWithInitialData() {
        // Set up the test data
        val recipes = createSampleRecipes()
        // Using a mock URL format that won't trigger actual network calls
        val initialData = "mock://example.com/recipe"
        
        // Set the states
        setScreenState(ScreenState.Add(ScreenState.Recipes))
        setRecipesState(recipes)
        
        // Set the content
        setContent {
            AddScreen(Modifier, null, initialData)
        }
        
        // Take a screenshot
        takeScreenshot("add_screen_with_initial_data")
    }
}