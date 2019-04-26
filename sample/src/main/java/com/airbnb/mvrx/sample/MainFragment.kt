package com.airbnb.mvrx.sample

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.airbnb.epoxy.EpoxyHolder
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.mvrx.sample.core.BaseFragment
import com.airbnb.mvrx.sample.core.simpleController
import com.airbnb.mvrx.sample.views.basicRow
import com.airbnb.mvrx.sample.views.marquee
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class MainFragment : BaseFragment() {

    override fun epoxyController() = simpleController {
        marquee {
            id("marquee")
            title("Welcome to MvRx")
            subtitle("Select a demo below")
        }

        basicRow {
            id("hello_world")
            title("Hello World")
            subtitle(demonstrates("Simple MvRx usage"))
            clickListener { _ -> navigateTo(R.id.action_main_to_helloWorldFragment) }
        }

        basicRow {
            id("hello_world_epoxy")
            title("Hello World (Epoxy)")
            subtitle(demonstrates("Simple MvRx usage", "Epoxy integration"))
            clickListener { _ -> navigateTo(R.id.action_main_to_helloWorldEpoxyFragment) }
        }

        basicRow {
            id("random_dad_joke")
            title("Random Dad Joke")
            subtitle(demonstrates("fragmentViewModel", "Network requests", "Dependency Injection"))
            clickListener { _ -> navigateTo(R.id.action_main_to_randomDadJokeFragment) }
        }

        basicRow {
            id("dad_jokes")
            title("Dad Jokes")
            subtitle(
                demonstrates(
                    "fragmentViewModel",
                    "Fragment arguments",
                    "Network requests",
                    "Pagination",
                    "Dependency Injection"
                )
            )
            clickListener { _ -> navigateTo(R.id.action_mainFragment_to_dadJokeIndex) }
        }

        basicRow {
            id("flow")
            title("Flow")
            subtitle(
                demonstrates(
                    "Sharing data across screens",
                    "activityViewModel and existingViewModel"
                )
            )
            clickListener { _ -> navigateTo(R.id.action_main_to_flowIntroFragment) }
        }
    }

    private fun demonstrates(vararg items: String) =
        arrayOf("Demonstrates:", *items).joinToString("\n\t\t• ")
}

/**
 * A pattern for easier view binding with an [EpoxyHolder]
 */
abstract class KotlinEpoxyHolder : EpoxyHolder() {
    private lateinit var view: View

    override fun bindView(itemView: View) {
        view = itemView
    }

    protected fun <V : View> bind(id: Int): ReadOnlyProperty<KotlinEpoxyHolder, V> =
            Lazy { holder: KotlinEpoxyHolder, prop ->
                holder.view.findViewById(id) as V?
                        ?: throw IllegalStateException("View ID $id for '${prop.name}' not found.")
            }

    /**
     * Taken from Kotterknife.
     * https://github.com/JakeWharton/kotterknife
     */
    private class Lazy<V>(
            private val initializer: (KotlinEpoxyHolder, KProperty<*>) -> V
    ) : ReadOnlyProperty<KotlinEpoxyHolder, V> {
        private object EMPTY

        private var value: Any? = EMPTY

        override fun getValue(thisRef: KotlinEpoxyHolder, property: KProperty<*>): V {
            if (value == EMPTY) {
                value = initializer(thisRef, property)
            }
            @Suppress("UNCHECKED_CAST")
            return value as V
        }
    }
}

abstract class EpoxyViewModel(
        @LayoutRes private val layoutRes: Int
) : EpoxyModel<View>() {

    var view: View? = null

    abstract fun bind()

    override fun bind(view: View) {
        this.view = view
        bind()
    }

    override fun unbind(view: View) {
        this.view = null
    }

    override fun getDefaultLayout() = layoutRes


    protected fun <V : View> bind(@IdRes id: Int) = object : ReadOnlyProperty<EpoxyViewModel, V?> {
        override fun getValue(thisRef: EpoxyViewModel, property: KProperty<*>): V? {
            // This is not efficient because it looks up the view by id every time (it loses
            // the pattern of a "holder" to cache that look up). But it is simple to use and could
            // be optimized with a map
            @Suppress("UNCHECKED_CAST")
            return view?.findViewById(id) as? V
        }
    }


}
