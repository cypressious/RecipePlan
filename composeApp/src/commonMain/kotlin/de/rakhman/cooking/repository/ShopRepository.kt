package de.rakhman.cooking.repository

/**
 * Repository interface for managing the shopping list.
 */
interface ShopRepository {
    /**
     * Get all recipe IDs in the shopping list.
     *
     * @return List of recipe IDs
     */
    suspend fun getShopRecipeIds(): List<Long>

    /**
     * Add a recipe to the shopping list.
     *
     * @param recipeId The ID of the recipe to add
     */
    suspend fun addToShop(recipeId: Long)

    /**
     * Remove a recipe from the shopping list.
     *
     * @param recipeId The ID of the recipe to remove
     */
    suspend fun removeFromShop(recipeId: Long)

    /**
     * Synchronize shopping list data between remote and local data sources.
     */
    suspend fun syncShop()
}