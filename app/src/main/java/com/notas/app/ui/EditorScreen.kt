package com.notas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notas.app.MainViewModel
import com.notas.app.data.Note
import com.notas.app.util.formatDate
import com.notas.app.util.noteToText
import com.notas.app.util.shareText
import kotlinx.coroutines.delay

private fun Note.sameAs(o: Note) =
    title == o.title && content == o.content && categoryId == o.categoryId && pinned == o.pinned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long,
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val categories by vm.categories.collectAsStateWithLifecycle()

    var base by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var pinned by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    LaunchedEffect(noteId) {
        val n = vm.getNote(noteId)
        if (n == null) {
            onBack()
        } else {
            base = n
            title = n.title
            content = n.content
            categoryId = n.categoryId
            pinned = n.pinned
            if (n.title.isEmpty() && n.content.isEmpty()) runCatching { titleFocus.requestFocus() }
        }
    }

    fun current(): Note? =
        base?.copy(title = title, content = content, categoryId = categoryId, pinned = pinned)

    // Salva sozinho meio segundo depois de parar de digitar
    LaunchedEffect(title, content, categoryId, pinned) {
        val b = base ?: return@LaunchedEffect
        val cur = current() ?: return@LaunchedEffect
        if (cur.sameAs(b)) return@LaunchedEffect
        delay(500)
        vm.update(cur)
        base = cur
    }

    // Salva na hora ao sair do app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val b = base
                val cur = current()
                if (b != null && cur != null && !cur.sameAs(b) && (cur.title.isNotBlank() || cur.content.isNotBlank())) {
                    vm.update(cur)
                    base = cur
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ao fechar a tela: apaga se ficou vazia, senão garante que foi salva
    DisposableEffect(Unit) {
        onDispose {
            val b = base
            val cur = current()
            if (b != null && cur != null) {
                if (cur.title.isBlank() && cur.content.isBlank()) {
                    vm.delete(listOf(cur.id))
                } else if (!cur.sameAs(b)) {
                    vm.update(cur)
                }
            }
        }
    }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { pinned = !pinned }) {
                        Icon(
                            if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (pinned) "Desafixar" else "Fixar no topo",
                            tint = if (pinned) scheme.primary else scheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        val cur = current()
                        if (cur != null) shareText(context, cur.title.ifBlank { null }, noteToText(cur))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Compartilhar") }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Categoria
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("Sem categoria") }
                    )
                }
                items(categories, key = { it.id }) { c ->
                    FilterChip(
                        selected = categoryId == c.id,
                        onClick = { categoryId = c.id },
                        label = { Text(c.name) },
                        leadingIcon = {
                            Box(Modifier.size(10.dp).background(categoryColor(c.colorIndex), CircleShape))
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Título
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                ),
                cursorBrush = SolidColor(scheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Título",
                                style = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
                                color = scheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .focusRequester(titleFocus)
            )

            Spacer(Modifier.height(12.dp))

            // Texto
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    color = scheme.onSurface
                ),
                cursorBrush = SolidColor(scheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                "Escreva aqui…",
                                style = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
                                color = scheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .padding(horizontal = 20.dp)
            )

            base?.let { b ->
                Text(
                    "Criada em ${formatDate(b.createdAt)}  ·  ${content.length} caracteres",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Excluir nota?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    val id = base?.id
                    base = null // evita o "salvar ao sair" recriar a nota
                    if (id != null) vm.delete(listOf(id))
                    onBack()
                }) { Text("Excluir", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
        )
    }
}
