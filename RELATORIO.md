# Relatório — enxugamento do núcleo, onda 2

Pacote: `pushinbr/pam-native-share-extension`. Branch: `feat/port4-core-slim-20261028`. Versão pública: `0.4.1`.

## O que foi feito

- A capacidade opcional passou a pertencer ao plugin; SDK mínimo 1.46.0.
- O núcleo conserva aliases PHP depreciados por uma versão, até 1.47.
- Contratos nativos e dados tipados foram preservados; testes acompanham a implementação.
- Release: [0.4.1](https://github.com/push-in/pam-native-share-extension/releases/tag/v0.4.1). Atualização do Packagist solicitada e aceita pela API.

## Validação

- PHP: 8/8.
- Compile Kotlin conjunto do núcleo e cinco plugins: aprovado.
- Testes JVM conjuntos: 360/360, zero falhas.
- Compilação dos testes instrumentados conjuntos: aprovada; execução em dispositivo pendente.
- SDK PHP com contratos de compatibilidade dos plugins: aprovado.
- Swift: pendente; Mac proibido.
- QA em dispositivo e medição de CPU/PSS: pendentes; o emulador compartilhado estava ocupado. S10 não utilizado.

## Commits de implementação

```text
21fc238 fix: include the incoming-share host provider manifest [skip ci]
86345e9 feat: own incoming shares through generic host events [skip ci]
187249b fix: drop the plugins.share.v1 capability requirement (0.3.1) [skip ci]
```

O commit deste relatório é posterior aos commits acima. Nenhuma release do núcleo 1.46.0 foi publicada.
