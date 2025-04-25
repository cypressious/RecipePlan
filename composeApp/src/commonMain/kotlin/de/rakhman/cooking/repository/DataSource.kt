package de.rakhman.cooking.repository

import de.rakhman.cooking.Recipe

/**
 * Interface for data sources (Google Sheets, SQLDelight).
 */
interface DataSource {
    /**
     * Get all recipes from the data source.
     */
    suspend fun getRecipes(): List<Recipe>
    
    /**
     * Add a recipe to the data source.
     */
    suspend fun addRecipe(title: String, url: String?): Long
    
    /**
     * Update a recipe in the data source.
     */
    suspend fun updateRecipe(id: Long, title: String, url: String?)
    
    /**
     * Delete a recipe from the data source.
     */
    suspend fun deleteRecipe(id: Long)
    
    /**
     * Increment the counter for a recipe.
     */
    suspend fun incrementRecipeCounter(id: Long)
    
    /**
     * Get all recipe IDs in the plan.
     */
    suspend fun getPlanRecipeIds(): List<Long>
    
    /**
     * Add a recipe to the plan.
     */
    suspend fun addToPlan(recipeId: Long)
    
    /**
     * Remove a recipe from the plan.
     */
    suspend fun removeFromPlan(recipeId: Long)
    
    /**
     * Get all recipe IDs in the shopping list.
     */
    suspend fun getShopRecipeIds(): List<Long>
    
    /**
     * Add a recipe to the shopping list.
     */
    suspend fun addToShop(recipeId: Long)
    
    /**
     * Remove a recipe from the shopping list.
     */
    suspend fun removeFromShop(recipeId: Long)
    
    /**
     * Update plan and shop lists.
     */
    suspend fun updatePlanAndShop(plan: List<Long>, shop: List<Long>)
}