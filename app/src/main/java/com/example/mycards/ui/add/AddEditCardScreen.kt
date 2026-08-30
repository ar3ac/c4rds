package com.example.mycards.ui.add

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.example.mycards.utils.BarcodeGenerator
import com.example.mycards.utils.BrandHelper
import com.example.mycards.utils.BrandInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    viewModel: AddEditViewModel,
    onSaveSuccess: (cardId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    var showBrandSelectorDialog by remember { mutableStateOf(false) }
    var showWebLogoSearchDialog by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Rileva automaticamente se il nome attualmente digitato corrisponde a un logo conosciuto
    val currentBrandInfo = remember(viewModel.name, viewModel.customLogoPath) { 
        BrandHelper.getBrandInfo(viewModel.name, viewModel.customLogoPath) 
    }
    
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.copyUriAndSetCustomLogo(context, uri)
            showWebLogoSearchDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "Modifica Tessera" else "Nuova Tessera") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Campo di Testo Pulito per Nome Tessera + Tasto Lente a Destra per aprire il catalogo marchi
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = {
                    viewModel.name = it
                    viewModel.errorMessage = null
                },
                label = { Text("Nome Tessera / Supermercato") },
                placeholder = { Text("es. Esselunga, Iperal, Conad...") },
                leadingIcon = if (currentBrandInfo.iconResId != null || currentBrandInfo.localFilePath != null) {
                    {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showWebLogoSearchDialog = true }
                                .padding(4.dp)
                        ) {
                            com.example.mycards.ui.components.BrandIcon(
                                brand = currentBrandInfo,
                                contentDescription = "Cambia logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                } else {
                    {
                        IconButton(onClick = { showWebLogoSearchDialog = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Cerca logo sul web")
                        }
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { showBrandSelectorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cerca o seleziona tra i marchi principali"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // Nickname / Note
            OutlinedTextField(
                value = viewModel.owner,
                onValueChange = { viewModel.owner = it },
                label = { Text("Nickname / Note") },
                placeholder = { Text("es. Nome, Personale, Aziendale") },
                leadingIcon = {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // Valore Barcode
            OutlinedTextField(
                value = viewModel.barcodeValue,
                onValueChange = {
                    viewModel.onBarcodeValueChanged(it)
                },
                label = { Text("Valore Codice a Barre / QR") },
                trailingIcon = {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )

            // Formato Barcode Dropdown Selector
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = viewModel.barcodeFormat,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Formato Barcode (Rilevato/Selezionato)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    BarcodeGenerator.supportedFormats.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format) },
                            onClick = {
                                viewModel.onFormatSelected(format)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Opzione Preferita Switch
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.favorite = !viewModel.favorite }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (viewModel.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (viewModel.favorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Preferiti",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Mostra in cima e nelle scorciatoie rapide",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = viewModel.favorite,
                        onCheckedChange = { viewModel.favorite = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pulsante Salva
            Button(
                onClick = {
                    viewModel.saveCard { id ->
                        onSaveSuccess(id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (viewModel.isEditMode) "Aggiorna Tessera" else "Salva Tessera",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Finestra di dialogo per cercare/selezionare rapidamente un supermercato con logo
        if (showBrandSelectorDialog) {
            BrandSelectorDialog(
                onDismiss = { showBrandSelectorDialog = false },
                onBrandSelected = { selectedBrand ->
                    viewModel.name = selectedBrand.brandName
                    showBrandSelectorDialog = false
                }
            )
        }

        if (showWebLogoSearchDialog) {
            WebLogoSearchDialog(
                viewModel = viewModel,
                onDismiss = { showWebLogoSearchDialog = false },
                onLogoSelected = { logoUrl ->
                    viewModel.downloadAndSetCustomLogo(context, logoUrl)
                    showWebLogoSearchDialog = false
                },
                onGalleryClick = { galleryLauncher.launch("image/*") }
            )
        }
    }
}

@Composable
fun BrandSelectorDialog(
    onDismiss: () -> Unit,
    onBrandSelected: (BrandInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(searchQuery) {
        val all = BrandHelper.getAllBrands()
        if (searchQuery.isBlank()) {
            all
        } else {
            all.filter {
                it.brandName.lowercase().contains(searchQuery.trim().lowercase())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleziona Supermercato / Brand",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtra brand...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Pulisci")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredList) { brand ->
                        Surface(
                            onClick = { onBrandSelected(brand) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                if (brand.iconResId != null || brand.localFilePath != null) {
                                    com.example.mycards.ui.components.BrandIcon(
                                        brand = brand,
                                        contentDescription = brand.brandName,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = brand.brandName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLogoSearchDialog(
    viewModel: AddEditViewModel,
    onDismiss: () -> Unit,
    onLogoSelected: (String) -> Unit,
    onGalleryClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(viewModel.name) }

    LaunchedEffect(Unit) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchWebLogos(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cerca Logo Web",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Button(
                    onClick = onGalleryClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scegli un'immagine dalla Galleria")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Oppure cerca sul Web (Favicon):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Nome negozio o dominio...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Pulisci")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.searchWebLogos(searchQuery) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = searchQuery.isNotBlank() && !viewModel.isSearchingLogo
                ) {
                    Text(if (viewModel.isSearchingLogo) "Ricerca in corso..." else "Cerca")
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.isSearchingLogo) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (viewModel.logoSearchResults.isEmpty()) {
                    Text(
                        "Nessun risultato. Prova a inserire il nome esatto del negozio (es. 'esselunga').",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.logoSearchResults) { result ->
                            Surface(
                                onClick = { onLogoSelected(result.logoUrl) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    AsyncImage(
                                        model = result.logoUrl,
                                        contentDescription = result.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = result.domain,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
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
