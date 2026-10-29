# Changelog

## 0.4.4 - 2026-10-29

- Android: identificador estável por intent e marcador de consumo impedem reentrega em recriação da Activity.
- Cópia em streaming fora da main thread, com limite por arquivo (300 MiB por padrão), falhas tipadas e remoção de cópias parciais.
- Limites configuráveis pelo app antes da cópia: 20 itens e 1 quando houver vídeo por padrão.
- PHP: `IncomingShare::eventId`, `IncomingShareLimits` e `IncomingShareException` com enum inteiro `IncomingShareError`.
- Testes JVM de streaming, limites e identidade; contratos PHP de configuração, erros e continuidade do listener após falha.

## 0.4.3 - 2026-10-28

- Android: `IncomingShareModule` has a `(Context)` constructor, which the
  generated plugin registry calls; 0.4.1 failed to build in a real app.
- 0.4.2 was cut from the wrong base (0.3.1) and is withdrawn; use 0.4.3.

## 0.4.1

- Include the process host provider manifest so cold/warm Android intents reach IncomingShares.

## 0.4.0

- Owns the optional incoming shares capability extracted from the 1.46 core.
- Preserves PHP aliases and native contracts during migration.


## 0.3.1 - 2026-10-06

- Stop requiring the `plugins.share.v1` capability: the PAM Native PHP
  runtime does not list it, so 0.3.0 failed plugin discovery at startup
  ("requires unavailable capabilities"). PAM Native 1.16 is still enforced by
  Composer and `pamNative.minimum`.

## 0.3.0 - 2026-10-06

- Accepted share types are configurable per app with `plugins.shareExtension`
  (`accept`, `multiple`) in `pam-native.json`, read at prepare/build time.
  Android intent filters (`SEND`, and `SEND_MULTIPLE` only for `multiple`
  types) and the iOS Share Extension `NSExtensionActivationRule` are
  generated from it instead of being hardcoded to `*/*`, so an app that only
  handles media is no longer offered for PDFs.
- The default stays `*/*`, several items at once, with the same iOS counts.
- Requires PAM Native 1.16. The manifest fragment with
  the `*/*` filters and the hardcoded activation rule are removed.
- The example accepts text, images and videos.

## 0.2.2 - 2026-10-04

- Copy incoming iOS file URLs into the App Group inbox and preserve the source
  title, original filename, and a stable identifier for each share.
- Clarify that the core `IncomingShares` API imports files into the application
  sandbox, while `ShareInbox` exposes raw App Group file tokens on iOS.

## 0.2.1 - 2026-08-25

- Accept the PAM Native 1.x runtime and plugin contract.

## 0.2.0 - 2026-08-23

- Support the PAM Native 0.8 line on PHP 8.5.

## 0.1.2 - 2026-08-24

- Expand the plugin contract through the complete pre-1.0 PAM Native line.

## 0.1.1 - 2026-08-24

- Certify PAM Native 0.9.1 while preserving the supported 0.8 line.

## 0.1.0 - 2026-08-01

- Initial public release of the documented PAM Native package contract.
- Add bounded input validation, sequential integer protocol enums, automated
  package tests, and PHP 8.4/8.5 continuous integration.
