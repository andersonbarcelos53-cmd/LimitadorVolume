# Limitador de Volume por Aplicativo

Aplicativo Android nativo em Kotlin que permite selecionar aplicativos instalados e definir um limite máximo para o volume de mídia enquanto cada aplicativo estiver em primeiro plano.

O limite altera o volume geral de mídia do aparelho. O Android não oferece uma API pública para controlar separadamente o áudio interno de cada aplicativo comum.

## Tecnologias

- Kotlin
- Jetpack Compose
- Material Design 3
- Gradle com Kotlin DSL
- Arquitetura MVVM
- DataStore Preferences
- UsageStatsManager
- AudioManager
- Foreground Service
- Coroutines e StateFlow

## Como abrir no Android Studio

1. Abra o Android Studio.
2. Selecione `Open`.
3. Escolha a pasta `Limitador Volume`.
4. Aguarde o Gradle Sync baixar as dependências.
5. Conecte um celular Android ou inicie um emulador.

O projeto usa `compileSdk 37`, `minSdk 26`, AGP 9.2.1 e prioriza Android 11 ou superior.

Para compilar pelo terminal, use JDK 17. O Android Studio normalmente já vem com um JDK compatível; fora dele, configure `JAVA_HOME` apontando para esse JDK.

## Como compilar

No terminal do Android Studio, execute:

No Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

No macOS ou Linux:

```bash
./gradlew :app:assembleDebug
```

Ou use o menu `Build > Make Project`.

## Como executar os testes

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Os testes cobrem:

- Conversão de percentual para nível de volume.
- Busca de regra ativa por aplicativo.
- Ativação e desativação de regras.
- Salvamento do volume original durante uma sessão.
- Restauração do volume.
- Troca direta entre dois aplicativos monitorados.
- Volume abaixo e acima do limite.

## Como gerar um APK de teste

```powershell
.\gradlew.bat :app:assembleDebug
```

O APK debug será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Como instalar por USB usando ADB

1. Ative `Opções do desenvolvedor` no celular.
2. Ative `Depuração USB`.
3. Conecte o aparelho por cabo.
4. Execute:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissões necessárias

### Acesso ao uso de aplicativos

O Android não concede `PACKAGE_USAGE_STATS` por diálogo comum. O app abre a tela:

```text
Settings.ACTION_USAGE_ACCESS_SETTINGS
```

Nessa tela, libere o acesso para `Limitador de Volume`.

### Notificações

No Android 13 ou superior, o app solicita `POST_NOTIFICATIONS` para exibir a notificação permanente do Foreground Service.

### Serviço em primeiro plano

O monitoramento usa um Foreground Service com notificação fixa. A notificação possui uma ação para interromper o monitoramento.

## Remover restrição de bateria

Algumas fabricantes encerram serviços em segundo plano. Se o monitoramento parar sozinho:

1. Abra a tela `Informações e permissões` no app.
2. Toque em `Abrir configurações de bateria`.
3. Remova a otimização/restrição de bateria para o app.

Os nomes exatos variam conforme Samsung, Xiaomi, Motorola e outros fabricantes.

## Como funciona

1. O serviço consulta eventos recentes do `UsageStatsManager`.
2. Identifica o aplicativo mais recente movido para primeiro plano.
3. Ignora o próprio app Limitador de Volume.
4. Procura uma regra ativa para o pacote detectado.
5. Converte o percentual configurado para um nível inteiro do `STREAM_MUSIC`.
6. Se o volume atual estiver acima do limite, reduz o volume.
7. Se o volume estiver abaixo do limite, não aumenta automaticamente.
8. Ao sair de apps monitorados, restaura o volume original somente quando o app realmente reduziu o volume.

## Limitações conhecidas

- O app limita o volume geral de mídia, não o áudio individual de cada aplicativo.
- `UsageStatsManager` depende de permissão manual do usuário.
- A detecção por eventos pode variar entre fabricantes.
- Serviços em primeiro plano podem sofrer restrições agressivas de bateria.
- Reinício automático após ligar o aparelho pode ser bloqueado em versões ou fabricantes específicos do Android.
- Se o usuário reduzir manualmente o volume durante a sessão, o app evita restaurar um valor antigo para não desfazer uma ação intencional.

## Estrutura principal

```text
com.example.volumelimiter
├── data
│   ├── datastore
│   ├── model
│   └── repository
├── domain
│   └── usecase
├── receiver
├── service
├── ui
│   ├── components
│   ├── navigation
│   ├── screens
│   └── theme
├── util
├── viewmodel
└── MainActivity.kt
```
