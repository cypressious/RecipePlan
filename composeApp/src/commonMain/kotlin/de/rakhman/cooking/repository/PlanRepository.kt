package de.rakhman.cooking.repository

/**
 * Repository interface for managing the meal plan.
 */
interface PlanRepository {
    /**
     * Get all recipe IDs in the plan.
     *
     * @return List of recipe IDs
     */
    suspend fun getPlanRecipeIds(): List<Long>

    /**
     * Add a recipe to the plan.
     *
     * @param recipeId The ID of the recipe to add
     */
    suspend fun addToPlan(recipeId: Long)

    /**
     * Remove a recipe from the plan.
     *
     * @param recipeId The ID of the recipe to remove
     */
    suspend fun removeFromPlan(recipeId: Long)

    /**
     * Synchronize plan data between remote and local data sources.
     */
    suspend fun syncPlan()
}