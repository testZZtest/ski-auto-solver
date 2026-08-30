# Ski Auto Solver

Base Android experimental de um agente que observa a tela continuamente, interpreta uma mecânica de minigame, procura uma sequência de ações e executa os toques. O tipo de minigame é escolhido manualmente no painel para evitar gastar processamento tentando classificar a mecânica.

## v0.1
- Overlay flutuante com liga/desliga.
- Painel para selecionar a mecânica.
- Captura contínua por MediaProjection.
- AccessibilityService para gestos/taps.
- FrameBus compartilhando o frame mais recente.
- Water Sort com solver por busca de estados.
- Water Sort visual inicial por amostragem/quantização de cor.
- Uma ação por ciclo: o bot observa o estado novamente antes de escolher o movimento seguinte.

## Arquitetura
`capture` -> captura da tela  
`vision` -> transforma pixels em estado do jogo  
`model` -> representação matemática  
`solver` -> procura solução  
`engine` -> coordena ações  
`accessibility` -> executa gestos  
`overlay` -> controla o agente por cima do jogo

A próxima evolução deve manter essas fronteiras. Assim, Car Jam, Tile Match, Block Puzzle e Fruit Merge entram como novos módulos de visão + solver, sem reescrever captura/overlay/toque.

## Water Sort
O solver considera o estado completo das garrafas e procura uma sequência até um estado resolvido. Isso é diferente de escolher a melhor jogada local.

A visão da v0.1 ainda é experimental: assume um layout típico de oito tubos e usa posições proporcionais. Ela não é universal ainda. O próximo passo é detectar a geometria dos tubos e os níveis de líquido diretamente na imagem, validar o estado e só então pedir uma ação ao solver.

## Gerar o APK pelo GitHub Actions
O repositório já contém workflows em `.github/workflows/`.

1. Crie um repositório vazio no GitHub.
2. Envie todos os arquivos deste projeto para a branch `main`.
3. Abra a aba **Actions**.
4. Entre em **Build Ski Auto Solver APK**.
5. Clique em **Run workflow**.
6. Quando terminar, abra a execução concluída e baixe o artefato **SkiAutoSolver-debug-apk**.
7. Extraia o artefato e instale `app-debug.apk` no Android.

O workflow usa JDK 17, Android SDK 35 e Gradle 8.10. A compilação acontece nos runners do GitHub, então Android Studio não é necessário no celular.

## Permissões
No Android, o usuário precisará conceder:
1. sobreposição sobre outros apps;
2. captura de tela pelo diálogo do sistema;
3. acessibilidade para executar gestos.

## Observação
Esta é uma build de desenvolvimento. O APK ainda não tem assinatura de release para distribuição em loja. Para testes pessoais, o artefato `debug` é suficiente.
