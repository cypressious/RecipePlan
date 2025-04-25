package de.rakhman.cooking.repository

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import de.rakhman.cooking.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of DataSource for Google Sheets.
 */
class GoogleSheetsDataSource(
    private val sheets: Sheets,
    private val spreadsheetId: String
) : DataSource {

    private val SHEET_NAME_RECIPES = "Rezepte"
    private val SHEET_NAME_PLAN = "Plan"
    private val RANGE_PLAN_AND_SHOP = "$SHEET_NAME_PLAN!A1:A2"
    private val DELETED_VALUE = "deleted"

    private val COLUMN_NAME = 0
    private val COLUMN_URL = 1
    private val COLUMN_DELETED = 2
    private val COLUMN_COUNTER = 3

    override suspend fun getRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        readRecipes()
    }

    override suspend fun addRecipe(title: String, url: String?): Long = withContext(Dispatchers.IO) {
        val recipes = readSheetRange(SHEET_NAME_RECIPES)
        val id = recipes.size + 1L

        sheets.spreadsheets().values().update(
            spreadsheetId,
            "$SHEET_NAME_RECIPES!A$id:B$id",
            ValueRange().apply {
                setValues(listOf(listOf(title, url ?: "")))
            }
        ).run {
            valueInputOption = "RAW"
            execute()
        }

        id
    }

    override suspend fun updateRecipe(id: Long, title: String, url: String?): Unit = withContext(Dispatchers.IO) {
        sheets.spreadsheets().values().update(
            spreadsheetId,
            "$SHEET_NAME_RECIPES!A$id:B$id",
            ValueRange().apply {
                setValues(listOf(listOf(title, url ?: "")))
            }
        ).run {
            valueInputOption = "RAW"
            execute()
        }
    }

    override suspend fun deleteRecipe(id: Long) = withContext(Dispatchers.IO) {
        updateRawValue("$SHEET_NAME_RECIPES!C$id", listOf(listOf(DELETED_VALUE)))
    }

    override suspend fun incrementRecipeCounter(id: Long) = withContext(Dispatchers.IO) {
        val rangeRef = "$SHEET_NAME_RECIPES!D$id"
        val range = readSheetRange(rangeRef)
        val oldCounter = range.elementAtOrNull(0)?.elementAtOrNull(0)?.toString()?.toLongOrNull()
        val newCounter = ((oldCounter ?: 0) + 1).toString()
        updateRawValue(rangeRef, listOf(listOf(newCounter)))
    }

    override suspend fun getPlanRecipeIds(): List<Long> = withContext(Dispatchers.IO) {
        val (plan, _) = readPlanAndShop()
        plan
    }

    override suspend fun getShopRecipeIds(): List<Long> = withContext(Dispatchers.IO) {
        val (_, shop) = readPlanAndShop()
        shop
    }

    override suspend fun addToPlan(recipeId: Long) = withContext(Dispatchers.IO) {
        val (plan, shop) = readPlanAndShop()
        val newPlan = plan + recipeId
        updatePlanAndShop(newPlan, shop)
    }

    override suspend fun removeFromPlan(recipeId: Long) = withContext(Dispatchers.IO) {
        val (plan, shop) = readPlanAndShop()
        val newPlan = plan.filter { it != recipeId }
        updatePlanAndShop(newPlan, shop)
    }

    override suspend fun addToShop(recipeId: Long) = withContext(Dispatchers.IO) {
        val (plan, shop) = readPlanAndShop()
        val newShop = shop + recipeId
        updatePlanAndShop(plan, newShop)
    }

    override suspend fun removeFromShop(recipeId: Long) = withContext(Dispatchers.IO) {
        val (plan, shop) = readPlanAndShop()
        val newShop = shop.filter { it != recipeId }
        updatePlanAndShop(plan, newShop)
    }

    override suspend fun updatePlanAndShop(plan: List<Long>, shop: List<Long>) = withContext(Dispatchers.IO) {
        updateRawValue(
            RANGE_PLAN_AND_SHOP,
            listOf(
                listOf(plan.joinToString(",")),
                listOf(shop.joinToString(","))
            )
        )
    }

    private fun readRecipes(): List<Recipe> {
        return readSheetRange(SHEET_NAME_RECIPES)
            .mapIndexedNotNull { i, row ->
                if (row.isNotEmpty() && row.elementAtOrNull(COLUMN_DELETED)?.toString() != DELETED_VALUE) {
                    Recipe(
                        id = i + 1L,
                        title = row[COLUMN_NAME].toString().trim(),
                        url = row.elementAtOrNull(COLUMN_URL)?.toString()?.ifBlank { null },
                        counter = row.elementAtOrNull(COLUMN_COUNTER).toString().toLongOrNull() ?: 0,
                    )
                } else {
                    null
                }
            }
    }

    private fun readPlanAndShop(): Pair<List<Long>, List<Long>> {
        val range = readSheetRange(RANGE_PLAN_AND_SHOP)
        return Pair(
            parsePlanCell(range.elementAtOrNull(0)?.elementAtOrNull(0)),
            parsePlanCell(range.elementAtOrNull(1)?.elementAtOrNull(0))
        )
    }

    private fun parsePlanCell(elementAtOrNull: Any?): List<Long> {
        return elementAtOrNull
            ?.toString()
            ?.split(",")
            ?.map { it.toLong() }
            .orEmpty()
    }

    private fun readSheetRange(range: String): List<List<Any?>> {
        return sheets.spreadsheets().values()
            .get(spreadsheetId, range)
            .execute()
            .getValues()
            ?: emptyList()
    }

    private fun updateRawValue(range: String, rawValues: List<List<String>>) {
        sheets.spreadsheets().values().update(
            spreadsheetId,
            range,
            ValueRange().apply { setValues(rawValues) }
        ).run {
            valueInputOption = "RAW"
            execute()
        }
    }
}
