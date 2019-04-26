package com.airbnb.mvrx.sample.features.dadjoke

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import android.util.Log
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.sample.R
import com.airbnb.mvrx.sample.core.BaseFragment
import com.airbnb.mvrx.sample.core.simpleController
import com.airbnb.mvrx.sample.views.*

private const val TAG = "DadJokeIndexFragment"

class DadJokeIndexFragment : BaseFragment() {

    /**
     * This will get or create a new ViewModel scoped to this Fragment. It will also automatically
     * subscribe to all state changes and call [invalidate] which we have wired up to
     * call [buildModels] in [BaseFragment].
     */
    private val marqueeViewModel: MarqueeViewModel by fragmentViewModel()
    private val viewModel: DadJokeIndexViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /**
         * Use viewModel.subscribe to listen for changes. The parameter is a shouldUpdate
         * function that is given the old state and new state and returns whether or not to
         * call the subscriber. onSuccess, onFail, and propertyWhitelist ship with MvRx.
         */
        viewModel.asyncSubscribe(DadJokeIndexState::request, onFail = { error ->
            Snackbar.make(coordinatorLayout, "Jokes request failed.", Snackbar.LENGTH_INDEFINITE)
                .show()
            Log.w(TAG, "Jokes request failed", error)
        })
        marqueeViewModel.setState(MarqueeState(title = "Andrew", subtitle = "Chen"))
        view.postDelayed({
            marqueeViewModel.setState(MarqueeState(title = "Andrew2", subtitle = "Chen"))
        }, 3000)
        view.postDelayed({
            marqueeViewModel.setState(MarqueeState(title = "Andrew2", subtitle = "Chen2"))
        }, 6000)
    }

    override fun epoxyController() = simpleController(marqueeViewModel, viewModel) { marqueeState, state ->
        marquee {
            id("marquee")
            title("Dad Jokes")
        }

        /*
        MarqueeView(title = marqueeState.title, subtitle = marqueeState.subtitle)
                .apply { id(title) }
                .addTo(this)
        */

        marqueeState
                .apply { id(title) }
                .addTo(this)

        state.jokes.forEach { joke ->
            /*
            basicRow {
                id(joke.id)
                title(joke.joke)
                clickListener { _ ->
                    navigateTo(
                        R.id.action_dadJokeIndex_to_dadJokeDetailFragment,
                        DadJokeDetailArgs(joke.id)
                    )
                }
            }
            */
            BasicRowView(title = joke.joke, onClick = {
                navigateTo(
                        R.id.action_dadJokeIndex_to_dadJokeDetailFragment,
                        DadJokeDetailArgs(joke.id)
                )
            }).id(joke.id).addTo(this)
        }

        loadingRow {
            // Changing the ID will force it to rebind when new data is loaded even if it is
            // still on screen which will ensure that we trigger loading again.
            id("loading${state.jokes.size}")
            onBind { _, _, _ -> viewModel.fetchNextPage() }
        }
    }
}