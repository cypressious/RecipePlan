package de.rakhman.cooking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import de.rakhman.cooking.getContext
import de.rakhman.cooking.openUrl
import de.rakhman.cooking.states.RecipeDto
import org.jetbrains.compose.resources.stringResource
import recipeplan.composeapp.generated.resources.*

@Composable
fun RecipeItem(
    recipe: RecipeDto,
    modifier: Modifier = Modifier,
    slotLeft: (@Composable RowScope.() -> Unit)? = null,
    slotRight: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = getContext()
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = { recipe.url?.let { openUrl(it, context) } }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slotLeft?.invoke(this)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (slotLeft == null) 16.dp else 0.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .weight(1f)
        ) {
            Text(
                text = recipe.title,
                fontSize = 18.sp,
            )
            if (recipe.counter > 0 || !recipe.url.isNullOrBlank()) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min).padding(top = 2.dp)
                ) {
                    if (recipe.counter > 0) {
                        Text(
                            text = stringResource(Res.string.cooked_times, recipe.counter),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    recipe.url?.ifBlank { null }?.let {
                        if (recipe.counter > 0) {
                            VerticalDivider(Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 2.dp))
                        }

                        Text(
                            text = it,
                            maxLines = 1,
                            fontSize = 14.sp,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            if (recipe.tags.isNotEmpty()) {
                FlowRow(modifier = Modifier.padding(bottom = 4.dp)) {
                    recipe.tags.forEach {
                        RecipeTag(it)
                    }
                }
            }
        }

        slotRight?.invoke(this)
    }
}

@Composable
fun RecipeTag(string: String, selected: Boolean = true, clickable: Boolean = false, onClick: () -> Unit = { }) {
    val shape = RoundedCornerShape(8.dp)
    val modifier = Modifier
        .padding(top = 4.dp, end = 4.dp)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
        .background(
            if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            shape
        )
        .clip(shape)
        .clickable(clickable, onClick = onClick)
        .padding(vertical = 2.dp, horizontal = 8.dp)


    Text(
        text = string,
        fontSize = 14.sp,
        modifier = modifier
    )
}

