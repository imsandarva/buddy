package com.sandarva.kotlinapps.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.ui.components.QuietTextAction
import com.sandarva.kotlinapps.ui.theme.BuddyColors

/** Compact live chrome — not full-screen, so taps can still reach the app underneath. */
@Composable
fun LiveBuddyBar(note: String?, onStop: () -> Unit, onTypeInstead: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(stringResource(R.string.live_title), style = MaterialTheme.typography.titleLarge, color = BuddyColors.Bone, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(note ?: stringResource(R.string.live_body), style = MaterialTheme.typography.bodyLarge, color = if (note != null) BuddyColors.Honey else BuddyColors.BoneMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        QuietTextAction(stringResource(R.string.live_thats_all), onStop)
        Spacer(Modifier.height(10.dp))
        QuietTextAction(stringResource(R.string.live_type_instead), onTypeInstead)
    }
}
