/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.roomlist.impl.filters.selection

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.features.roomlist.impl.filters.RoomListFilter
import io.element.android.libraries.di.SessionScope
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class DefaultFilterSelectionStrategy @Inject constructor() : FilterSelectionStrategy {
    private val selectedFilters = LinkedHashSet<RoomListFilter>()

    override val filterSelectionStates = MutableStateFlow(buildFilters())

    override fun select(filter: RoomListFilter) {
        // If selecting a specific filter, remove incompatible filters
        if (filter != RoomListFilter.All) {
            selectedFilters.removeAll(filter.incompatibleFilters)
        }

        // Add the new filter
        selectedFilters.add(filter)

        filterSelectionStates.value = buildFilters()
    }

    override fun deselect(filter: RoomListFilter) {
        // Prevent deselecting specific filters if it would leave no filters selected
        if (selectedFilters.size == 1 && selectedFilters.contains(filter)) return

        selectedFilters.remove(filter)

        filterSelectionStates.value = buildFilters()
    }

    override fun isSelected(filter: RoomListFilter): Boolean {
        return selectedFilters.contains(filter)
    }

    override fun clear() {
        selectedFilters.clear()
        selectedFilters.add(RoomListFilter.All)
        filterSelectionStates.value = buildFilters()
    }

    private fun buildFilters(): Set<FilterSelectionState> {
        // If any non-All filter is selected, exclude "All"
        if (selectedFilters.any { it != RoomListFilter.All }) {
            val selectedFilterStates = selectedFilters.map {
                FilterSelectionState(
                    filter = it,
                    isSelected = true
                )
            }

            val unselectedFilters = RoomListFilter.entries - selectedFilters -
                selectedFilters.flatMap { it.incompatibleFilters }.toSet() -
                RoomListFilter.All

            val unselectedFilterStates = unselectedFilters.map {
                FilterSelectionState(
                    filter = it,
                    isSelected = false
                )
            }

            return (selectedFilterStates + unselectedFilterStates).toSet()
        }

        // Otherwise, return all filters with "All" selected
        val unselectedFilters = RoomListFilter.entries - RoomListFilter.All

        val unselectedFilterStates = unselectedFilters.map {
            FilterSelectionState(
                filter = it,
                isSelected = false
            )
        }

        val allFilterState = FilterSelectionState(
            filter = RoomListFilter.All,
            isSelected = true
        )

        return setOf(allFilterState) + unselectedFilterStates
    }
}
