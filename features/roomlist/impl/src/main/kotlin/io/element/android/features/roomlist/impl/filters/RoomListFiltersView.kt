/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.roomlist.impl.filters

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoomListFiltersView(
    state: RoomListFiltersState,
    modifier: Modifier = Modifier
) {
    fun onClearFiltersClick() {
        state.eventSink(RoomListFiltersEvents.ClearSelectedFilters)
    }

    fun onToggleFilter(filter: RoomListFilter) {
        state.eventSink(RoomListFiltersEvents.ToggleFilter(filter))
    }

    var scrollToStart by remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()
    LaunchedEffect(scrollToStart) {
        // Scroll until the first item start to be displayed
        // Since all items have different size, there is no way to compute the amount of
        // pixel to scroll to go directly to the start of the row.
        // But IRL it should only happen for one item.
        while (lazyListState.firstVisibleItemIndex > 0) {
            lazyListState.animateScrollBy(
                value = -(lazyListState.firstVisibleItemScrollOffset + 1f),
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                )
            )
        }
        // Then scroll to the start of the list, a bit faster, to fully reveal the first
        // item, which can be the close button to reset filter, or the first item
        // if the user has scroll a bit before clicking on the close button.
        lazyListState.animateScrollBy(
            value = -lazyListState.firstVisibleItemScrollOffset.toFloat(),
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
            )
        )
    }
    val previousFilters = remember { mutableStateOf(listOf<RoomListFilter>()) }
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 16.dp),
        modifier = modifier.fillMaxWidth(),
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item("clear_filters") {
            if (state.hasAnyFilterSelected) {
                RoomListClearFiltersButton(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag(TestTags.homeScreenClearFilters),
                    onClick = {
                        previousFilters.value = state.selectedFilters()
                        onClearFiltersClick()
                        // When clearing filter, we want to ensure that the list
                        // of filters is scrolled to the start.
                        scrollToStart++
                    }
                )
            }
        }
        state.filterSelectionStates.forEachIndexed { i, filterWithSelection ->
            item(filterWithSelection.filter) {
                val zIndex = (if (previousFilters.value.contains(filterWithSelection.filter)) state.filterSelectionStates.size else 0) - i.toFloat()
                RoomListFilterView(
                    modifier = Modifier
                        .animateItem()
                        .zIndex(zIndex),
                    roomListFilter = filterWithSelection.filter,
                    selected = filterWithSelection.isSelected,
                    onClick = {
                        previousFilters.value = state.selectedFilters()
                        onToggleFilter(it)
                        // When selecting a filter, we want to scroll to the start of the list
                        if (filterWithSelection.isSelected.not()) {
                            scrollToStart++
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RoomListClearFiltersButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val closeBoxColor = if (ElementTheme.colors.isLight) {
        Color(0xFF002F25)
    } else {
        Color(0xFFCCCCCC)
    }

    val closeColor = if (ElementTheme.colors.isLight) {
        ElementTheme.colors.iconOnSolidPrimary
    } else {
        ElementTheme.colors.iconOnSolidPrimary
    }

    Box(
        modifier = modifier
            .size(36.dp) // Ensures consistent size
            .clip(CircleShape) // Keeps the shape circular
            .background(closeBoxColor) // Sets the background color
            .clickable(onClick = onClick) // Makes the box clickable
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center), // Centers the icon within the box
            imageVector = CompoundIcons.Close(),
            tint = closeColor, // Icon color
            contentDescription = stringResource(id = io.element.android.libraries.ui.strings.R.string.action_clear) // Accessibility text
        )
    }

}

@Composable
private fun RoomListFilterView(
    roomListFilter: RoomListFilter,
    selected: Boolean,
    onClick: (RoomListFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.bgActionPrimaryRest else ElementTheme.colors.bgCanvasDefault,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip background colour",
    )
    val textColour = animateColorAsState(
        targetValue = if (selected) ElementTheme.colors.textOnSolidPrimary else ElementTheme.colors.textPrimary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip text colour",
    )
    // Define fill color based on theme mode
    val containerColor = if (ElementTheme.colors.isLight) {
        Color(0xFF002F25) 
    } else {
        Color(0xFFCCCCCC)
    }

    // Define Text color based on theme mode
    val textColor = if (ElementTheme.colors.isLight) {
        Color(0xFF002F25) // Outline color for light theme
    } else {
        Color(0xFFCCCCCC) // Outline color for dark theme
    }

    // Define outline color based on theme mode
    val outlineColor = if (ElementTheme.colors.isLight) {
        Color(0xFF002F25) // Outline color for light theme
    } else {
        Color(0xFFCCCCCC) // Outline color for dark theme
    }

    FilterChip(
        selected = selected,
        onClick = { onClick(roomListFilter) },
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = containerColor,
            labelColor = textColor,
            selectedLabelColor = textColour.value
        ),
        border = BorderStroke(
            width = 1.dp,
            color = outlineColor // Outline color
        ),
        label = {
            Text(text = stringResource(id = roomListFilter.stringResource))
        }
    )
}

@PreviewsDayNight
@Composable
internal fun RoomListFiltersViewPreview(@PreviewParameter(RoomListFiltersStateProvider::class) state: RoomListFiltersState) = ElementPreview {
    RoomListFiltersView(
        state = state,
    )
}
