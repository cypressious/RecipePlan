package de.rakhman.cooking.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of ShopRepository that uses both local and remote data sources.
 */
class ShopRepositoryImpl(
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource
) : ShopRepository {
    
    override suspend fun getShopRecipeIds(): List<Long> = withContext(Dispatchers.IO) {
        // Prioritize local data source for reading
        localDataSource.getShopRecipeIds()
    }
    
    override suspend fun addToShop(recipeId: Long) = withContext(Dispatchers.IO) {
        // Add to remote data source first
        remoteDataSource.addToShop(recipeId)
        
        // Then sync with local data source
        syncShop()
    }
    
    override suspend fun removeFromShop(recipeId: Long) = withContext(Dispatchers.IO) {
        // Remove from remote data source first
        remoteDataSource.removeFromShop(recipeId)
        
        // Then sync with local data source
        syncShop()
    }
    
    override suspend fun syncShop() = withContext(Dispatchers.IO) {
        // Get shop from remote data source
        val remoteShop = remoteDataSource.getShopRecipeIds()
        
        // Update local data source with shop from remote
        val localShop = localDataSource.getShopRecipeIds()
        
        // If shops are different, update local shop
        if (localShop != remoteShop) {
            // Clear local shop and add all remote shop entries
            localDataSource.updatePlanAndShop(localDataSource.getPlanRecipeIds(), remoteShop)
        }
    }
}