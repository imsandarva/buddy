package com.sandarva.kotlinapps.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/** Compact live chrome — voice only, no captions. The rest of the screen stays tappable. */
@Composable
fun LiveBuddyBar(onStop: () -> Unit, onTypeInstead: () -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "liveListen").animateFloat(
        initialValue = 0.42f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = BuddyMotion.EnterEasing), RepeatMode.Reverse),
        label = "livePulse"
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(BuddyColors.Ink)
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 12.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.padding(bottom = 12.dp).width(48.dp).height(4.dp).background(BuddyColors.Honey.copy(alpha = 0.55f), RoundedCornerShape(50)))
        Box(Modifier.size(12.dp).graphicsLayer { alpha = pulse; scaleX = 0.86f + 0.14f * pulse; scaleY = 0.86f + 0.14f * pulse }.background(BuddyColors.Honey, CircleShape))
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.live_title), style = MaterialTheme.typography.titleLarge, color = BuddyColors.Bone, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.live_body), style = MaterialTheme.typography.bodyLarge, color = BuddyColors.BoneMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        QuietTextAction(stringResource(R.string.live_thats_all), onStop)
        Spacer(Modifier.height(10.dp))
        QuietTextAction(stringResource(R.string.live_type_instead), onTypeInstead)
    }
}
