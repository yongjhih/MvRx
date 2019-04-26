package com.airbnb.mvrx.sample.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.sample.EpoxyViewModel
import com.airbnb.mvrx.sample.R
import com.airbnb.mvrx.sample.core.MvRxViewModel

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class Marquee @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val subtitleView: TextView

    init {
        inflate(context, R.layout.marquee, this)
        titleView = findViewById(R.id.title)
        subtitleView = findViewById(R.id.subtitle)
        orientation = VERTICAL
    }

    @TextProp
    fun setTitle(title: CharSequence) {
        titleView.text = title
    }

    @TextProp
    fun setSubtitle(subtitle: CharSequence?) {
        subtitleView.visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        subtitleView.text = subtitle
    }
}

data class MarqueeView(
        val title: String = "",
        val subtitle: String = "",
        val onClick: () -> Unit = {}
) : EpoxyViewModel(R.layout.item_marquee) {
    val _titleView by bind<TextView>(R.id.title)
    val _subtitleView by bind<TextView>(R.id.subtitle)

    override fun bind() {
        _titleView?.text = title
        _subtitleView?.text = subtitle
        _subtitleView?.visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        view?.setOnClickListener { onClick() }
    }
}

data class MarqueeState(
        val title: String = "",
        val subtitle: String = "",
        val onClick: () -> Unit = {}
) : MvRxState, EpoxyViewModel(R.layout.item_marquee) {
    val _titleView by bind<TextView>(R.id.title)
    val _subtitleView by bind<TextView>(R.id.subtitle)

    override fun bind() {
        _titleView?.text = title
        _subtitleView?.text = subtitle
        _subtitleView?.visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        view?.setOnClickListener { onClick() }
    }
}

class MarqueeViewModel(
        initialState: MarqueeState
) : MvRxViewModel<MarqueeState>(initialState) {
    fun setState(state: MarqueeState) {
        setState { state }
    }
}