package de.rakhman.cooking.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of PlanRepository that uses both local and remote data sources.
 */
class PlanRepositoryImpl(
    private val localDataSource: DataSource,
    private val remoteDataSource: DataSource
) : PlanRepository {
    
    override suspend fun getPlanRecipeIds(): List<Long> = withContext(Dispatchers.IO) {
        // Prioritize local data source for reading
        localDataSource.getPlanRecipeIds()
    }
    
    override suspend fun addToPlan(recipeId: Long) = withContext(Dispatchers.IO) {
        // Add to remote data source first
        remoteDataSource.addToPlan(recipeId)
        
        // Then sync with local data source
        syncPlan()
    }
    
    override suspend fun removeFromPlan(recipeId: Long) = withContext(Dispatchers.IO) {
        // Remove from remote data source first
        remoteDataSource.removeFromPlan(recipeId)
        
        // Then sync with local data source
        syncPlan()
    }
    
    override suspend fun syncPlan() = withContext(Dispatchers.IO) {
        // Get plan from remote data source
        val remotePlan = remoteDataSource.getPlanRecipeIds()
        
        // Update local data source with plan from remote
        val localPlan = localDataSource.getPlanRecipeIds()
        
        // If plans are different, update local plan
        if (localPlan != remotePlan) {
            // Clear local plan and add all remote plan entries
            localDataSource.updatePlanAndShop(remotePlan, localDataSource.getShopRecipeIds())
        }
    }
}