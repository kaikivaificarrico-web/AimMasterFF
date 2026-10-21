# Aim Master FF

App Android independente de treino de precisão (mira e "capa") para jogadores de Free Fire.
Não é afiliado à Garena, não acessa o jogo, não pede senhas, não injeta código, não modifica
arquivos de outros apps e **não declara nenhuma permissão** (sem internet).

## Funcionalidades (v1.1.0)
- Tela inicial com botões grandes, animações de entrada e título pulsante; tema escuro (padrão) e claro
- Simulador de mira: 30 s, alvos que andam e mudam de direção, cabeça = 100 pts, corpo = 30, erro = -10, bônus por velocidade
- Treino de capa: 15 rodadas; toque no peito e arraste até a cabeça; mede reação e tempo de puxada
- Demonstração da capa: uma mira fantasma amarela faz a puxada ideal do peito à cabeça (câmera lenta nas 3 primeiras rodadas, depois em velocidade real); você repete e o app mostra o desvio do seu trajeto em relação ao ideal
- Sensibilidade: detecta tipo de aparelho (tela, Hz, RAM) e sugere valores por estilo de jogo, com ajuste fino e dica baseada nos seus treinos
- Estatísticas: histórico (até 300 sessões), médias, melhor pontuação, evolução e gráfico de precisão
- Configurações: tema, vibração, tamanho e velocidade dos alvos
- Dados salvos apenas no aparelho (SharedPreferences)

## Como compilar
1. Instale o Android Studio (Koala ou mais novo) com JDK 17.
2. File > Open e escolha a pasta `AimMasterFF`. Aguarde o Gradle sincronizar (baixa o Gradle 8.7 sozinho).
3. Conecte um celular com Depuração USB ativada (ou crie um emulador) e clique em Run.
4. Para gerar APK: Build > Build Bundle(s) / APK(s) > Build APK(s).
   Por linha de comando (após `gradle wrapper`): `./gradlew assembleDebug`
   O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura
- `data/` repositórios de configurações e estatísticas
- `training/` modos, resultado de sessão e cálculo de sensibilidade
- `ui/` activities e views customizadas (TargetFieldView, ChartView)

## Gerar o APK pelo GitHub (sem Android Studio)
1. Crie um repositório no github.com e envie **todo o conteúdo** desta pasta, incluindo `.github/workflows/build.yml`.
2. Abra a aba **Actions**, escolha "Build APK" e clique em **Run workflow** (ou espere rodar sozinho após o envio).
3. Quando ficar verde, abra a execução e baixe **AimMasterFF-apk** em Artifacts (vem num .zip com o `app-debug.apk`).
4. No celular: extraia o zip, toque no APK e permita "instalar apps desconhecidos" para o app que você usou para abrir o arquivo.
   Para remover, desinstale como qualquer app.

## Link direto para baixar o APK
O workflow também publica o APK numa Release chamada "latest". Com o repositório **público**, o link fica:
`https://github.com/SEU_USUARIO/SEU_REPOSITORIO/releases/latest/download/AimMasterFF.apk`
Abra esse link no navegador do celular para baixar e instalar.
