/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumble.appyx.core.composable.PermanentChild
import com.bumble.appyx.core.lifecycle.subscribe
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.navigation.NavElements
import com.bumble.appyx.core.navigation.NavKey
import com.bumble.appyx.core.navigation.model.permanent.PermanentNavModel
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.BackStack.State.ACTIVE
import com.bumble.appyx.navmodel.backstack.BackStack.State.CREATED
import com.bumble.appyx.navmodel.backstack.BackStack.State.STASHED
import com.bumble.appyx.navmodel.backstack.BackStackElement
import com.bumble.appyx.navmodel.backstack.BackStackElements
import com.bumble.appyx.navmodel.backstack.activeElement
import com.bumble.appyx.navmodel.backstack.operation.BackStackOperation
import com.bumble.appyx.navmodel.backstack.operation.Push
import com.bumble.appyx.navmodel.backstack.operation.pop
import com.bumble.appyx.navmodel.backstack.operation.push
import com.bumble.appyx.navmodel.backstack.operation.replace
import com.bumble.appyx.navmodel.backstack.operation.singleTop
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import im.vector.app.features.analytics.plan.JoinedRoom
import io.element.android.anvilannotations.ContributesNode
import io.element.android.appnav.loggedin.LoggedInNode
import io.element.android.appnav.loggedin.SendQueues
import io.element.android.appnav.room.RoomFlowNode
import io.element.android.appnav.room.RoomNavigationTarget
import io.element.android.appnav.room.joined.JoinedRoomLoadedFlowNode
import io.element.android.features.call.impl.callshistory.Call
import io.element.android.features.createroom.api.CreateRoomEntryPoint
import io.element.android.features.ftue.api.FtueEntryPoint
import io.element.android.features.ftue.api.state.FtueService
import io.element.android.features.ftue.api.state.FtueState
import io.element.android.features.logout.api.LogoutEntryPoint
import io.element.android.features.preferences.api.PreferencesEntryPoint
import io.element.android.features.roomdirectory.api.RoomDescription
import io.element.android.features.roomdirectory.api.RoomDirectoryEntryPoint
import io.element.android.features.roomlist.api.RoomListEntryPoint
import io.element.android.features.securebackup.api.SecureBackupEntryPoint
import io.element.android.features.share.api.ShareEntryPoint
import io.element.android.features.userprofile.api.UserProfileEntryPoint
import io.element.android.features.verifysession.api.IncomingVerificationEntryPoint
import io.element.android.libraries.architecture.BackstackView
import io.element.android.libraries.architecture.BaseFlowNode
import io.element.android.libraries.architecture.createNode
import io.element.android.libraries.architecture.waitForNavTargetAttached
import io.element.android.libraries.designsystem.components.navbar.BottomNavBar
import io.element.android.libraries.designsystem.components.navbar.BottomNavRoute
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.MAIN_SPACE
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.RoomIdOrAlias
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.core.toRoomIdOrAlias
import io.element.android.libraries.matrix.api.permalink.PermalinkData
import io.element.android.libraries.matrix.api.verification.SessionVerificationRequestDetails
import io.element.android.libraries.matrix.api.verification.SessionVerificationServiceListener
import io.element.android.services.appnavstate.api.AppNavigationStateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.util.Optional
import java.util.UUID
import io.element.android.features.call.impl.callshistory.CallsHistoryNode
import kotlinx.parcelize.RawValue

private const val TAG = "LoggedInFlowNode"

