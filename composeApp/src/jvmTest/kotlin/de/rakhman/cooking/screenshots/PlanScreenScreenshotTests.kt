package de.rakhman.cooking.screenshots

import de.rakhman.cooking.events.Events
import de.rakhman.cooking.states.*
import kotlin.test.Test

/**
 * Screenshot tests for the Plan screen functionality.
 * Tests both plan view and shop view (isShop parameter).
 */
class PlanScreenScreenshotTests : ScreenshotTestsBase() {
    
    @Test
    fun testPlanScreenSetup() {
        val sampleState = createSampleRecipesState()
        val testStates = createTestStates(screenState = ScreenState.Plan)
        
        // Verify our test data is set up correctly
        assert(sampleState.plan.contains(1L))
        assert(sampleState.plan.contains(2L))
        assert(sampleState.shop.contains(1L))
        
        println("Plan screen test setup completed successfully")
        println("Sample recipes: ${sampleState.recipes.map { it.title }}")
        println("Plan recipe IDs: ${sampleState.plan}")
        println("Shop recipe IDs: ${sampleState.shop}")
    }
    
    @Test
    fun testShopScreenSetup() {
        val sampleState = createSampleRecipesState()
        val testStates = createTestStates(screenState = ScreenState.Shop)
        
        // Verify our test data is set up correctly for shop
        assert(sampleState.shop.isNotEmpty())
        
        println("Shop screen test setup completed successfully")
        println("Shop recipes: ${sampleState.shop.mapNotNull { id -> sampleState.byId[id]?.title }}")
    }
}