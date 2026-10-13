package io.github.ar3ac.c4rds.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.ar3ac.c4rds.utils.BrandInfo
import java.io.File

@Composable
fun BrandIcon(
    brand: BrandInfo,
    modifier: Modifier = Modifier,
    contentDescription: String? = brand.brandName,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (brand.iconResId != null) {
        Image(
            painter = painterResource(id = brand.iconResId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (brand.localFilePath != null) {
        val bitmap = remember(brand.localFilePath) {
            val file = File(brand.localFilePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        }
        
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}
