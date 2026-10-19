package com.example.acetimetracker.core.ui

import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.acetimetracker.R

private data class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

internal fun installLegacyWindowInsets(content: ViewGroup) {
    val initial = (content.getTag(R.id.legacy_initial_padding) as? InitialPadding)
        ?: InitialPadding(content.paddingLeft, content.paddingTop, content.paddingRight, content.paddingBottom)
            .also { content.setTag(R.id.legacy_initial_padding, it) }
    val barsAndCutout = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    val handled = barsAndCutout or WindowInsetsCompat.Type.ime()
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars = insets.getInsets(barsAndCutout)
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(
            initial.left + bars.left,
            initial.top + bars.top,
            initial.right + bars.right,
            initial.bottom + maxOf(bars.bottom, ime.bottom)
        )
        // Children retain gesture/tappable-element insets, but must not apply these twice.
        WindowInsetsCompat.Builder(insets)
            .setInsets(handled, Insets.NONE)
            .setInsetsIgnoringVisibility(barsAndCutout, Insets.NONE)
            .setDisplayCutout(null)
            .build()
    }
    ViewCompat.requestApplyInsets(content)
}
