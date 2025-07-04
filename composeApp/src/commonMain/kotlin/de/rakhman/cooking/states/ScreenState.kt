package de.rakhman.cooking.states

import io.sellmair.evas.State
import org.jetbrains.compose.resources.StringResource
import recipeplan.composeapp.generated.resources.*

sealed class ScreenState(val title: StringResource) : State {
    sealed class BaseScreen(title: StringResource) : ScreenState(title)
    data object Recipes : BaseScreen(Res.string.recipes)
    data object Plan : BaseScreen(Res.string.plan)
    data object Shop : BaseScreen(Res.string.shop)
    class Add(val target: BaseScreen, val editingRecipe: RecipeDto? = null, val initialData: String? = null) : ScreenState(Res.string.add_recipe)
    data object Settings : ScreenState(Res.string.settings)
    data object Inspiration : ScreenState(Res.string.inspiration)

    companion object Key : State.Key<ScreenState> {
        override val default: ScreenState
            get() = Plan
    }
}
