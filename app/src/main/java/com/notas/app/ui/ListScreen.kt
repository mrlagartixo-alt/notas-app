package com.notas.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notas.app.MainViewModel
import com.notas.app.data.Category
import com.notas.app.data.Note
import com.notas.app.util.Ocr
import com.notas.app.util.formatDate
import com.notas.app.util.notesToText
import com.notas.app.util.shareText
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListScreen(
    vm: MainViewModel,
    onOpenNote: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val notes by vm.notes.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    var selected by remember { mutableStateOf(setOf<Long>()) }
    var searching by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val searchFocus = remember { FocusRequester() }
    val catById = remember(categories) { categories.associateBy { it.id } }
    val shown = remember(notes, filter, query) {
        val q = query.trim()
        notes.filter { n ->
            (filter == null || n.categoryId == filter) &&
                (q.isEmpty() || n.title.contains(q, ignoreCase = true) || n.content.contains(q, ignoreCase = true))
        }
    }
    val selecting = selected.isNotEmpty()

    BackHandler(enabled = selecting || searching || fabOpen) {
        when {
            selecting -> selected = emptySet()
            fabOpen -> fabOpen = false
            searching -> {
                searching = false
                vm.query.value = ""
            }
        }
    }

    LaunchedEffect(searching) {
        if (searching) runCatching { searchFocus.requestFocus() }
    }

    // ---- Foto / print -> texto ----
    fun runOcr(uri: Uri) {
        scope.launch {
            busy = true
            val result = Ocr.read(context, uri)
            busy = false
            val text = result.getOrNull()
            when {
                text == null -> snackbar.showSnackbar("Não consegui ler essa imagem.")
                text.isBlank() -> snackbar.showSnackbar("Não encontrei texto na imagem.")
                else -> onOpenNote(vm.createFromText(text))
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runOcr(uri)
    }
    var pendingPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingPhoto
        if (ok && uri != null) runOcr(uri)
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("scan_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingPhoto = uri
        takePhoto.launch(uri)
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                when {
                    selecting -> TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        title = { Text(if (selected.size == 1) "1 selecionada" else "${selected.size} selecionadas") },
                        navigationIcon = {
                            IconButton(onClick = { selected = emptySet() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancelar seleção")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selected = if (shown.all { it.id in selected }) emptySet() else shown.map { it.id }.toSet()
                            }) { Icon(Icons.Filled.SelectAll, contentDescription = "Selecionar todas") }
                            IconButton(onClick = {
                                val chosen = notes.filter { it.id in selected }
                                shareText(context, "Minhas notas", notesToText(chosen))
                            }) { Icon(Icons.Filled.Share, contentDescription = "Compartilhar") }
                            IconButton(onClick = { showMove = true }) {
                                Icon(Icons.Filled.Folder, contentDescription = "Mover para categoria")
                            }
                            IconButton(onClick = { showDelete = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                            }
                        }
                    )

                    searching -> TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        title = {
                            TextField(
                                value = query,
                                onValueChange = { vm.query.value = it },
                                placeholder = { Text("Buscar nas notas") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocus)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                searching = false
                                vm.query.value = ""
                            }) { Icon(Icons.Filled.Close, contentDescription = "Fechar busca") }
                        }
                    )

                    else -> TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        title = {
                            Column {
                                Text("Anota", style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    if (notes.size == 1) "1 nota" else "${notes.size} notas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Buscar")
                            }
                            IconButton(onClick = onSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ajustes e backup")
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (!selecting) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedVisibility(visible = fabOpen) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        fabOpen = false
                                        launchCamera()
                                    },
                                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                                    text = { Text("Foto → texto") },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        fabOpen = false
                                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    icon = { Icon(Icons.Filled.Image, contentDescription = null) },
                                    text = { Text("Print → texto") },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        fabOpen = false
                                        scope.launch { onOpenNote(vm.createBlank()) }
                                    },
                                    icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                                    text = { Text("Nova nota") },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        FloatingActionButton(
                            onClick = { fabOpen = !fabOpen },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                if (fabOpen) Icons.Filled.Close else Icons.Filled.Add,
                                contentDescription = if (fabOpen) "Fechar menu" else "Adicionar"
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(4.dp))
                    CategoryBar(
                        categories = categories,
                        selected = filter,
                        onSelect = { vm.filter.value = it },
                        onManage = { showCategories = true }
                    )
                    Spacer(Modifier.height(8.dp))

                    if (shown.isEmpty()) {
                        EmptyState(isFiltering = query.isNotBlank() || filter != null)
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            staggeredItems(shown, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    category = note.categoryId?.let { catById[it] },
                                    selected = note.id in selected,
                                    selecting = selecting,
                                    onClick = {
                                        if (selecting) {
                                            selected = if (note.id in selected) selected - note.id else selected + note.id
                                        } else {
                                            onOpenNote(note.id)
                                        }
                                    },
                                    onLongClick = {
                                        selected = if (note.id in selected) selected - note.id else selected + note.id
                                    }
                                )
                            }
                        }
                    }
                }

                // Escurece o fundo quando o menu do "+" está aberto
                if (fabOpen) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { fabOpen = false }
                    )
                }
            }
        }

        // Carregando OCR
        if (busy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Lendo o texto da imagem…", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // ---- Diálogos ----
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Excluir notas?") },
            text = {
                Text(
                    if (selected.size == 1) "A nota selecionada será apagada e não dá para desfazer."
                    else "As ${selected.size} notas selecionadas serão apagadas e não dá para desfazer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(selected)
                    selected = emptySet()
                    showDelete = false
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
        )
    }

    if (showMove) {
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text("Mover para…") },
            text = {
                Column {
                    val options: List<Category?> = listOf(null) + categories
                    options.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    vm.move(selected, c?.id)
                                    selected = emptySet()
                                    showMove = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(
                                        if (c == null) MaterialTheme.colorScheme.outline else categoryColor(c.colorIndex),
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(c?.name ?: "Sem categoria", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMove = false }) { Text("Cancelar") } }
        )
    }

    if (showCategories) {
        CategoriesDialog(
            categories = categories,
            onAdd = { vm.addCategory(it) },
            onRename = { c, name -> vm.renameCategory(c, name) },
            onRecolor = { vm.recolor(it) },
            onDelete = { vm.deleteCategory(it) },
            onDismiss = { showCategories = false }
        )
    }
}

