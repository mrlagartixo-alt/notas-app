# Anota — bloco de notas para Android

App de notas com visual moderno, categorias, busca, seleção múltipla, compartilhamento, leitura de texto por foto/print e **backup diário por e-mail**.

## Como baixar o APK

1. Suba este projeto para o GitHub (repositório `notas-app`).
2. O GitHub compila sozinho: aba **Actions → Gerar APK** (leva uns 5 minutos).
3. Quando terminar, abra **Releases** (coluna da direita no repositório) e baixe o **Anota.apk**.
4. No celular, abra o arquivo e permita "instalar de fontes desconhecidas" quando o Android pedir.

Cada vez que você der `git push`, um novo APK é gerado. Eles se instalam por cima um do outro, sem perder as notas.

> Se a aba Actions estiver desativada, entre em **Actions** e clique em "I understand my workflows, go ahead and enable them".

## Recursos

- Notas com título e texto, salvamento automático
- Categorias com cores (criar, renomear, trocar cor, excluir)
- Fixar notas no topo, busca por título e texto
- Segurar uma nota = modo seleção → **selecionar todas**, compartilhar, mover de categoria ou excluir
- **Foto → texto** e **Print → texto** (reconhecimento offline, ML Kit)
- Receber texto do menu "Compartilhar" de qualquer app (vira nota)
- Backup diário por e-mail (.json para restaurar + .txt para ler) e restauração pelo arquivo .json
- Tema claro/escuro automático

## Backup diário por e-mail

O app envia o e-mail pela **sua própria conta Gmail**, usando uma *senha de app* (não a senha normal):

1. Conta Google → ative a verificação em duas etapas.
2. Abra https://myaccount.google.com/apppasswords e crie uma senha chamada "Anota".
3. No app: ⚙️ Ajustes → preencha o e-mail de destino, seu Gmail, a senha de app e o horário → **Salvar**.
4. Use **Enviar backup agora (teste)** para conferir.

## Detalhes técnicos

Kotlin · Jetpack Compose · Room · WorkManager · ML Kit Text Recognition · JavaMail (SMTP).
Pacote: `com.notas.app` · minSdk 26 (Android 8+).

O APK é assinado com a chave em `keystore/anota.jks`, para que as atualizações instalem por cima. Deixe o repositório **privado** se não quiser que essa chave fique pública.
