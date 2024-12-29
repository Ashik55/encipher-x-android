/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.onboarding.impl

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtom
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtomSize
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.pages.OnBoardingPage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun OnBoardingView(
    state: OnBoardingState,
    onSignIn: () -> Unit,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    autoProgressDuration: Long = 3000,
    enableAutoProgress: Boolean = true
) {
    val swipeableState = rememberSwipeableState(initialValue = state.currentPage)
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    val anchors = (0 until 4).associateBy { -screenWidthPx * it }

    LaunchedEffect(enableAutoProgress, swipeableState) {
        if (enableAutoProgress) {
            while (true) {
                delay(autoProgressDuration)
                val nextPage = (swipeableState.currentValue + 1) % 4
                swipeableState.animateTo(nextPage)
                onPageChange(nextPage)
            }
        }
    }

    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue != state.currentPage) {
            onPageChange(swipeableState.currentValue)
        }
    }

    OnBoardingPage(
        modifier = modifier.swipeable(
            state = swipeableState,
            anchors = anchors,
            thresholds = { _, _ -> FractionalThreshold(0.3f) },
            orientation = Orientation.Horizontal
        ),
        content = { OnBoardingContent(state) },
        footer = {
            Column {
                PageIndicator(totalPages = 4, currentPage = state.currentPage, onPageChange = onPageChange)
                OnBoardingButtons(state = state, onSignIn = onSignIn)
            }
        }
    )
}

@Composable
private fun OnBoardingContent(state: OnBoardingState) {
    val pages = listOf(
        OnboardingPageContent(
            imageRes = io.element.android.libraries.designsystem.R.drawable.onboarding1,
            title = "Secure your voice & own your story",
            description = "Confidential and private communication, as\nsecure as a personal conversation at home.",
            imageSize = Modifier.size(width = 250.dp, height = 220.dp)
        ),
        OnboardingPageContent(
            imageRes = io.element.android.libraries.designsystem.R.drawable.onboarding2,
            title = "Stay in charge",
            description = "Decide where your conversations live, putting\nyou in charge of your privacy.",
            imageSize = Modifier.size(width = 300.dp, height = 290.dp)
        ),
        OnboardingPageContent(
            imageRes = io.element.android.libraries.designsystem.R.drawable.onboarding3,
            title = "Private and Safe Messaging",
            description = "Fully encrypted, phone-free messaging with\nno ads or data collection.",
            imageSize = Modifier.size(width = 275.dp, height = 265.dp)
        ),
        OnboardingPageContent(
            imageRes = io.element.android.libraries.designsystem.R.drawable.onboarding4,
            title = "Focused teamwork & redefined",
            description = "Perfect for the workplace, Encipher is the\nchoice of trusted secure organizations.",
            imageSize = Modifier.size(width = 325.dp, height = 315.dp)
        )
    )

    pages[state.currentPage].let {
        OnboardingPage(
            imageRes = it.imageRes,
            title = it.title,
            description = it.description,
            imageSize = it.imageSize
        )
    }
}

@Composable
private fun OnboardingPage(
    imageRes: Int,
    title: String,
    description: String,
    imageSize: Modifier = Modifier.size(width = 300.dp, height = 290.dp)
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .then(imageSize),
                contentScale = ContentScale.FillBounds
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = ElementTheme.materialColors.primary,
                style = ElementTheme.typography.fontHeadingLgBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = description,
                color = ElementTheme.materialColors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

// Data class updated to include imageSize
data class OnboardingPageContent(
    val imageRes: Int,
    val title: String,
    val description: String,
    val imageSize: Modifier = Modifier.size(width = 300.dp, height = 290.dp)
)

@Composable
private fun OnBoardingButtons(state: OnBoardingState, onSignIn: () -> Unit) {
    val buttonTextRes = if (state.canLoginWithQrCode || state.canCreateAccount) {
        R.string.screen_onboarding_sign_in_manually
    } else {
        CommonStrings.action_continue
    }
    Button(
        text = stringResource(id = buttonTextRes),
        onClick = onSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .testTag(TestTags.onBoardingSignIn)
    )
}

@Composable
private fun PageIndicator(totalPages: Int, currentPage: Int, onPageChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalPages) { page ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (currentPage == page)
                            ElementTheme.materialColors.primary
                        else
                            ElementTheme.materialColors.secondary.copy(alpha = 0.3f)
                    )
                    .clickable { onPageChange(page) }
            )
        }
    }
}


@PreviewsDayNight
@Composable
internal fun OnBoardingViewPreview(
    @PreviewParameter(OnBoardingStateProvider::class) state: OnBoardingState
) = ElementPreview {
    OnBoardingView(
        state = state,
        onSignIn = {},
        onPageChange = {}
    )
}