@ContributesNode(SessionScope::class)
class LoggedInFlowNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val roomListEntryPoint: RoomListEntryPoint,
    private val preferencesEntryPoint: PreferencesEntryPoint,
    private val createRoomEntryPoint: CreateRoomEntryPoint,
    private val appNavigationStateService: AppNavigationStateService,
    private val secureBackupEntryPoint: SecureBackupEntryPoint,
    private val userProfileEntryPoint: UserProfileEntryPoint,
    private val ftueEntryPoint: FtueEntryPoint,
    private val coroutineScope: CoroutineScope,
    private val ftueService: FtueService,
    private val roomDirectoryEntryPoint: RoomDirectoryEntryPoint,
    private val shareEntryPoint: ShareEntryPoint,
    private val matrixClient: MatrixClient,
    private val sendingQueue: SendQueues,
    private val logoutEntryPoint: LogoutEntryPoint,
    private val incomingVerificationEntryPoint: IncomingVerificationEntryPoint,
    snackbarDispatcher: SnackbarDispatcher,
) : BaseFlowNode<LoggedInFlowNode.NavTarget>(
    backstack = BackStack(
        initialElement = NavTarget.Placeholder,
        savedStateMap = buildContext.savedStateMap,
    ),
    permanentNavModel = PermanentNavModel(
        navTargets = setOf(NavTarget.LoggedInPermanent),
        savedStateMap = buildContext.savedStateMap,
    ),
    buildContext = buildContext,
    plugins = plugins
) {
    interface Callback : Plugin {
        fun onOpenBugReport()
    }

    private val settingsRootVisible = MutableStateFlow(false)

    private val syncService = matrixClient.syncService()
    private val loggedInFlowProcessor = LoggedInEventProcessor(
        snackbarDispatcher,
        matrixClient.roomMembershipObserver(),
    )

    private val verificationListener = object : SessionVerificationServiceListener {
        override fun onIncomingSessionRequest(sessionVerificationRequestDetails: SessionVerificationRequestDetails) {
            if (backstack.elements.value.none { it.key.navTarget is NavTarget.IncomingVerificationRequest }) {
                safeSingleTop(NavTarget.IncomingVerificationRequest(sessionVerificationRequestDetails))
            }
        }
    }

    private val navigationJobs = mutableListOf<kotlinx.coroutines.Job>()

    override fun onBuilt() {
        super.onBuilt()

        lifecycle.subscribe(
            onCreate = {
                appNavigationStateService.onNavigateToSession(id, matrixClient.sessionId)
                appNavigationStateService.onNavigateToSpace(id, MAIN_SPACE)
                loggedInFlowProcessor.observeEvents(coroutineScope)
                matrixClient.sessionVerificationService().setListener(verificationListener)

                val ftueJob = ftueService.state
                    .onEach { ftueState ->
                        when (ftueState) {
                            is FtueState.Unknown -> Unit
                            is FtueState.Incomplete -> {
                                if (backstack.elements.value.none { it.key.navTarget is NavTarget.Ftue }) {
                                    backstack.safeRoot(NavTarget.Ftue)
                                }
                            }
                            is FtueState.Complete -> {
                                if (backstack.elements.value.none { it.key.navTarget is NavTarget.RoomList }) {
                                    backstack.safeRoot(NavTarget.RoomList)
                                }
                            }
                        }
                    }
                    .launchIn(lifecycleScope)
                navigationJobs.add(ftueJob)
            },
            onDestroy = {
                navigationJobs.forEach { it.cancel() }
                navigationJobs.clear()
                
                appNavigationStateService.onLeavingSpace(id)
                appNavigationStateService.onLeavingSession(id)
                loggedInFlowProcessor.stopObserving()
                matrixClient.sessionVerificationService().setListener(null)
            }
        )
        setupSendingQueue()
    }

    private fun setupSendingQueue() {
        sendingQueue.launchIn(lifecycleScope)
    }

    sealed interface NavTarget : Parcelable {
        @Parcelize
        data object Placeholder : NavTarget

        @Parcelize
        data object LoggedInPermanent : NavTarget

        @Parcelize
        data object RoomList : NavTarget

        @Parcelize
        data class Room(
            val roomIdOrAlias: RoomIdOrAlias,
            val serverNames: List<String> = emptyList(),
            val trigger: JoinedRoom.Trigger? = null,
            val roomDescription: RoomDescription? = null,
            val initialElement: RoomNavigationTarget = RoomNavigationTarget.Messages(),
            val targetId: UUID = UUID.randomUUID(),
        ) : NavTarget

        @Parcelize
        data object Calls : NavTarget
        
        @Parcelize
        data class CallDetails(
            val call: @kotlinx.parcelize.RawValue io.element.android.features.call.impl.callshistory.Call
        ) : NavTarget

        @Parcelize
        data class UserProfile(
            val userId: UserId,
        ) : NavTarget

        @Parcelize
        data class Settings(
            val initialElement: PreferencesEntryPoint.InitialTarget = PreferencesEntryPoint.InitialTarget.Root
        ) : NavTarget

        @Parcelize
        data object CreateRoom : NavTarget

        @Parcelize
        data class SecureBackup(
            val initialElement: SecureBackupEntryPoint.InitialTarget = SecureBackupEntryPoint.InitialTarget.Root
        ) : NavTarget

        @Parcelize
        data object Ftue : NavTarget

        @Parcelize
        data object RoomDirectorySearch : NavTarget

        @Parcelize
        data class IncomingShare(val intent: Intent) : NavTarget

        @Parcelize
        data object LogoutForNativeSlidingSyncMigrationNeeded : NavTarget

        @Parcelize
        data class IncomingVerificationRequest(val data: SessionVerificationRequestDetails) : NavTarget
    }

    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node {
        Timber.tag(TAG).d("Resolving nav target: $navTarget")
        return when (navTarget) {
            NavTarget.Placeholder -> createNode<PlaceholderNode>(buildContext)
            NavTarget.LoggedInPermanent -> {
                val callback = object : LoggedInNode.Callback {
                    override fun navigateToNotificationTroubleshoot() {
                        safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.NotificationSettings))
                    }
                }
                createNode<LoggedInNode>(buildContext, listOf(callback))
            }
            NavTarget.RoomList -> {
                roomListEntryPoint.nodeBuilder(this, buildContext)
                    .callback(object : RoomListEntryPoint.Callback {
                        override fun onRoomClick(roomId: RoomId) {
                            safePush(NavTarget.Room(roomId.toRoomIdOrAlias()))
                        }
                        
                        override fun onSettingsClick() {
                            // Skip preferences and navigate directly to user profile edit
                            safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.UserProfile(matrixClient.userProfile.value)))
                        }
                        
                        override fun onCreateRoomClick() {
                            safePush(NavTarget.CreateRoom)
                        }
                        
                        override fun onSetUpRecoveryClick() {
                            safePush(NavTarget.SecureBackup())
                        }
                        
                        override fun onSessionConfirmRecoveryKeyClick() {
                            safePush(NavTarget.SecureBackup(SecureBackupEntryPoint.InitialTarget.EnterRecoveryKey))
                        }
                        
                        override fun onRoomSettingsClick(roomId: RoomId) {
                            safePush(NavTarget.Room(roomId.toRoomIdOrAlias(), initialElement = RoomNavigationTarget.Details))
                        }
                        
                        override fun onReportBugClick() {
                            plugins<Callback>().forEach { it.onOpenBugReport() }
                        }
                        
                        override fun onRoomDirectorySearchClick() {
                            safePush(NavTarget.RoomDirectorySearch)
                        }
                        
                        override fun onLogoutForNativeSlidingSyncMigrationNeeded() {
                            safePush(NavTarget.LogoutForNativeSlidingSyncMigrationNeeded)
                        }
                    })
                    .build()
            }
            is NavTarget.Room -> {
                val inputs = RoomFlowNode.Inputs(
                    roomIdOrAlias = navTarget.roomIdOrAlias,
                    roomDescription = Optional.empty(),
                    serverNames = emptyList(),
                    trigger = Optional.empty(),
                    initialElement = RoomNavigationTarget.Messages()
                )
                val callback = object : JoinedRoomLoadedFlowNode.Callback {
                    override fun onOpenRoom(roomId: RoomId) {
                        safePush(NavTarget.Room(roomId.toRoomIdOrAlias()))
                    }
                    
                    override fun onPermalinkClick(data: PermalinkData, pushToBackstack: Boolean) {
                        if (pushToBackstack) {
                            handlePermalink(data, true)
                        } else {
                            handlePermalink(data, false)
                        }
                    }
                    
                    override fun onForwardedToSingleRoom(roomId: RoomId) {
                        safePush(NavTarget.Room(roomId.toRoomIdOrAlias()))
                    }
                    
                    override fun onOpenGlobalNotificationSettings() {
                        safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.NotificationSettings))
                    }
                }
                createNode<RoomFlowNode>(buildContext, listOf(inputs, callback))
            }
            NavTarget.Calls -> {
                val callback = object : CallsHistoryNode.Callback {
                    override fun onRoomDetailsClick(roomId: RoomId) {
                        safePush(NavTarget.Room(roomId.toRoomIdOrAlias()))
                    }
                    
                    override fun onHomeClick() {
                        safePush(NavTarget.RoomList)
                    }
                    
                    override fun onSettingsClick() {
                        safePush(NavTarget.Settings())
                    }
                    
                    override fun navigateToCallDetails(call: Call) {
                        // Navigate to the call details screen using the new CallDetails NavTarget
                        safePush(NavTarget.CallDetails(call))
                    }
                }
                createNode<CallsHistoryNode>(buildContext, plugins = listOf(callback))
            }
            is NavTarget.UserProfile -> {
                userProfileEntryPoint.nodeBuilder(this, buildContext)
                    .params(UserProfileEntryPoint.Params(userId = navTarget.userId))
                    .callback(object : UserProfileEntryPoint.Callback {
                        override fun onOpenRoom(roomId: RoomId) {
                            safePush(NavTarget.Room(roomId.toRoomIdOrAlias()))
                        }
                    })
                    .build()
            }
            is NavTarget.Settings -> {
                val callback = object : PreferencesEntryPoint.Callback {
                    override fun onSettingsRootVisibilityChanged(isVisible: Boolean) {
                        settingsRootVisible.value = isVisible
                    }
                    override fun onOpenBugReport() {
                        plugins<Callback>().forEach { it.onOpenBugReport() }
                    }

                    override fun onSecureBackupClick() {
                        safePush(NavTarget.SecureBackup())
                    }

                    override fun onOpenRoomNotificationSettings(roomId: RoomId) {
                        safePush(NavTarget.Room(roomId.toRoomIdOrAlias(), initialElement = RoomNavigationTarget.NotificationSettings))
                    }

                    override fun onScreenLockClick() {
                        safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.ScreenLock))
                    }

                    override fun onAdvancedSettingsClick() {
                        safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.AdvancedSettings))
                    }

                    override fun onSignOutClick() {
                        safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.SignOut))
                    }

                    override fun onDeactivateAccountClick() {
                        safePush(NavTarget.Settings(PreferencesEntryPoint.InitialTarget.DeactivateAccount))
                    }
                }
                val inputs = PreferencesEntryPoint.Params(navTarget.initialElement)
                preferencesEntryPoint.nodeBuilder(this, buildContext)
                    .params(inputs)
                    .callback(callback)
                    .build()
            }
            NavTarget.CreateRoom -> {
                val callback = object : CreateRoomEntryPoint.Callback {
                    override fun onSuccess(roomId: RoomId) {
                        backstack.replace(NavTarget.Room(roomId.toRoomIdOrAlias()))
                    }
                }

                createRoomEntryPoint
                    .nodeBuilder(this, buildContext)
                    .callback(callback)
                    .build()
            }
            is NavTarget.SecureBackup -> {
                secureBackupEntryPoint.nodeBuilder(this, buildContext)
                    .params(SecureBackupEntryPoint.Params(initialElement = navTarget.initialElement))
                    .callback(object : SecureBackupEntryPoint.Callback {
                        override fun onDone() {
                            backstack.pop()
                        }
                    })
                    .build()
            }
            NavTarget.Ftue -> {
                ftueEntryPoint.nodeBuilder(this, buildContext)
                    .build()
            }
            NavTarget.RoomDirectorySearch -> {
                Timber.tag("$TAG:Search").d("Setting up room directory search")
                roomDirectoryEntryPoint.nodeBuilder(this, buildContext)
                    .callback(object : RoomDirectoryEntryPoint.Callback {
                        override fun onResultClick(roomDescription: RoomDescription) {
                            Timber.tag("$TAG:Search").d("Search result clicked: ${roomDescription.roomId}")
                            backstack.push(
                                NavTarget.Room(
                                    roomIdOrAlias = roomDescription.roomId.toRoomIdOrAlias(),
                                    roomDescription = roomDescription,
                                    trigger = JoinedRoom.Trigger.RoomDirectory,
                                )
                            )
                        }
                    })
                    .build()
            }
            is NavTarget.IncomingShare -> {
                shareEntryPoint.nodeBuilder(this, buildContext)
                    .callback(object : ShareEntryPoint.Callback {
                        override fun onDone(roomIds: List<RoomId>) {
                            navigateUp()
                            if (roomIds.size == 1) {
                                val targetRoomId = roomIds.first()
                                safePush(NavTarget.Room(targetRoomId.toRoomIdOrAlias()))
                            }
                        }
                    })
                    .params(ShareEntryPoint.Params(intent = navTarget.intent))
                    .build()
            }
            is NavTarget.LogoutForNativeSlidingSyncMigrationNeeded -> {
                val callback = object : LogoutEntryPoint.Callback {
                    override fun onChangeRecoveryKeyClick() {
                        safePush(NavTarget.SecureBackup())
                    }
                }

                logoutEntryPoint.nodeBuilder(this, buildContext)
                    .callback(callback)
                    .build()
            }
            is NavTarget.IncomingVerificationRequest -> {
                incomingVerificationEntryPoint.nodeBuilder(this, buildContext)
                    .params(IncomingVerificationEntryPoint.Params(navTarget.data))
                    .callback(object : IncomingVerificationEntryPoint.Callback {
                        override fun onDone() {
                            backstack.pop()
                        }
                    })
                    .build()
            }
            is NavTarget.CallDetails -> {
                val callDetailsInput = io.element.android.features.call.impl.callshistory.details.CallDetailsInput(navTarget.call)
                val callback = object : io.element.android.features.call.impl.callshistory.details.CallDetailsScreen.Callback {
                    override fun onBackPressed() {
                        backstack.pop()
                    }
                    
                    override fun onRoomDetailsClick(roomId: RoomId) {
                        safePush(NavTarget.Room(roomId.toRoomIdOrAlias()))
                    }
                }
                
                createNode<io.element.android.features.call.impl.callshistory.details.CallDetailsScreen>(
                    buildContext, 
                    listOf(callDetailsInput, callback)
                )
            }
        }
    }

    suspend fun attachRoom(
        roomIdOrAlias: RoomIdOrAlias,
        serverNames: List<String> = emptyList(),
        trigger: JoinedRoom.Trigger? = null,
        eventId: EventId? = null,
        clearBackstack: Boolean,
    ) {
        waitForNavTargetAttached { navTarget ->
            navTarget is NavTarget.RoomList
        }
        attachChild<RoomFlowNode> {
            val roomNavTarget = NavTarget.Room(
                roomIdOrAlias = roomIdOrAlias,
                serverNames = serverNames,
                trigger = trigger,
                initialElement = RoomNavigationTarget.Messages(
                    focusedEventId = eventId
                )
            )
            backstack.accept(AttachRoomOperation(roomNavTarget, clearBackstack))
        }
    }

    suspend fun attachUser(userId: UserId) {
        waitForNavTargetAttached { navTarget ->
            navTarget is NavTarget.RoomList
        }
        attachChild<Node> {
            safePush(
                NavTarget.UserProfile(
                    userId = userId,
                )
            )
        }
    }

    internal suspend fun attachIncomingShare(intent: Intent) {
        waitForNavTargetAttached { navTarget ->
            navTarget is NavTarget.RoomList
        }
        attachChild<Node> {
            safePush(
                NavTarget.IncomingShare(intent)
            )
        }
    }

    @Composable
    override fun View(modifier: Modifier) {
        val navState by backstack.elements.collectAsState()
        val activeNavTarget = navState.lastOrNull { it.targetState == ACTIVE }?.key?.navTarget
        val isSettingsRootVisible by settingsRootVisible.collectAsState()
        val activity = LocalContext.current as? Activity

        BackHandler(
            enabled = activeNavTarget == NavTarget.RoomList
        ) {
            activity?.let {
                if (!it.isFinishing) {
                    it.finish()
                }
            }
        }

        LaunchedEffect(activeNavTarget) {
            if (activeNavTarget !is NavTarget.Settings) {
                settingsRootVisible.value = false
            }
        }

        val shouldShowBottomBar = when (activeNavTarget) {
            is NavTarget.RoomList -> true
            is NavTarget.Calls -> true
            is NavTarget.Settings -> isSettingsRootVisible
            else -> false
        }

        val currentRoute = when (activeNavTarget) {
            is NavTarget.RoomList -> BottomNavRoute.Home
            is NavTarget.Calls -> BottomNavRoute.Calls
            is NavTarget.Settings -> BottomNavRoute.Settings
            else -> BottomNavRoute.Home
        }

        Scaffold(
            bottomBar = {
                if (shouldShowBottomBar) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onRouteSelect = { route ->
                            if (currentRoute != route) {
                                when (route) {
                                    BottomNavRoute.Home -> safeReplace(NavTarget.RoomList)
                                    BottomNavRoute.Calls -> safeReplace(NavTarget.Calls)
                                    BottomNavRoute.Settings -> safeReplace(NavTarget.Settings())
                                }
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = modifier) {
                val ftueState by ftueService.state.collectAsState()
                BackstackView()
                if (ftueState is FtueState.Complete) {
                    PermanentChild(permanentNavModel = permanentNavModel, navTarget = NavTarget.LoggedInPermanent)
                }
            }
        }
    }

    @ContributesNode(AppScope::class)
    class PlaceholderNode @AssistedInject constructor(
        @Assisted buildContext: BuildContext,
        @Assisted plugins: List<Plugin>,
    ) : Node(buildContext, plugins = plugins)

    private fun safePush(navTarget: NavTarget) {
        if (backstack.elements.value.none { it.key.navTarget == navTarget }) {
            Timber.tag(TAG).d("Pushing navigation target: $navTarget")
            backstack.push(navTarget)
        } else {
            Timber.tag(TAG).d("Navigation target already exists, ignoring: $navTarget")
        }
    }
    
    private fun safeReplace(navTarget: NavTarget) {
        // This method properly handles bottom navigation by replacing the current screen
        Timber.tag(TAG).d("Replacing navigation target with: $navTarget")
        backstack.replace(navTarget)
    }

    private fun safeSingleTop(navTarget: NavTarget) {
        if (backstack.elements.value.none { it.key.navTarget == navTarget }) {
            Timber.tag(TAG).d("Single top navigation target: $navTarget")
            backstack.singleTop(navTarget)
        } else {
            Timber.tag(TAG).d("Navigation target already exists, ignoring singleTop: $navTarget")
        }
    }

    /**
     * Safely handles permalink navigation
     */
    private fun handlePermalink(data: PermalinkData, pushToBackstack: Boolean) {
        Timber.tag(TAG).d("Handling permalink: $data, pushToBackstack: $pushToBackstack")
        if (!pushToBackstack) return
        
        when (data) {
            is PermalinkData.RoomLink -> {
                val navTarget = NavTarget.Room(
                    roomIdOrAlias = data.roomIdOrAlias,
                    serverNames = data.viaParameters
                )
                safePush(navTarget)
            }
            is PermalinkData.UserLink -> {
                val navTarget = NavTarget.UserProfile(userId = data.userId)
                safePush(navTarget)
            }
            else -> {
                Timber.w("Unsupported permalink type: $data")
            }
        }
    }

    internal fun handleMatrixToLink(target: PermalinkData) {
        when (target) {
            is PermalinkData.RoomLink -> {
                safePush(NavTarget.Room(
                    roomIdOrAlias = target.roomIdOrAlias,
                    serverNames = target.viaParameters
                ))
            }
            else -> {
                handlePermalink(target, true)
            }
        }
    }
}

@Parcelize
private class AttachRoomOperation(
    val roomTarget: LoggedInFlowNode.NavTarget.Room,
    val clearBackstack: Boolean,
) : BackStackOperation<LoggedInFlowNode.NavTarget> {
    override fun isApplicable(elements: NavElements<LoggedInFlowNode.NavTarget, BackStack.State>) = true

    override fun invoke(elements: BackStackElements<LoggedInFlowNode.NavTarget>): BackStackElements<LoggedInFlowNode.NavTarget> {
        return if (clearBackstack) {
            elements.mapNotNull { element ->
                if (element.key.navTarget == LoggedInFlowNode.NavTarget.RoomList) {
                    element.transitionTo(STASHED, this)
                } else {
                    null
                }
            } + BackStackElement(
                key = NavKey(roomTarget),
                fromState = CREATED,
                targetState = ACTIVE,
                operation = this
            )
        } else {
            Push<LoggedInFlowNode.NavTarget>(roomTarget).invoke(elements)
        }
    }
}