@Composable
private fun CategoryBar(
    categories: List<Category>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    onManage: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Todas") }
            )
        }
        items(categories, key = { it.id }) { c ->
            FilterChip(
                selected = selected == c.id,
                onClick = { onSelect(c.id) },
                label = { Text(c.name) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(categoryColor(c.colorIndex), CircleShape)
                    )
                }
            )
        }
        item {
            AssistChip(
                onClick = onManage,
                label = { Text("Categorias") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    category: Category?,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent = category?.let { categoryColor(it.colorIndex) }
    val shape = RoundedCornerShape(22.dp)
    val background = when {
        selected -> scheme.primaryContainer
        accent != null -> accent.copy(alpha = 0.10f).compositeOver(scheme.surface)
        else -> scheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        color = background,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) scheme.primary else scheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (category != null && accent != null) {
                    Box(Modifier.size(8.dp).background(accent, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (note.pinned) {
                    Icon(Icons.Filled.PushPin, contentDescription = "Fixada", tint = scheme.primary, modifier = Modifier.size(16.dp))
                }
                if (selecting) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (category != null || note.pinned || selecting) Spacer(Modifier.height(8.dp))

            if (note.title.isNotBlank()) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                formatDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyState(isFiltering: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (isFiltering) "Nada encontrado" else "Nenhuma nota por aqui",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (isFiltering) "Tente outra busca ou outra categoria."
            else "Toque no + para escrever, ou tire uma foto de um texto e eu transformo em nota.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
