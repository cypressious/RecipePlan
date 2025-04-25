package de.rakhman.cooking.repository

import com.google.api.services.sheets.v4.Sheets
import de.rakhman.cooking.Database
import de.rakhman.cooking.PlatformContext

/**
 * Factory for creating repositories.
 */
class RepositoryFactory(
    database: Database,
    sheets: Sheets,
    spreadsheetId: String,
    platformContext: PlatformContext
) {
    private val localDataSource: DataSource = SQLDelightDataSource(database)
    private val remoteDataSource: DataSource = GoogleSheetsDataSource(sheets, spreadsheetId)
    
    /**
     * Get the recipe repository.
     */
    val recipeRepository: RecipeRepository = RecipeRepositoryImpl(localDataSource, remoteDataSource)
    
    /**
     * Get the plan repository.
     */
    val planRepository: PlanRepository = PlanRepositoryImpl(localDataSource, remoteDataSource)
    
    /**
     * Get the shop repository.
     */
    val shopRepository: ShopRepository = ShopRepositoryImpl(localDataSource, remoteDataSource)
    
    /**
     * Synchronize all data between remote and local data sources.
     */
    suspend fun syncAll() {
        recipeRepository.syncRecipes()
        planRepository.syncPlan()
        shopRepository.syncShop()
    }
}