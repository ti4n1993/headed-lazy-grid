package ua.hospes.lazygrid

import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away subcomposition and span calculation from the measuring logic of entire lines.
 */
internal class LazyGridMeasuredLineProvider(
    private val isVertical: Boolean,
    private val slots: LazyGridSlots,
    private val gridItemsCount: Int,
    private val spaceBetweenLines: Int,
    private val measuredItemProvider: LazyGridMeasuredItemProvider,
    private val spanLayoutProvider: LazyGridSpanLayoutProvider,
    private val measuredLineFactory: MeasuredLineFactory,
) {
    // The constraints for cross axis size. The main axis is not restricted.
    internal fun childConstraints(startSlot: Int, span: Int): Constraints {
        val crossAxisSize = if (span == 1) {
            slots.sizes[startSlot]
        } else {
            val endSlot = startSlot + span - 1
            slots.positions[endSlot] + slots.sizes[endSlot] - slots.positions[startSlot]
        }.coerceAtLeast(0)
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun itemConstraints(itemIndex: Int): Constraints {
        val span = spanLayoutProvider.spanOf(
            itemIndex,
            spanLayoutProvider.slotsPerLine
        )
        return childConstraints(0, span)
    }

    /**
     * Used to subcompose items on lines of lazy grids. Composed placeables will be measured
     * with the correct constraints and wrapped into [LazyGridMeasuredLine].
     */
    fun getAndMeasure(lineIndex: Int): LazyGridMeasuredLine {
        val lineConfiguration = spanLayoutProvider.getLineConfiguration(lineIndex)
        val lineItemsCount = lineConfiguration.spans.size

        // we add space between lines as an extra spacing for all lines apart from the last one
        // so the lazy grid measuring logic will take it into account.
        val mainAxisSpacing = if (lineItemsCount == 0 ||
            lineConfiguration.firstItemIndex + lineItemsCount == gridItemsCount
        ) {
            0
        } else {
            spaceBetweenLines
        }

        var startSlot = 0
        val items = Array(lineItemsCount) {
            val span = lineConfiguration.spans[it].currentLineSpan
            val constraints = childConstraints(startSlot, span)
            measuredItemProvider.getAndMeasure(
                lineConfiguration.firstItemIndex + it,
                mainAxisSpacing,
                constraints
            ).also { startSlot += span }
        }
        return measuredLineFactory.createLine(
            lineIndex,
            items,
            lineConfiguration.spans,
            mainAxisSpacing
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyIndexMap: LazyLayoutKeyIndexMap get() = measuredItemProvider.keyIndexMap
}

// This interface allows to avoid autoboxing on index param
internal fun interface MeasuredLineFactory {
    fun createLine(
        index: Int,
        items: Array<LazyGridMeasuredItem>,
        spans: List<GridItemSpan>,
        mainAxisSpacing: Int
    ): LazyGridMeasuredLine
}