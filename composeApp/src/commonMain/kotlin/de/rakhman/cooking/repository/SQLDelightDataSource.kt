package de.rakhman.cooking.repository

import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of DataSource for SQLDelight database.
 */
class SQLDelightDataSource(private val database: Database) : DataSource {

    override suspend fun getRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        database.recipesQueries.selectAll().executeAsList()
    }

    override suspend fun addRecipe(title: String, url: String?): Long = withContext(Dispatchers.IO) {
        // Get the highest ID from the database and increment it
        val maxId = database.recipesQueries.selectAll().executeAsList().maxOfOrNull { it.id } ?: 0
        val newId = maxId + 1

        database.recipesQueries.insert(newId, title, url, 0)
        newId
    }

    override suspend fun updateRecipe(id: Long, title: String, url: String?) = withContext(Dispatchers.IO) {
        // SQLDelight doesn't have a direct update method, so we need to delete and insert
        val recipe = database.recipesQueries.selectAll().executeAsList().find { it.id == id }
        if (recipe != null) {
            val counter = recipe.counter
            database.transaction {
                // Delete all recipes and reinsert them except the one we're updating
                val allRecipes = database.recipesQueries.selectAll().executeAsList()
                database.recipesQueries.deleteAll()
                allRecipes.forEach { 
                    if (it.id == id) {
                        database.recipesQueries.insert(id, title, url, counter)
                    } else {
                        database.recipesQueries.insert(it.id, it.title, it.url, it.counter)
                    }
                }
            }
        }
    }

    override suspend fun deleteRecipe(id: Long) = withContext(Dispatchers.IO) {
        database.transaction {
            // Delete all recipes and reinsert them except the one we're deleting
            val allRecipes = database.recipesQueries.selectAll().executeAsList().filter { it.id != id }
            database.recipesQueries.deleteAll()
            allRecipes.forEach { 
                database.recipesQueries.insert(it.id, it.title, it.url, it.counter)
            }

            // Delete all plan entries and reinsert them except those with the recipe ID we're deleting
            val allPlanEntries = database.planQueries.selectAll().executeAsList().filter { it != id }
            database.planQueries.deleteAll()
            allPlanEntries.forEach { database.planQueries.insert(it) }

            // Delete all shop entries and reinsert them except those with the recipe ID we're deleting
            val allShopEntries = database.shopQueries.selectAll().executeAsList().filter { it != id }
            database.shopQueries.deleteAll()
            allShopEntries.forEach { database.shopQueries.insert(it) }
        }
    }

    override suspend fun incrementRecipeCounter(id: Long) = withContext(Dispatchers.IO) {
        val recipe = database.recipesQueries.selectAll().executeAsList().find { it.id == id }
        if (recipe != null) {
            val newCounter = recipe.counter + 1
            database.transaction {
                // Delete all recipes and reinsert them except the one we're updating
                val allRecipes = database.recipesQueries.selectAll().executeAsList()
                database.recipesQueries.deleteAll()
                allRecipes.forEach { 
                    if (it.id == id) {
                        database.recipesQueries.insert(id, recipe.title, recipe.url, newCounter)
                    } else {
                        database.recipesQueries.insert(it.id, it.title, it.url, it.counter)
                    }
                }
            }
        }
    }

    override suspend fun getPlanRecipeIds(): List<Long> = withContext(Dispatchers.IO) {
        database.planQueries.selectAll().executeAsList()
    }

    override suspend fun addToPlan(recipeId: Long) = withContext(Dispatchers.IO) {
        database.planQueries.insert(recipeId)
    }

    override suspend fun removeFromPlan(recipeId: Long) = withContext(Dispatchers.IO) {
        // Delete all plan entries and reinsert them except the one we're removing
        val allPlanEntries = database.planQueries.selectAll().executeAsList().filter { it != recipeId }
        database.planQueries.deleteAll()
        allPlanEntries.forEach { database.planQueries.insert(it) }
    }

    override suspend fun getShopRecipeIds(): List<Long> = withContext(Dispatchers.IO) {
        database.shopQueries.selectAll().executeAsList()
    }

    override suspend fun addToShop(recipeId: Long) = withContext(Dispatchers.IO) {
        database.shopQueries.insert(recipeId)
    }

    override suspend fun removeFromShop(recipeId: Long) = withContext(Dispatchers.IO) {
        // Delete all shop entries and reinsert them except the one we're removing
        val allShopEntries = database.shopQueries.selectAll().executeAsList().filter { it != recipeId }
        database.shopQueries.deleteAll()
        allShopEntries.forEach { database.shopQueries.insert(it) }
    }

    override suspend fun updatePlanAndShop(plan: List<Long>, shop: List<Long>) = withContext(Dispatchers.IO) {
        database.transaction {
            // Update plan
            database.planQueries.deleteAll()
            plan.forEach { database.planQueries.insert(it) }

            // Update shop
            database.shopQueries.deleteAll()
            shop.forEach { database.shopQueries.insert(it) }
        }
    }
}
