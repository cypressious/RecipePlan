package de.rakhman.cooking.repository

import de.rakhman.cooking.Recipe

/**
 * Repository interface for managing recipes.
 */
interface RecipeRepository {
    /**
     * Get all recipes.
     *
     * @return List of recipes
     */
    suspend fun getAllRecipes(): List<Recipe>

    /**
     * Add a new recipe.
     *
     * @param title The title of the recipe
     * @param url Optional URL for the recipe
     * @return The ID of the newly created recipe
     */
    suspend fun addRecipe(title: String, url: String?): Long

    /**
     * Update an existing recipe.
     *
     * @param id The ID of the recipe to update
     * @param title The new title for the recipe
     * @param url The new URL for the recipe
     */
    suspend fun updateRecipe(id: Long, title: String, url: String?)

    /**
     * Delete a recipe.
     *
     * @param id The ID of the recipe to delete
     */
    suspend fun deleteRecipe(id: Long)

    /**
     * Increment the counter for a recipe.
     *
     * @param id The ID of the recipe
     */
    suspend fun incrementCounter(id: Long)

    /**
     * Synchronize recipes between remote and local data sources.
     */
    suspend fun syncRecipes()
}