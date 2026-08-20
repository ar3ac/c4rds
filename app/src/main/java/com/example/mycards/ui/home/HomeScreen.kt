package com.example.mycards.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.mycards.R
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.mycards.data.model.CardEntity
import com.example.mycards.utils.BrandHelper
import com.example.mycards.utils.ShortcutHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCardClick: (cardId: Long) -> Unit,
    onScanClick: () -> Unit,
    onAddManualClick: () -> Unit,
    onEditClick: (cardId: Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val allCards by viewModel.cards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredCards = remember(allCards, viewModel.searchQuery) {
        val query = viewModel.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allCards
        } else {
            allCards.filter {
                it.name.lowercase().contains(query) ||
                        it.owner.lowercase().contains(query) ||
                        it.barcodeValue.lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_round),
                            contentDescription = "c4rds Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Le tue tessere",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "c4rds Wallet",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Barra di ricerca rapida
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text("Cerca tessera o nickname...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Pulisci")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Aggiungi carta") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (filteredCards.isEmpty()) {
                EmptyStateView(
                    isSearch = viewModel.searchQuery.isNotEmpty(),
                    onAddClick = { showAddDialog = true }
                )
            } else {
                var list by remember { mutableStateOf(filteredCards) }
                
                LaunchedEffect(filteredCards) {
                    list = filteredCards
                }

                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    list = list.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                }

                // Quando l'utente smette di trascinare, salviamo l'ordine
                LaunchedEffect(reorderableState.isAnyItemDragging) {
                    if (!reorderableState.isAnyItemDragging && list != filteredCards) {
                        viewModel.updateCardsOrder(list)
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = list,
                        key = { it.id }
                    ) { card ->
                        ReorderableItem(reorderableState, key = card.id) {
                            val interactionSource = remember { MutableInteractionSource() }
                            
                            Box(modifier = Modifier.longPressDraggableHandle(interactionSource = interactionSource)) {
                                CardItemRow(
                                    card = card,
                                    onClick = { onCardClick(card.id) },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(card) {
                                            Toast.makeText(context, "Puoi avere al massimo 3 tessere preferite per i comandi rapidi.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onEdit = { onEditClick(card.id) },
                                    onDelete = { viewModel.deleteCard(card) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCardOptionsDialog(
                onDismiss = { showAddDialog = false },
                onScanSelect = {
                    showAddDialog = false
                    onScanClick()
                },
                onManualSelect = {
                    showAddDialog = false
                    onAddManualClick()
                }
            )
        }
    }
}

@Composable
fun CardItemRow(
    card: CardEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val brandInfo = remember(card.name) { BrandHelper.getBrandInfo(card.name) }
    val avatarColor = remember(card.name) { getBadgeColor(card.name) }
    val initial = remember(card.name) {
        if (card.name.isNotBlank()) card.name.trim().take(1).uppercase() else "?"
    }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona Logo Brand Ufficiale o Badge Iniziali (ora in Card rettangolare su sfondo bianco)
            if (brandInfo.iconResId != null || brandInfo.localFilePath != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .width(64.dp)
                        .height(42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        com.example.mycards.ui.components.BrandIcon(
                            brand = brandInfo,
                            contentDescription = card.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = avatarColor,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .width(64.dp)
                        .height(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Badge Nickname / Note (es. "Luca")
                    if (card.owner.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = card.owner,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Valore del codice a barre
                Text(
                    text = card.barcodeValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Trascina per riordinare",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (card.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Preferita",
                    tint = if (card.favorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Altre opzioni")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Aggiungi alla Home") },
                        onClick = {
                            menuExpanded = false
                            ShortcutHelper.createCardShortcut(context, card)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copia codice") },
                        onClick = {
                            menuExpanded = false
                            clipboardManager.setText(AnnotatedString(card.barcodeValue))
                            Toast.makeText(context, "Codice copiato negli appunti!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Condividi tessera") },
                        onClick = {
                            menuExpanded = false
                            val safeName = android.net.Uri.encode(card.name)
                            val safeBarcode = android.net.Uri.encode(card.barcodeValue)
                            val safeFormat = android.net.Uri.encode(card.barcodeFormat)
                            val safeOwner = android.net.Uri.encode(card.owner)
                            val shareLink = "https://ar3ac.github.io/c4rds-share/create/?name=$safeName&barcode=$safeBarcode&format=$safeFormat&owner=$safeOwner"
                            
                            val text = "Ecco la mia tessera: *${card.name}*\nCodice: ${card.barcodeValue}\n\n$shareLink"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Condividi tessera tramite..."))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Modifica") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isSearch: Boolean,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearch) "Nessuna tessera trovata" else "Nessuna tessera salvata",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearch)
                "Prova a modificare i termini di ricerca."
            else
                "Aggiungi le tue tessere fedeltà scansionando il barcode o inserendolo manualmente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isSearch) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi prima tessera")
            }
        }
    }
}

@Composable
fun AddCardOptionsDialog(
    onDismiss: () -> Unit,
    onScanSelect: () -> Unit,
    onManualSelect: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi nuova tessera") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    onClick = onScanSelect,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Scansiona con Fotocamera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Inquadra il codice a barre della tua carta",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Card(
                    onClick = onManualSelect,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Inserimento Manuale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Digita direttamente nome e codice",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun getBadgeColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE53935), // Red
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFFB8C00), // Orange
        Color(0xFF8E24AA), // Purple
        Color(0xFF00ACC1), // Cyan
        Color(0xFFD81B60), // Pink
        Color(0xFF3949AB)  // Indigo
    )
    val hash = kotlin.math.abs(name.hashCode())
    return colors[hash % colors.size]
}
