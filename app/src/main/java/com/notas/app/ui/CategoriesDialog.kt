package com.notas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notas.app.data.Category

@Composable
fun CategoriesDialog(
    categories: List<Category>,
    onAdd: (String) -> Unit,
    onRename: (Category, String) -> Unit,
    onRecolor: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Category?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categorias") },
        text = {
            Column {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (categories.isEmpty()) {
                        Text(
                            "Nenhuma categoria ainda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    categories.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toque na bolinha para trocar a cor
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(categoryColor(c.colorIndex), CircleShape)
                                    .clickable { onRecolor(c) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                c.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                renaming = c
                                renameText = c.name
                            }) { Icon(Icons.Filled.Edit, contentDescription = "Renomear") }
                            IconButton(onClick = { deleting = c }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                            }
                        }
                    }
                }
                Text(
                    "Dica: toque na bolinha colorida para trocar a cor.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nova categoria") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onAdd(newName)
                            newName = ""
                        },
                        enabled = newName.isNotBlank()
                    ) { Icon(Icons.Filled.Add, contentDescription = "Adicionar categoria") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Pronto") } }
    )

    renaming?.let { c ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Renomear categoria") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(c, renameText)
                        renaming = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancelar") } }
        )
    }

    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir \"${c.name}\"?") },
            text = { Text("As notas dessa categoria não serão apagadas, só ficam sem categoria.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(c)
                    deleting = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}
