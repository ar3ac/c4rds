package io.github.ar3ac.c4rds.ui.detail

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Shortcut
import android.content.Intent
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.ar3ac.c4rds.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ar3ac.c4rds.utils.BarcodeGenerator
import io.github.ar3ac.c4rds.utils.BrandHelper
import io.github.ar3ac.c4rds.utils.KeepScreenBright
import io.github.ar3ac.c4rds.utils.ShortcutHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    viewModel: CardDetailViewModel,
    onEditClick: (cardId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    KeepScreenBright()

    val cardState by viewModel.card.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullscreenBarcode by remember { mutableStateOf(false) }
    var isBarcodeRotated by remember { mutableStateOf(false) }

    val currentCard = cardState
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val copyToClipboard = {
        currentCard?.let { card ->
            clipboardManager.setText(AnnotatedString(card.barcodeValue))
            Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentCard?.name ?: stringResource(R.string.card_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (currentCard != null) {
                        IconButton(onClick = { ShortcutHelper.createCardShortcut(context, currentCard) }) {
                            Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = stringResource(R.string.menu_add_to_home))
                        }
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                val safeName = android.net.Uri.encode(currentCard.name)
                                val safeBarcode = android.net.Uri.encode(currentCard.barcodeValue)
                                val safeFormat = android.net.Uri.encode(currentCard.barcodeFormat)
                                val safeOwner = android.net.Uri.encode(currentCard.owner)
                                val shareLink = "https://ar3ac.github.io/c4rds-share/create/?name=$safeName&barcode=$safeBarcode&format=$safeFormat&owner=$safeOwner"
                                
                                val text = context.getString(R.string.share_card_text, currentCard.name, currentCard.barcodeValue, shareLink)
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { copyToClipboard() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                        }
                        IconButton(onClick = {
                            viewModel.toggleFavorite {
                                Toast.makeText(context, context.getString(R.string.favorite_max_limit), Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = if (currentCard.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = stringResource(R.string.favorite_toggle),
                                tint = if (currentCard.favorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onEditClick(currentCard.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (currentCard == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val is2D = currentCard.barcodeFormat.contains("QR") || currentCard.barcodeFormat.contains("MATRIX")
            val barcodeBitmap = remember(currentCard.barcodeValue, currentCard.barcodeFormat) {
                BarcodeGenerator.generateBarcode(
                    value = currentCard.barcodeValue,
                    formatName = currentCard.barcodeFormat,
                    width = 1200,
                    height = if (is2D) 1200 else 480
                )
            }
            val brandInfo = remember(currentCard.name, currentCard.customLogoPath) { BrandHelper.getBrandInfo(currentCard.name, currentCard.customLogoPath) }

            // Dynamic margins based on display width: on zoomed displays (screenWidthDp <= 360),
            // adapt horizontal padding so the barcode container gets maximum physical display width.
            val configuration = LocalConfiguration.current
            val isZoomedOrSmallScreen = configuration.screenWidthDp <= 360
            val screenHorizontalPadding = if (isZoomedOrSmallScreen) 8.dp else 16.dp
            val cardHorizontalPadding = if (isZoomedOrSmallScreen) 8.dp else 14.dp

            val barcodePainter = remember(barcodeBitmap) {
                barcodeBitmap?.let {
                    BitmapPainter(
                        image = it.asImageBitmap(),
                        filterQuality = FilterQuality.None
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = screenHorizontalPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Card contenente il Codice a Barre gigante (Spostata in alto)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = cardHorizontalPadding,
                                vertical = 14.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (barcodePainter != null) {
                            Image(
                                painter = barcodePainter,
                                contentDescription = "Codice a barre ${currentCard.name}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        min = if (is2D) 150.dp else 100.dp,
                                        max = if (is2D) 220.dp else 160.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showFullscreenBarcode = true }
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Hint badge to tap for full screen
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showFullscreenBarcode = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.barcode_fullscreen_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Impossibile generare il barcode.\nVerifica il formato e il valore.",
                                color = Color.Red,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Valore numerico del codice interattivo per la copia
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFA6C8FF).copy(alpha = 0.25f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { copyToClipboard() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = currentCard.barcodeValue,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 22.sp,
                                        letterSpacing = 1.5.sp
                                    ),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Formato del codice a barre
                Text(
                    text = stringResource(R.string.format_label, currentCard.barcodeFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Logo Brand Ufficiale prominente
                if (brandInfo.iconResId != null || brandInfo.localFilePath != null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        tonalElevation = 4.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .widthIn(min = 140.dp, max = 220.dp)
                            .height(105.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            io.github.ar3ac.c4rds.ui.components.BrandIcon(
                                brand = brandInfo,
                                contentDescription = currentCard.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Nome Tessera / Supermercato
                Text(
                    text = currentCard.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Nickname / Note
                if (currentCard.owner.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentCard.owner,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDeleteDialog && currentCard != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_card_title)) },
                text = { Text(stringResource(R.string.delete_card_message, currentCard.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCard(onDeleted = onBackClick)
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showFullscreenBarcode && currentCard != null) {
            val is2D = currentCard.barcodeFormat.contains("QR") || currentCard.barcodeFormat.contains("MATRIX")
            val fsBitmap = remember(currentCard.barcodeValue, currentCard.barcodeFormat) {
                BarcodeGenerator.generateBarcode(
                    value = currentCard.barcodeValue,
                    formatName = currentCard.barcodeFormat,
                    width = 1600,
                    height = if (is2D) 1600 else 600
                )
            }
            val fsPainter = remember(fsBitmap) {
                fsBitmap?.let {
                    BitmapPainter(
                        image = it.asImageBitmap(),
                        filterQuality = FilterQuality.None
                    )
                }
            }

            Dialog(
                onDismissRequest = {
                    showFullscreenBarcode = false
                    isBarcodeRotated = false
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top action bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    showFullscreenBarcode = false
                                    isBarcodeRotated = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = Color.Black
                                )
                            }

                            Text(
                                text = currentCard.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            if (!is2D) {
                                IconButton(
                                    onClick = { isBarcodeRotated = !isBarcodeRotated }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = stringResource(R.string.barcode_rotate),
                                        tint = if (isBarcodeRotated) MaterialTheme.colorScheme.primary else Color.Black
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Barcode in Center
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!is2D) isBarcodeRotated = !isBarcodeRotated
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val boxWidth = maxWidth
                            val boxHeight = maxHeight

                            if (fsPainter != null) {
                                if (isBarcodeRotated && !is2D) {
                                    Image(
                                        painter = fsPainter,
                                        contentDescription = currentCard.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(width = boxHeight * 0.92f, height = boxWidth * 0.90f)
                                            .graphicsLayer { rotationZ = 90f }
                                    )
                                } else {
                                    Image(
                                        painter = fsPainter,
                                        contentDescription = currentCard.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth(0.96f)
                                            .fillMaxHeight(if (is2D) 0.85f else 0.70f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Barcode numeric value & Owner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentCard.barcodeValue,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 26.sp,
                                    letterSpacing = 2.sp
                                ),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )

                            if (currentCard.owner.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentCard.owner,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (!is2D) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.barcode_rotate_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
