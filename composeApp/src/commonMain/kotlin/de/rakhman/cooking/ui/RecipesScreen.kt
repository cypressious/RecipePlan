@file:OptIn(ExperimentalMaterial3Api::class)

package de.rakhman.cooking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.Settled
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.events.AddToPlanEvent
import de.rakhman.cooking.events.AddToShopEvent
import de.rakhman.cooking.events.ChangeScreenEvent
import de.rakhman.cooking.events.NotificationEvent
import de.rakhman.cooking.states.RecipeDto
import de.rakhman.cooking.states.RecipesState
import de.rakhman.cooking.states.ScreenState
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.compose.rememberEvasCoroutineScope
import io.sellmair.evas.emitAsync
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import recipeplan.composeapp.generated.resources.*

@Composable
fun RecipesScreen(modifier: Modifier) {
    val recipeState = RecipesState.composeValue()
    Column(modifier = modifier) {
        when (recipeState) {
            is RecipesState.Success -> {
                Recipes(recipeState)
            }

            RecipesState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

enum class SortOrder(
    val comparator: Comparator<RecipeDto>
) {
    Name(compareBy { it.title.lowercase() }),
    Counter(compareByDescending<RecipeDto> { it.counter }.thenBy { it.title.lowercase() }),
}

@Composable
private fun Recipes(state: RecipesState.Success) {
    val recipes = state.recipes
    var filter by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortOrder.Name) }
    var tagsToShow by remember { mutableStateOf(emptySet<String>()) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp, 12.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        OutlinedTextField(
            modifier = Modifier.weight(1f).padding(end = 12.dp, bottom = 8.dp).focusRequester(focusRequester),
            value = filter,
            onValueChange = { filter = it },
            label = { Text(stringResource(Res.string.search)) },
            trailingIcon = {
                if (filter.isNotEmpty()) {
                    IconButton(
                        modifier = Modifier.padding(4.dp, 0.dp),
                        onClick = { filter = "" },
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(Res.string.clear_search),
                        )
                    }
                }
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
        )

        collectEventsComposable<ChangeScreenEvent> { event ->
            if (event.screen == ScreenState.Recipes) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        IconButton(onClick = {
            sort = SortOrder.entries[(sort.ordinal + 1) % SortOrder.entries.size]
        }) {
            Icon(
                imageVector = vectorResource(Res.drawable.bootstrap_sort_down),
                contentDescription = stringResource(Res.string.sort),
                modifier = Modifier.size(24.dp)
            )
        }

        var showTagsFilter by remember { mutableStateOf(false) }

        IconButton(onClick = {
            showTagsFilter = true
        }) {
            Icon(
                imageVector = vectorResource(if (tagsToShow.isNotEmpty()) Res.drawable.tags_fill else Res.drawable.tags),
                contentDescription = stringResource(Res.string.sort),
                modifier = Modifier.size(24.dp)
            )
        }

        if (showTagsFilter) {
            ModalBottomSheet(onDismissRequest = { showTagsFilter = false}) {
                FlowRow(modifier = Modifier.padding(16.dp)) {
                    for (tag in state.allTags) {
                        val shown = tag in tagsToShow
                        RecipeTag(
                            string = tag,
                            selected = shown,
                            clickable = true,
                            onClick = {
                                if (shown) tagsToShow -= tag else tagsToShow += tag
                            }
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider()

    if (recipes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_entries), fontSize = 20.sp)
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filter, sort) { listState.scrollToItem(0) }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 70.dp)) {
        val filteredList = recipes
            .filter { tagsToShow.isEmpty() || it.tags.any { tag -> tag in tagsToShow } }
            .let { list ->
                if (filter.isEmpty()) {
                    list.sortedWith(sort.comparator)
                } else {
                    list
                        .filter { it.matchesFilter(filter.trim()) }
                        .sortedWith(compareByDescending<RecipeDto> {
                            it.title.startsWith(filter.trim(), ignoreCase = true)
                        }.thenByDescending {
                            it.title.contains(filter.trim(), ignoreCase = true)
                        })
                }
            }

        items(
            count = filteredList.size,
            key = { i -> filteredList[i].id },
            itemContent = { i ->
                val recipe = filteredList[i]

                SwipeableRecipeItem(recipe)

                if (i != filteredList.lastIndex) HorizontalDivider(modifier = Modifier.animateItem())
            },
        )
    }
}

@Composable
private fun LazyItemScope.SwipeableRecipeItem(recipe: RecipeDto) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberEvasCoroutineScope()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != Settled) {
            scope.launch {
                if (dismissState.dismissDirection == StartToEnd) {
                    AddToPlanEvent(recipe.id).emitAsync()
                    NotificationEvent(getString(Res.string.recipe_added_to_plan, recipe.title)).emitAsync()
                } else {
                    AddToShopEvent(recipe.id).emitAsync()
                    NotificationEvent(getString(Res.string.recipe_added_to_shop, recipe.title)).emitAsync()
                }
                dismissState.reset()
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Icon(
                imageVector = if (dismissState.dismissDirection == StartToEnd) Icons.Filled.DateRange else Icons.Filled.ShoppingCart,
                contentDescription = if (dismissState.dismissDirection == StartToEnd) {
                    stringResource(Res.string.add_to_plan)
                } else {
                    stringResource(Res.string.add_to_shop)
                },
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .wrapContentSize(if (dismissState.dismissDirection == StartToEnd) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 24.dp),
            )
        }
    ) {
        RecipeItem(
            recipe = recipe,
            modifier = Modifier.animateItem(),
            slotRight = { RecipeDropdown(recipe, ScreenState.Recipes) }
        )
    }
}

private fun RecipeDto.matchesFilter(filter: String): Boolean =
    title.contains(filter, ignoreCase = true) ||
            url?.contains(filter, ignoreCase = true) == true

