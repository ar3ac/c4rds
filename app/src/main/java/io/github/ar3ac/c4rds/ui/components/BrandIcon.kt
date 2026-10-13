package io.github.ar3ac.c4rds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
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
        AsyncImage(
            model = File(brand.localFilePath),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
