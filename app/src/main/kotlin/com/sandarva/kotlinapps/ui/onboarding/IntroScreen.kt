package com.sandarva.kotlinapps.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.ui.components.BuddyMark
import com.sandarva.kotlinapps.ui.components.OverlayGlyph
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.motion.FadeSlideIn
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import kotlinx.coroutines.launch

private data class Beat(val headline: String, val body: String)

private val BEATS = listOf(
    Beat("I talk with you.", "Say what's on your mind — I'm always ready to listen."),
    Beat("I can act on your\nscreen, too.", "A second pair of hands for the small, tedious stuff."),
    Beat("You're always\nin control.", "One tap, anytime, and I stop right where I am.")
)

/** Two to three wordless-leaning cards, one idea each — never a screenshot of buddy's own UI. */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { BEATS.size })
    val scope = rememberCoroutineScope()
    val onLastPage = pagerState.currentPage == BEATS.lastIndex
    OnboardingScaffold(
        bottom = {
            Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.Center) {
                repeat(BEATS.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) BuddyColors.Violet else BuddyColors.Line)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuietTextAction("Skip", onFinished)
                QuietTextAction(if (onLastPage) "Continue" else "Next", onClick = {
                    if (onLastPage) onFinished() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                })
            }
        }
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page -> IntroCard(BEATS[page], page) }
    }
}

@Composable
private fun IntroCard(beat: Beat, index: Int) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        FadeSlideIn(0) {
            when (index) {
                0 -> BuddyMark(alive = true)
                1 -> OverlayGlyph()
                else -> BuddyMark(alive = false)
            }
        }
        Spacer(Modifier.height(36.dp))
        FadeSlideIn(80) {
            Text(beat.headline, style = MaterialTheme.typography.displayLarge, color = BuddyColors.Ink, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        FadeSlideIn(140) {
            Text(beat.body, style = MaterialTheme.typography.bodyLarge, color = BuddyColors.InkMuted, textAlign = TextAlign.Center)
        }
    }
}
