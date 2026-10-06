# Changelog

## 0.3.0 - 2026-10-06

- Accepted share types are configurable per app with `plugins.shareExtension`
  (`accept`, `multiple`) in `pam-native.json`, read at prepare/build time.
  Android intent filters (`SEND`, and `SEND_MULTIPLE` only for `multiple`
  types) and the iOS Share Extension `NSExtensionActivationRule` are
  generated from it instead of being hardcoded to `*/*`, so an app that only
  handles media is no longer offered for PDFs.
- The default stays `*/*`, several items at once, with the same iOS counts.
- Requires PAM Native 1.16 (`plugins.share.v1`). The manifest fragment with
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
