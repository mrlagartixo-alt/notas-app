package com.notas.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.notas.app.MainViewModel
import com.notas.app.backup.BackupManager
import com.notas.app.backup.BackupPrefs
import com.notas.app.backup.BackupScheduler
import com.notas.app.util.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val prefs = remember { BackupPrefs(context) }

    var enabled by remember { mutableStateOf(prefs.enabled) }
    var dest by remember { mutableStateOf(prefs.destEmail) }
    var sender by remember { mutableStateOf(prefs.senderEmail) }
    var password by remember { mutableStateOf(prefs.appPassword) }
    var hour by remember { mutableFloatStateOf(prefs.hour.toFloat()) }
    var showPassword by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var lastTime by remember { mutableStateOf(prefs.lastTime) }
    var lastStatus by remember { mutableStateOf(prefs.lastStatus) }

    fun looksLikeEmail(s: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()

    /** Devolve uma mensagem de erro, ou null se está tudo certo. */
    fun validate(): String? = when {
        !looksLikeEmail(dest) -> "O e-mail que vai receber o backup parece inválido."
        !looksLikeEmail(sender) -> "O e-mail remetente (Gmail) parece inválido."
        password.isBlank() -> "Cole a senha de app do Gmail."
        else -> null
    }

    fun save() {
        prefs.enabled = enabled
        prefs.destEmail = dest.trim()
        prefs.senderEmail = sender.trim()
        prefs.appPassword = password.replace(" ", "")
        prefs.hour = hour.toInt()
        BackupScheduler.schedule(context, replace = true)
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                    }
                    vm.restore(text)
                }
                result.onSuccess { count ->
                    snackbar.showSnackbar(
                        when (count) {
                            0 -> "Nada novo para restaurar: essas notas já estão no app."
                            1 -> "1 nota restaurada."
                            else -> "$count notas restauradas."
                        }
                    )
                }.onFailure {
                    snackbar.showSnackbar("Não consegui ler esse arquivo. Use o .json do backup.")
                }
            }
        }
    }

    Scaffold(
        containerColor = scheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background),
                title = { Text("Backup e ajustes", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Backup diário ----
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(scheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null, tint = scheme.primary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Backup diário por e-mail", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Todo dia, suas notas vão para o e-mail escolhido.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    OutlinedTextField(
                        value = dest,
                        onValueChange = { dest = it },
                        label = { Text("Enviar o backup para") },
                        placeholder = { Text("voce@email.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sender,
                        onValueChange = { sender = it },
                        label = { Text("Seu Gmail (quem envia)") },
                        placeholder = { Text("seunome@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha de app do Gmail") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            "Horário do backup: ${String.format(Locale.US, "%02d:00", hour.toInt())}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = hour,
                            onValueChange = { hour = it },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                        Text(
                            "O Android pode atrasar alguns minutos para economizar bateria, e o envio precisa de internet.",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    if (lastTime > 0L) {
                        val ok = lastStatus == "ok"
                        Text(
                            if (ok) "✓ Último backup enviado em ${formatDate(lastTime)}"
                            else "✗ Último envio falhou (${formatDate(lastTime)}): $lastStatus",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ok) scheme.primary else scheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val problem = if (enabled) validate() else null
                            if (problem != null) {
                                scope.launch { snackbar.showSnackbar(problem) }
                            } else {
                                save()
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (enabled) "Salvo! Próximo backup às ${String.format(Locale.US, "%02d:00", hour.toInt())}."
                                        else "Salvo. O backup diário está desligado."
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Salvar") }

                    OutlinedButton(
                        onClick = {
                            val problem = validate()
                            if (problem != null) {
                                scope.launch { snackbar.showSnackbar(problem) }
                            } else {
                                save()
                                scope.launch {
                                    sending = true
                                    val result = BackupManager.sendBackup(context)
                                    sending = false
                                    lastTime = prefs.lastTime
                                    lastStatus = prefs.lastStatus
                                    snackbar.showSnackbar(
                                        if (result.isSuccess) "Backup enviado! Confira a caixa de entrada."
                                        else "Falhou: ${BackupManager.friendlyError(result.exceptionOrNull()!!)}"
                                    )
                                }
                            }
                        },
                        enabled = !sending,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (sending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Enviando…")
                        } else {
                            Text("Enviar backup agora (teste)")
                        }
                    }
                }
            }

            // ---- Como criar a senha de app ----
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Como criar a senha de app (1 minuto)",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSecondaryContainer
                    )
                    Text(
                        "1. Na sua conta Google, ative a verificação em duas etapas.\n" +
                            "2. Abra a página de senhas de app, dê um nome (ex.: Anota) e toque em Criar.\n" +
                            "3. Copie a senha de 16 letras e cole no campo acima.\n\n" +
                            "É uma senha só para este app: a sua senha normal do Gmail nunca é usada, e você pode apagá-la no Google quando quiser. Ela fica guardada criptografada no seu aparelho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSecondaryContainer
                    )
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }) { Text("Abrir página do Google") }
                }
            }

            // ---- Restaurar ----
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Restaurar notas", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Trocou de celular? Baixe o arquivo .json do e-mail de backup e escolha ele aqui. Notas repetidas não são duplicadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Restaurar de um arquivo de backup")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
