/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.usersearch.api.UserListDataSource
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
class MatrixUserRepository @Inject constructor(
    private val client: MatrixClient,
    private val dataSource: UserListDataSource
) : UserRepository {
    override fun search(query: String): Flow<UserSearchResultState> = flow {
        val processedQuery = when {
            query.startsWith("@") && query.contains(":") -> query  // Full MXID
            query.startsWith("@") -> "$query:prod.enciph-er.com"      // Only @username
            query.isNotEmpty() -> "@$query:prod.enciph-er.com"        // Just username
            else -> query
        }

        Timber.d("Original query: $query")
        Timber.d("Processed query: $processedQuery")

        val shouldQueryProfile = processedQuery.startsWith("@") &&
            MatrixPatterns.isUserId(processedQuery) &&
            !client.isMe(UserId(processedQuery))

        val shouldFetchSearchResults = processedQuery.length >= MINIMUM_SEARCH_LENGTH

        val fakeSearchResult = if (shouldQueryProfile) {
            UserSearchResult(MatrixUser(UserId(processedQuery)))
        } else {
            null
        }

        if (shouldQueryProfile || shouldFetchSearchResults) {
            emit(UserSearchResultState(
                isSearching = shouldFetchSearchResults,
                results = listOfNotNull(fakeSearchResult)
            ))

            if (shouldFetchSearchResults) {
                val results = fetchSearchResults(processedQuery, shouldQueryProfile)
                emit(results)
            }
        }

        Timber.d("Should fetch search results: $shouldFetchSearchResults")

    }

    private suspend fun fetchSearchResults(query: String, shouldQueryProfile: Boolean): UserSearchResultState {
        // Debounce
        delay(DEBOUNCE_TIME_MILLIS)
        val results = dataSource
            .search(query, MAXIMUM_SEARCH_RESULTS)
            .filter { !client.isMe(it.userId) }
            .map { UserSearchResult(it) }
            .toMutableList()

        // If the query is another user's MXID and the result doesn't contain that user ID, query the profile information explicitly
        if (shouldQueryProfile && results.none { it.matrixUser.userId.value == query }) {
            results.add(
                0,
                dataSource.getProfile(UserId(query))
                    ?.let { UserSearchResult(it) }
                    ?: UserSearchResult(MatrixUser(UserId(query)), isUnresolved = true)
            )
        }

        return UserSearchResultState(results = results, isSearching = false)
    }

    companion object {
        private const val DEBOUNCE_TIME_MILLIS = 250L
        private const val MINIMUM_SEARCH_LENGTH = 3
        private const val MAXIMUM_SEARCH_RESULTS = 10L
    }
}

