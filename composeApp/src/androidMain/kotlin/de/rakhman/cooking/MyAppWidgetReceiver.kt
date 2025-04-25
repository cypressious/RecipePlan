package de.rakhman.cooking

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.cash.sqldelight.coroutines.asFlow
import de.rakhman.cooking.database.DriverFactory
import de.rakhman.cooking.states.RecipeContext
import de.rakhman.cooking.states.syncWithSheets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyAppWidget()
}

class MyAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*>?
        get() = object : GlanceStateDefinition<List<Recipe>> {
            override suspend fun getDataStore(context: Context, fileKey: String): DataStore<List<Recipe>> {
                val driver = DriverFactory(context).createDriver()
                val database = Database(driver)

                return object : DataStore<List<Recipe>> {
                    override val data: Flow<List<Recipe>>
                        get() = database.recipesQueries.selectAll().asFlow()
                            .combine(database.planQueries.selectAll().asFlow()) { recipeQuery, planQuery ->
                                val allRecipes = recipeQuery.executeAsList().associateBy { it.id }
                                planQuery.executeAsList().mapNotNull { allRecipes[it] }
                            }

                    override suspend fun updateData(transform: suspend (List<Recipe>) -> List<Recipe>): List<Recipe> {
                        throw NotImplementedError()
                    }
                }
            }

            override fun getLocation(context: Context, fileKey: String): File {
                throw NotImplementedError()
            }
        }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetLayout(currentState())
            }
        }
    }

    @Composable
    private fun WidgetLayout(recipes: List<Recipe>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
        ) {
            Row(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clickable(actionStartActivity(MainActivity::class.java))
            ) {
                Text(
                    text = LocalContext.current.getString(R.string.app_name),
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.primary)
                )
                Button(
                    text = LocalContext.current.getString(R.string.reload),
                    onClick = actionRunCallback<RefreshAction>()
                )
            }

            if (recipes.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = LocalContext.current.getString(R.string.no_items), style = TextStyle(fontSize = 20.sp, color = GlanceTheme.colors.onBackground))
                }
                return@Column
            }

            LazyColumn {
                items(
                    count = recipes.size,
                    itemId = { recipes[it].id }
                ) { i ->
                    val recipe = recipes[i]
                    var modifier: GlanceModifier = GlanceModifier
                    recipe.url?.let { url ->
                        modifier = modifier.clickable(actionStartActivity(intent = url.toUrlIntent()))
                    }
                    Row(modifier = modifier) {
                        Column(
                            modifier = GlanceModifier.fillMaxWidth().padding(16.dp, 6.dp),
                        ) {
                            Text(
                                text = recipe.title,
                                modifier = GlanceModifier.padding(bottom = 6.dp),
                                style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onBackground)
                            )
                            recipe.url?.let { url ->
                                Text(
                                    text = url,
                                    maxLines = 1,
                                    style = TextStyle(color = GlanceTheme.colors.secondary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        with(context) {
            coroutineScope {
                val authz = handleAuthz().await() ?: return@coroutineScope

                val driver = DriverFactory(context).createDriver()
                val database = Database(driver)

                val spreadSheetsId =
                    database.settingsQueries.selectFirst().executeAsOneOrNull() ?: return@coroutineScope

                withContext(Dispatchers.IO) {
                    val recipeContext = RecipeContext(
                        database = database,
                        sheets = buildSheetsService(authz.toCredentials()),
                        spreadSheetsId = spreadSheetsId,
                        platformContext = context
                    )
                    with(recipeContext) {
                        syncWithSheets()
                    }
                }
            }
        }
    }
}
