package de.rakhman.cooking.repositories

import de.rakhman.cooking.Database
import de.rakhman.cooking.Recipe

class DatabaseRepository(
    private val database: Database
) {
    fun getSpreadSheetIdOrNull(): String? {
        return database.settingsQueries.selectFirst().executeAsOneOrNull()
    }

    fun getRecipes(): List<Recipe> {
        return database.recipesQueries.selectAll().executeAsList()
    }

    fun getPlan(): List<Long> {
        return database.planQueries.selectAll().executeAsList()
    }

    fun getShop(): List<Long> {
        return database.shopQueries.selectAll().executeAsList()
    }

    fun updateSpreadsheetId(id: String?) {
        database.transaction {
            database.settingsQueries.deleteAll()
            id?.let { database.settingsQueries.insert(it) }
            database.recipesQueries.deleteAll()
            database.planQueries.deleteAll()
            database.shopQueries.deleteAll()
        }
    }

    fun updateWith(recipes: List<Recipe>, plan: List<Long>, shop: List<Long>) {
        database.transaction {
            database.recipesQueries.deleteAll()
            recipes.forEach {
                database.recipesQueries.insert(it.id, it.title, it.url, it.counter, it.tags)
            }

            database.planQueries.deleteAll()
            plan.forEach {
                database.planQueries.insert(it)
            }

            database.shopQueries.deleteAll()
            shop.forEach {
                database.shopQueries.insert(it)
            }
        }
    }
}