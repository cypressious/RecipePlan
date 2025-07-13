package de.rakhman.cooking.ui

import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for the Plan and Shop screens.
 */
@RunWith(AndroidJUnit4::class)
class PlanScreenTest : BaseScreenshotTest() {

    /**
     * Test the Plan screen.
     */
    @Test
    fun testPlanScreen() {
        // Set up the test data
        val recipes = createSampleRecipes()
        val planIds = listOf(1L, 3L, 5L)
        val shopIds = listOf(2L, 4L)
        
        // Set the states
        setScreenState(ScreenState.Plan)
        setRecipesState(recipes, planIds, shopIds)
        
        // Set the content
        setContent {
            PlanScreen(Modifier, false)
        }
        
        // Take a screenshot
        takeScreenshot("plan_screen")
    }
    
    /**
     * Test the Shop screen.
     */
    @Test
    fun testShopScreen() {
        // Set up the test data
        val recipes = createSampleRecipes()
        val planIds = listOf(1L, 3L, 5L)
        val shopIds = listOf(2L, 4L)
        
        // Set the states
        setScreenState(ScreenState.Shop)
        setRecipesState(recipes, planIds, shopIds)
        
        // Set the content
        setContent {
            PlanScreen(Modifier, true)
        }
        
        // Take a screenshot
        takeScreenshot("shop_screen")
    }
}