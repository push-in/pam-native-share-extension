<!-- pam:product-page:start -->
<div align="center">

# PAM Native Share Extension

**Bring content into your app from anywhere in the operating system.**

Receive validated text, URLs, and files from Android shares and an iOS Share Extension through sandbox-safe handoff contracts.

[![Latest version](https://img.shields.io/packagist/v/pushinbr/pam-native-share-extension?style=flat-square&label=stable)](https://packagist.org/packages/pushinbr/pam-native-share-extension)
[![CI](https://img.shields.io/github/actions/workflow/status/push-in/pam-native-share-extension/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/push-in/pam-native-share-extension/actions)
![PHP](https://img.shields.io/badge/PHP-8.5-777BB4?style=flat-square&logo=php&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-15%2B-000000?style=flat-square&logo=apple&logoColor=white)

**[Documentation](https://push-in.github.io/pam-docs/native/overview/) · [Quick start](#quick-start) · [What you can build](#what-you-can-build) · [PAM ecosystem](https://push-in.github.io/pam-docs/ecosystem/) · [Issues](https://github.com/push-in/pam-native-share-extension/issues)**

</div>

---

## Why PAM Native Share Extension

Receive validated text, URLs, and files from Android shares and an iOS Share Extension through sandbox-safe handoff contracts. The public API is strictly typed for PHP 8.5; expensive or frame-sensitive work stays in Rust or the platform SDK instead of crossing the application boundary every frame.

| | |
| --- | --- |
| **Best for** | A focused capability you can add to any PAM Native application |
| **Native path** | Android Sharesheet · iOS Share Extension |
| **Application model** | Composer package + generated native integration |
| **Design rule** | Independent module; no feed, vertical, or application template bundled |

## What you can build

- Save-to-app and read-later flows
- Import media or documents from other apps
- Create posts or messages from the system share sheet

## Quick start

Already have a PAM Native project? Add only this capability:

```bash
pam composer require pushinbr/pam-native-share-extension
pam doctor --fix
```

New to PAM? Follow the **[five-minute PAM Native setup](https://push-in.github.io/pam-docs/native/overview/)** once, then return here. Your application stays a normal Composer project with a committed lockfile.
<!-- pam:product-page:end -->

## See it in action

Receives text, URLs and sandboxed file copies from Android `ACTION_SEND`/`ACTION_SEND_MULTIPLE` and an iOS Share Extension. Call `ShareInbox::drain()` after launch or resume.

The iOS extension keeps each share together, copies file URLs into the App Group inbox, and carries the original filename and title when the source app provides them. PAM Native's `IncomingShares` API imports these files into the app sandbox and exposes the filename as `$file->name`. Use either `IncomingShares` or this package's `ShareInbox` to consume the App Group inbox; both drain the same entries.

iOS requires the generated app and extension targets to share `group.<application-id>.pam-native`; the supplied entitlements use the `PAM_NATIVE_APPLICATION_ID` build setting. Never trust shared MIME types or file contents—validate them before processing or uploading.

## Install

```bash
pam add share-extension
pam doctor
```

PAM Native generates the iOS Share Extension and Android intent filters automatically from the package manifest and the types you accept.

## Choose the accepted types

By default the app is offered for every share (`*/*`), several items at once. Narrow it in `pam-native.json`; prepare/build reads it (PAM Native 1.16+):

```json
{
    "plugins": {
        "shareExtension": {
            "accept": ["text/plain", "image/*", "video/*"],
            "multiple": ["image/*", "video/*"]
        }
    }
}
```

| Key | Default | Meaning |
| --- | --- | --- |
| `accept` | `["*/*"]` | 1–16 lowercase MIME types (`type/subtype` or `type/*`). |
| `multiple` | `true` | `true`: every accepted type may arrive several at once; `false`: single items only; or a list of accepted types that may. |

With the example above a PDF share no longer lists the app, a single text or link does, and photos/videos may arrive in batches. Keep validating what arrives in PHP: the share sheet is untrusted input.

## Drain the inbox

```php
use Pam\Native\ShareExtension\ShareInbox;

(new ShareInbox())->drain(function (array $items): void {
    foreach ($items as $item) {
        // $item->kind is a typed enum; $item->value is text, URL, or a file inbox token.
        handleSharedItem($item);
    }
});
```

Call `drain()` after launch and whenever the app resumes. Successfully returned entries are consumed, so move any file you need to retain into application-owned storage.

For file imports on both platforms, prefer PAM Native's `IncomingShares`: it returns `FileReference` values in the application sandbox. On iOS, `ShareInbox` exposes the App Group file token and consumes the same inbox without importing that file into the application sandbox.


## What installation does

`pam add share-extension` resolves the official compatible package, performs a non-mutating Composer preflight, updates the normal `composer.json` and `composer.lock`, refreshes generated native integration when required, and leaves the project ready for `pam doctor` validation. Nothing is added to `pam-native.json`; add `plugins.shareExtension` only to narrow the accepted types.

Use `pam packages` to inspect availability and `pam remove share-extension` to uninstall the capability safely. Direct Composer commands are an advanced interoperability path; PAM is the supported application workflow.

### Android

PAM Native adds one intent filter per accepted type to `PamActivity`:
`android.intent.action.SEND`, plus `SEND_MULTIPLE` for the types in
`multiple`. The default is `*/*` with both actions. No permissions are needed:
shared content URIs are copied into the app sandbox. Types listed in the
core `android.shareTargets` are merged into the same filters.

### iOS

- A Share Extension target (`PamShareExtension`, bundle suffix `.share`,
  `com.apple.share-services`) generated from `ios/ShareExtension`.
- Activation rule generated from `accept`/`multiple`. The default `*/*` keeps
  text, up to 8 web URLs, 32 files, 32 images and 8 movies. `text/plain`
  enables text and web URLs, `image/*` images (32, or 1), `video/*` movies
  (8, or 1). Specific types such as `application/pdf` become a
  `UTI-CONFORMS-TO` predicate; prepare rejects a type with no known iOS
  identifier, so use a wildcard such as `application/*` for those.
- App Group `group.<application id>.pam-native` on both the app
  (`ios/App.entitlements`) and the extension; `PAM_NATIVE_APPLICATION_ID`
  fills it in. Register the App Group for both bundle ids in your Apple
  developer account and include it in both provisioning profiles.
- Framework `UniformTypeIdentifiers`.

## Core `IncomingShares` or `ShareInbox`?

| | `Pam\Native\System\IncomingShares` (core) | `ShareInbox` (this package) |
| --- | --- | --- |
| Delivery | `initial(Closure(?IncomingShare))` at launch and `listen(Closure(IncomingShare))` while running | `drain(Closure(list<SharedItem>))` when you ask |
| Files | Imported into the app sandbox as `FileReference` (`$file->path`, `$file->name`) | Android: name of a private copy in `filesDir/pam-share-inbox` (outside the `FileReference` sandbox); iOS: raw App Group file tokens |
| Grouping | One `IncomingShare` per share (`text`, `subject`, `mimeType`, `files`) | One `SharedItem` per text, URL or file |

Both consume the same iOS App Group inbox, so use one of them per app. Zé
Chat routes shares with the core API:

```php
use Pam\Native\IncomingShare;
use Pam\Native\System\IncomingShares;

IncomingShares::initial(function (?IncomingShare $share): void {
    if ($share !== null) {
        $this->routeShare($share);   // pick a chat, then send $share->files / $share->text
    }
});
IncomingShares::listen($this->routeShare(...));
```

A runnable minimal app using `ShareInbox` is in [`example/`](example).

## API reference

All classes live in `Pam\Native\ShareExtension`.

| API | Description |
| --- | --- |
| `(new ShareInbox())->drain(Closure(list<SharedItem>) $complete): int` | Returns and consumes every pending item (module `share-extension`). On Android it reads the share intent that launched the current activity (text, http(s) links as `Url`, up to 32 streams copied in 64 KiB chunks) and clears it. A native failure yields an empty list. |
| `SharedItem` (readonly) | `identifier`, `kind` (`SharedItemKind`), `value` (text or URL up to 8192 bytes, or the file name/token), `mimeType`, `createdAtMillis`. |
| `SharedItemKind` (int enum) | `Text = 1`, `Url = 2`, `File = 3`. |
| `ShareExtensionPluginProvider` | Plugin provider. Accepted types are configured in `pam-native.json` under `plugins.shareExtension`. |

## Production checklist

- Drain after launch and every app resume.
- Move files that must survive after the successful drain.
- Validate MIME type, extension, content, and size before parsing or upload.
- Run `pam doctor`, `pam test`, and a signed release build on every supported platform.
- Exercise denial, cancellation, backgrounding, process restart, and offline behavior before release.

## Troubleshooting

- **iOS items do not arrive:** verify the shared app group on both targets.
- **Android app is absent from sharing:** inspect the generated intent filters and `plugins.shareExtension.accept`.
- **The app appears for types it should not handle:** narrow `plugins.shareExtension.accept` and rebuild.
- **A drained file disappears:** move it into application-owned storage before returning.
- **Native integration is stale:** run `pam doctor --fix`, rebuild the native host, and inspect the first reported diagnostic.

## Compatibility and support

| `pushinbr/pam-native-share-extension` | `pushinbr/pam-native` | Android | iOS |
| --- | --- | --- | --- |
| 0.3.1 | `>=1.16.0 <2.0.0` | API 26+ | 15+, accepted types from `plugins.shareExtension` |
| 0.3.0 | `>=1.16.0 <2.0.0` | — | Do not use: fails plugin discovery at runtime |
| 0.2.2 | `>=0.8.0 <2.0.0` (tested with 1.14.x) | API 26+ | 15+, files copied into the App Group with names and titles |
| 0.2.1 | `>=0.8.0 <2.0.0` | API 26+ | 15+ |

This package targets PAM Native `1.16` through `1.x`, Android API 26+, and iOS 15+ unless a platform-specific section above states a stricter requirement. Platform SDKs, credentials, entitlements, physical hardware, and store configuration remain application responsibilities.

- [PAM documentation](https://push-in.github.io/pam-docs/introduction/)
- [PAM Native overview](https://push-in.github.io/pam-docs/native/overview/)
- [Plugin and native capability model](https://push-in.github.io/pam-docs/native/plugins/)
- [Report an issue](https://github.com/push-in/pam-native-share-extension/issues)

Security vulnerabilities should be reported through the repository security policy or GitHub private vulnerability reporting, not a public issue.
