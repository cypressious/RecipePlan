package de.rakhman.cooking.screenshots

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.rakhman.cooking.events.Events
import de.rakhman.cooking.states.*
import de.rakhman.cooking.ui.*
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Screenshot testing application for RecipePlan UI screens.
 * This application sets up UI states manually and can take screenshots of different screens.
 * 
 * Run this application to generate screenshots for:
 * - Plan screen (with recipes)
 * - Plan screen (empty)
 * - Shop screen (with recipes) 
 * - Shop screen (empty)
 * - Recipes screen (with recipes)
 * - Recipes screen (empty)
 * - Add screen (new recipe)
 * - Add screen (edit recipe)
 */

/**
 * Creates sample recipe data for testing
 */
fun createSampleRecipes(): List<RecipeDto> = listOf(
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
    ),
    RecipeDto.create(
        id = 4L,
        title = "Beef Tacos",
        url = "https://example.com/tacos", 
        counter = 2L,
        tags = setOf("mexican", "beef", "dinner")
    ),
    RecipeDto.create(
        id = 5L,
        title = "Greek Salad",
        url = null,
        counter = 10L,
        tags = setOf("salad", "greek", "healthy", "vegetarian")
    )
)

/**
 * Creates different test scenarios for screenshot testing
 */
object TestScenarios {
    
    fun recipesWithData(): RecipesState.Success {
        val recipes = createSampleRecipes()
        return RecipesState.Success(
            recipes = recipes,
            plan = listOf(1L, 2L, 3L), // Plan has 3 recipes
            shop = listOf(1L, 4L) // Shop has 2 recipes
        )
    }
    
    fun recipesEmpty(): RecipesState.Success {
        return RecipesState.Success(
            recipes = emptyList(),
            plan = emptyList(),
            shop = emptyList()
        )
    }
    
    fun recipesWithEmptyPlan(): RecipesState.Success {
        val recipes = createSampleRecipes()
        return RecipesState.Success(
            recipes = recipes,
            plan = emptyList(), // Empty plan
            shop = emptyList() // Empty shop
        )
    }
    
    fun createStates(
        recipesState: RecipesState = recipesWithData(),
        screenState: ScreenState = ScreenState.Plan
    ): States {
        val states = States()
        states[RecipesState] = recipesState
        states[ScreenState] = screenState
        return states
    }
}

/**
 * Test screen configurations for screenshots
 */
sealed class ScreenshotTest(
    val name: String,
    val description: String,
    val screenState: ScreenState,
    val recipesState: RecipesState
) {
    object PlanWithRecipes : ScreenshotTest(
        "plan_with_recipes",
        "Plan screen showing planned recipes",
        ScreenState.Plan,
        TestScenarios.recipesWithData()
    )
    
    object PlanEmpty : ScreenshotTest(
        "plan_empty", 
        "Plan screen with no planned recipes",
        ScreenState.Plan,
        TestScenarios.recipesWithEmptyPlan()
    )
    
    object ShopWithRecipes : ScreenshotTest(
        "shop_with_recipes",
        "Shop screen showing shopping list",
        ScreenState.Shop,
        TestScenarios.recipesWithData()
    )
    
    object ShopEmpty : ScreenshotTest(
        "shop_empty",
        "Shop screen with empty shopping list", 
        ScreenState.Shop,
        TestScenarios.recipesWithEmptyPlan()
    )
    
    object RecipesWithData : ScreenshotTest(
        "recipes_with_data",
        "Recipes screen showing all recipes",
        ScreenState.Recipes,
        TestScenarios.recipesWithData()
    )
    
    object RecipesEmpty : ScreenshotTest(
        "recipes_empty",
        "Recipes screen with no recipes",
        ScreenState.Recipes,
        TestScenarios.recipesEmpty()
    )
    
    object AddNewRecipe : ScreenshotTest(
        "add_new_recipe",
        "Add screen for creating new recipe",
        ScreenState.Add(ScreenState.Recipes),
        TestScenarios.recipesWithData()
    )
    
    object EditRecipe : ScreenshotTest(
        "edit_recipe", 
        "Add screen for editing existing recipe",
        ScreenState.Add(ScreenState.Recipes, TestScenarios.recipesWithData().recipes.first()),
        TestScenarios.recipesWithData()
    )
}

@Composable
fun ScreenshotTestApp() {
    var currentTest by remember { mutableStateOf<ScreenshotTest>(ScreenshotTest.PlanWithRecipes) }
    
    val allTests = listOf(
        ScreenshotTest.PlanWithRecipes,
        ScreenshotTest.PlanEmpty,
        ScreenshotTest.ShopWithRecipes,
        ScreenshotTest.ShopEmpty,
        ScreenshotTest.RecipesWithData,
        ScreenshotTest.RecipesEmpty,
        ScreenshotTest.AddNewRecipe,
        ScreenshotTest.EditRecipe
    )
    
    installEvas(Events(), TestScenarios.createStates(currentTest.recipesState, currentTest.screenState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Test controls at the top
            Row(modifier = Modifier.padding(16.dp)) {
                Text("Current Test: ${currentTest.name} - ${currentTest.description}")
                Spacer(modifier = Modifier.weight(1f))
                
                Button(onClick = {
                    val currentIndex = allTests.indexOf(currentTest)
                    val nextIndex = (currentIndex + 1) % allTests.size
                    currentTest = allTests[nextIndex]
                }) {
                    Text("Next Test")
                }
            }
            
            // Main UI content
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (currentTest.screenState) {
                    is ScreenState.Plan -> PlanScreen(Modifier.fillMaxSize(), isShop = false)
                    is ScreenState.Shop -> PlanScreen(Modifier.fillMaxSize(), isShop = true)
                    is ScreenState.Recipes -> RecipesScreen(Modifier.fillMaxSize())
                    is ScreenState.Add -> {
                        val addState = currentTest.screenState as ScreenState.Add
                        AddScreen(
                            Modifier.fillMaxSize(),
                            editingRecipe = addState.editingRecipe,
                            initialData = addState.initialData
                        )
                    }
                    else -> Text("Screen not implemented for screenshot testing")
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RecipePlan Screenshot Tests",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        ScreenshotTestApp()
    }
}

/**
 * Instructions for taking screenshots:
 * 
 * 1. Run this application using: ./gradlew :composeApp:runDistributable
 * 2. Use the "Next Test" button to cycle through different test scenarios
 * 3. Take screenshots of each scenario manually or programmatically
 * 4. Screenshots should be saved to the screenshots/ directory
 * 
 * Test scenarios covered:
 * - plan_with_recipes: Plan screen with 3 planned recipes
 * - plan_empty: Plan screen with no planned recipes  
 * - shop_with_recipes: Shop screen with 2 recipes in shopping list
 * - shop_empty: Shop screen with empty shopping list
 * - recipes_with_data: Recipes screen with 5 sample recipes
 * - recipes_empty: Recipes screen with no recipes
 * - add_new_recipe: Add screen for creating a new recipe
 * - edit_recipe: Add screen for editing an existing recipe
 * 
 * All tests run with mock data and do not require authentication or internet access.
 */