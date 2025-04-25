package de.rakhman.cooking.repository

import de.rakhman.cooking.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of RecipeRepository that uses both local and remote data sources.
 */
class RecipeRepositoryImpl(
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource
) : RecipeRepository {
    
    override suspend fun getAllRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        // Prioritize local data source for reading
        localDataSource.getRecipes()
    }
    
    override suspend fun addRecipe(title: String, url: String?): Long = withContext(Dispatchers.IO) {
        // Add to remote data source first
        val id = remoteDataSource.addRecipe(title, url)
        
        // Then sync with local data source
        syncRecipes()
        
        id
    }
    
    override suspend fun updateRecipe(id: Long, title: String, url: String?) = withContext(Dispatchers.IO) {
        // Update remote data source first
        remoteDataSource.updateRecipe(id, title, url)
        
        // Then sync with local data source
        syncRecipes()
    }
    
    override suspend fun deleteRecipe(id: Long) = withContext(Dispatchers.IO) {
        // Delete from remote data source first
        remoteDataSource.deleteRecipe(id)
        
        // Then sync with local data source
        syncRecipes()
    }
    
    override suspend fun incrementCounter(id: Long) = withContext(Dispatchers.IO) {
        // Increment counter in remote data source first
        remoteDataSource.incrementRecipeCounter(id)
        
        // Then sync with local data source
        syncRecipes()
    }
    
    override suspend fun syncRecipes() = withContext(Dispatchers.IO) {
        // Get recipes from remote data source
        val recipes = remoteDataSource.getRecipes()
        
        // Update local data source with recipes from remote
        val localRecipes = localDataSource.getRecipes()
        
        // Delete recipes that are in local but not in remote
        val localIds = localRecipes.map { it.id }.toSet()
        val remoteIds = recipes.map { it.id }.toSet()
        
        val idsToDelete = localIds - remoteIds
        idsToDelete.forEach { localDataSource.deleteRecipe(it) }
        
        // Update or add recipes from remote
        recipes.forEach { recipe ->
            val localRecipe = localRecipes.find { it.id == recipe.id }
            if (localRecipe != null) {
                // Update if different
                if (localRecipe.title != recipe.title || localRecipe.url != recipe.url || localRecipe.counter != recipe.counter) {
                    localDataSource.updateRecipe(recipe.id, recipe.title, recipe.url)
                }
            } else {
                // Add if not exists
                localDataSource.addRecipe(recipe.title, recipe.url)
            }
        }
    }
}