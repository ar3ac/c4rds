package com.example.mycards.ui.detail

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Shortcut
import android.content.Intent
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycards.utils.BarcodeGenerator
import com.example.mycards.utils.BrandHelper
import com.example.mycards.utils.KeepScreenBright
import com.example.mycards.utils.ShortcutHelper

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

    val currentCard = cardState
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val copyToClipboard = {
        currentCard?.let { card ->
            clipboardManager.setText(AnnotatedString(card.barcodeValue))
            Toast.makeText(context, "Codice copiato negli appunti!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentCard?.name ?: "Dettaglio Tessera") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (currentCard != null) {
                        IconButton(onClick = { ShortcutHelper.createCardShortcut(context, currentCard) }) {
                            Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = "Aggiungi alla Home")
                        }
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                val safeName = android.net.Uri.encode(currentCard.name)
                                val safeBarcode = android.net.Uri.encode(currentCard.barcodeValue)
                                val safeFormat = android.net.Uri.encode(currentCard.barcodeFormat)
                                val safeOwner = android.net.Uri.encode(currentCard.owner)
                                val shareLink = "https://ar3ac.github.io/c4rds-share/create/?name=$safeName&barcode=$safeBarcode&format=$safeFormat&owner=$safeOwner"
                                
                                val text = "Ecco la mia tessera: *${currentCard.name}*\nCodice: ${currentCard.barcodeValue}\n\n$shareLink"
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Condividi tessera tramite...")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Condividi Tessera")
                        }
                        IconButton(onClick = { copyToClipboard() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copia Codice")
                        }
                        IconButton(onClick = {
                            viewModel.toggleFavorite {
                                Toast.makeText(context, "Puoi avere al massimo 3 tessere preferite per i comandi rapidi.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = if (currentCard.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Preferito",
                                tint = if (currentCard.favorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onEditClick(currentCard.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Elimina",
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
            val barcodeBitmap = remember(currentCard.barcodeValue, currentCard.barcodeFormat) {
                BarcodeGenerator.generateBarcode(
                    value = currentCard.barcodeValue,
                    formatName = currentCard.barcodeFormat,
                    width = 1200,
                    height = 600
                )
            }
            val brandInfo = remember(currentCard.name) { BrandHelper.getBrandInfo(currentCard.name) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Logo Brand Ufficiale prominente con card elevata
                if (brandInfo.iconResId != null) {
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
                            Image(
                                painter = painterResource(id = brandInfo.iconResId),
                                contentDescription = currentCard.name,
                                contentScale = ContentScale.Fit,
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

                // Nickname / Note (es. solo "Luca", senza prefissi fisso, in font ben visibile)
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

                Spacer(modifier = Modifier.height(28.dp))

                // Card contenente il Codice a Barre gigante
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (barcodeBitmap != null) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "Codice a barre ${currentCard.name}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 320.dp)
                                    .padding(vertical = 12.dp)
                            )
                        } else {
                            Text(
                                text = "Impossibile generare il barcode.\nVerifica il formato e il valore.",
                                color = Color.Red,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                    contentDescription = "Copia codice negli appunti",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Formato del codice a barre posizionato SOTTO in piccolo
                Text(
                    text = "Formato: ${currentCard.barcodeFormat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDeleteDialog && currentCard != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminare la tessera?") },
                text = { Text("Sei sicuro di voler eliminare permanentemente \"${currentCard.name}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCard(onDeleted = onBackClick)
                        }
                    ) {
                        Text("Elimina", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}
