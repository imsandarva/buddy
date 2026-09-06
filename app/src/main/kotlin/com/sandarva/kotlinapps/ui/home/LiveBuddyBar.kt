package com.sandarva.kotlinapps.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors
import com.sandarva.kotlinapps.ui.theme.BuddyMotion

/** Floating live pill — small enough that bottom buttons stay reachable. */
@Composable
fun LiveBuddyBar(onStop: () -> Unit, onTypeInstead: () -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "liveListen").animateFloat(
        initialValue = 0.42f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = BuddyMotion.EnterEasing), RepeatMode.Reverse),
        label = "livePulse"
    )
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .wrapContentSize()
            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = BuddyColors.Ink.copy(alpha = 0.28f), spotColor = BuddyColors.Ink.copy(alpha = 0.22f))
            .clip(RoundedCornerShape(28.dp))
            .background(BuddyColors.Ink)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse; scaleX = 0.86f + 0.14f * pulse; scaleY = 0.86f + 0.14f * pulse }.background(BuddyColors.Honey, CircleShape))
        Text(
            stringResource(R.string.live_title),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp),
            color = BuddyColors.Bone,
            maxLines = 1
        )
        QuietTextAction(stringResource(R.string.live_thats_all), onStop)
        QuietTextAction(stringResource(R.string.live_type_instead), onTypeInstead)
    }
}
