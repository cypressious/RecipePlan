package de.rakhman.cooking.repositories

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import de.rakhman.cooking.states.RecipeDto

const val SHEET_NAME_RECIPES = "Rezepte"
const val SHEET_NAME_PLAN = "Plan"
private const val RANGE_PLAN_AND_SHOP = "$SHEET_NAME_PLAN!A1:A2"
private const val DELETED_VALUE = "deleted"

private const val COLUMN_NAME = 0
private const val COLUMN_URL = 1
private const val COLUMN_DELETED = 2
private const val COLUMN_COUNTER = 3
private const val COLUMN_TAGS = 4

class SheetsRepository(
    private val sheets: Sheets,
    private val spreadSheetsId: String,
) {
    fun getRecipes(): List<RecipeDto> {
        fun parseRecipe(index: Int, row: List<Any?>): RecipeDto {
            return RecipeDto.create(
                id = index + 1L,
                title = row[COLUMN_NAME].toString().trim(),
                url = row.elementAtOrNull(COLUMN_URL)?.toString()?.ifBlank { null },
                counter = row.elementAtOrNull(COLUMN_COUNTER)?.toString()?.toLongOrNull() ?: 0,
                tagsString = row.elementAtOrNull(COLUMN_TAGS)?.toString(),
            )
        }

        return readSheetRange(SHEET_NAME_RECIPES)
            .mapIndexedNotNull { i, row ->
                if (row.isNotEmpty() && row.elementAtOrNull(COLUMN_DELETED)?.toString() != DELETED_VALUE) {
                    parseRecipe(i, row)
                } else {
                    null
                }
            }
    }

    fun getNewId(): Long {
        return readSheetRange(SHEET_NAME_RECIPES).size + 1L
    }

    fun getPlanAndShop(): Pair<List<Long>, List<Long>> {
        val range = readSheetRange(RANGE_PLAN_AND_SHOP)

        fun parsePlanCell(elementAtOrNull: Any?): List<Long> {
            return elementAtOrNull
                ?.toString()
                ?.split(",")
                ?.map { it.toLong() }
                .orEmpty()
        }

        return Pair(
            parsePlanCell(range.elementAtOrNull(0)?.elementAtOrNull(0)),
            parsePlanCell(range.elementAtOrNull(1)?.elementAtOrNull(0))
        )
    }

    fun updateRecipe(id: Long, title: String, url: String?, tags: Set<String>) {
        sheets.spreadsheets().values().batchUpdate(
            spreadSheetsId,
            BatchUpdateValuesRequest().apply {
                data = listOf(
                    ValueRange().apply {
                        range = "$SHEET_NAME_RECIPES!A$id:B$id"
                        setValues(listOf(listOf(title, url ?: "")))
                        valueInputOption = "RAW"
                    },
                    ValueRange().apply {
                        range = "$SHEET_NAME_RECIPES!E$id"
                        setValues(listOf(listOf(tags.joinToString(RecipeDto.SEPARATOR_TAGS))))
                        valueInputOption = "RAW"
                    },
                )
            }
        ).execute()
    }

    fun updatePlanAndShop(
        newPlan: List<Long>,
        newShop: List<Long>,
        idToIncrementCounter: Long?
    ) {
        sheets.spreadsheets().values().batchUpdate(
            spreadSheetsId,
            BatchUpdateValuesRequest().apply {
                data = buildList {
                    add(ValueRange().apply {
                        range = RANGE_PLAN_AND_SHOP
                        setValues(
                            listOf(
                                listOf(newPlan.joinToString(",")),
                                listOf(newShop.joinToString(",")),
                            )
                        )
                        valueInputOption = "RAW"
                    })

                    if (idToIncrementCounter != null) {
                        val rangeRef = "$SHEET_NAME_RECIPES!D${idToIncrementCounter}"
                        val oldCounter = readSheetRange(rangeRef)
                            .elementAtOrNull(0)?.elementAtOrNull(0)?.toString()?.toLongOrNull()
                        val newCounter = ((oldCounter ?: 0) + 1).toString()

                        add(ValueRange().apply {
                            range = rangeRef
                            setValues(listOf(listOf(newCounter)))
                            valueInputOption = "RAW"
                        })
                    }
                }
            }
        ).execute()
    }

    fun delete(id: Long) {
        updateRawValue("$SHEET_NAME_RECIPES!C${id}", listOf(listOf(DELETED_VALUE)))
    }


    private fun updateRawValue(range: String, rawValues: List<List<String>>) {
        sheets.spreadsheets().values().update(
            spreadSheetsId,
            range,
            ValueRange().apply { setValues(rawValues) }
        ).run {
            valueInputOption = "RAW"
            execute()
        }
    }

    private fun readSheetRange(range: String): List<List<Any?>> {
        return sheets.spreadsheets().values()
            .get(spreadSheetsId, range)
            .execute()
            .getValues()
            ?: emptyList()
    }
}
