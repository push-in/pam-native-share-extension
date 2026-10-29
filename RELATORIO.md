# N6 — recebimento de compartilhamentos no Android

Data: 29/10/2026. Versão patch: 0.4.4.

## Implementação

- Cada intent recebe `eventId` UUID e marcador de consumo nos extras do intent original antes do processamento. A recriação da Activity conserva a identidade e não reentrega o compartilhamento; uma nova ação de compartilhar, mesmo com conteúdo igual, recebe outro ID. O PHP expõe `IncomingShare::eventId`, com argumento opcional para compatibilidade.
- O callback da Activity apenas registra a solicitação. Uma fila limitada e um único worker executam metadados e cópia fora da main thread. A thread termina após ficar ociosa; a fila mantém no máximo 16 solicitações aguardando consumo.
- `IncomingShareLimits` configura os limites na chamada `initial()`, antes da cópia: padrão de 314.572.800 bytes por arquivo, 20 itens e 1 quando o intent ou qualquer URI declarar vídeo. A ordem é preservada. O limite de itens aceita 1–32, e o limite de vídeo aceita 1–maximumItems.
- Streaming com buffer de 64 KiB e verificação dos bytes realmente lidos. Metadados acima do limite rejeitam a entrada antes da cópia. Falhas removem os arquivos parciais e os arquivos já copiados naquela solicitação.
- `IncomingShareException` expõe `eventId` e o enum inteiro sequencial `IncomingShareError`: FileTooLarge=1, CopyFailed=2, QueueFull=3. O listener se rearma mesmo após um erro tipado. Exceções dos callbacks seguem `App::onError()`.
- A API legada de inbox também copia fora da main thread, limita arquivos e utiliza o marcador de consumo.

## Validação

- PHP: **22/22** contratos verdes com o PHP PAM 2.2.7 e o SDK 1.46.6.
- JVM: **8/8** testes verdes; todas as fontes Android de produção compiladas contra a API nativa e o SDK Android 35. JDK com `-Xmx3g`, sem Gradle ou Cargo.
- Android: **7/7** cenários verdes no `emulator-5554`, Android API 36: trabalho fora da main thread, `Activity.recreate()` sem duplicação, novo intent com conteúdo igual, corte de 25 fotos em 20, lote misto cortado em 1, metadados incorretos com rollback após erro de tamanho, e limite de itens configurado pela aplicação.
- PSS no final dos cenários: **42.145 KiB**; em repouso: **36.626 KiB**; CPU em repouso: **0,0%**. Nenhuma thread `pam-incoming-share` persistia após a espera de repouso.
- `composer validate --strict --no-check-lock` e lint PHP verdes.
- Harness isolado construído sem Gradle, com ferramentas Android já instaladas. Sessão curta sob `/tmp/pam-emulator.lock.d`; APK de teste desinstalado e lock liberado. Nenhum dispositivo físico foi utilizado.
- Logs preservados em `~/.local/share/pam-codex/n6/`. Reprodução: `tests/run-jvm.py`, `tests/build-device-test.py` e `tests/run.php`, conforme README.

## Escopo e limites

Somente Android foi alterado/testado; nenhuma mudança visual de produto. O harness verifica o plugin real, mas não substitui QA dos APKs de produção da aplicação consumidora. Não foram utilizados conta QA, credenciais, dispositivos físicos ou plataformas Apple. PHPStan não está disponível nas dependências do repositório; não foram adicionadas dependências para esta tarefa.

A identidade é preservada pelos extras do intent. Não há garantia de entrega após morte do processo durante uma cópia. Os limites configuráveis e o novo identificador são específicos do Android nesta versão.

O repositório do plugin não possui a branch integrate indicada nas regras gerais; foi criado `codex-share-ext`, branch `codex/share-ext`, a partir de `origin/main` (9c18f1a), com fetch e rebase. Fluxo de distribuição: tag `v0.4.4`, GitHub Release e `packagist-publish`.

O disco já começou abaixo dos 20 GB exigidos (cerca de 12 GB). Não havia artefatos anteriores desta tarefa capazes de recuperar a diferença. Foram evitados builds completos; nenhum artefato de outra sessão foi removido. Os diretórios temporários de build desta tarefa são removidos ao encerrar.
