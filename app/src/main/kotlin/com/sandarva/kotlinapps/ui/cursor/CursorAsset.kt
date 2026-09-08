package com.sandarva.kotlinapps.ui.cursor

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/** Loads the buddy cursor PNG once per composition tree. */
@Composable
fun rememberBuddyCursorBitmap(): ImageBitmap {
    val context = LocalContext.current
    return remember {
        context.assets.open(CursorGeometry.ASSET_PATH).use { stream ->
            requireNotNull(BitmapFactory.decodeStream(stream)).asImageBitmap()
        }
    }
}
