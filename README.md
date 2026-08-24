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

iOS requires the generated app and extension targets to share `group.<application-id>.pam-native`; the supplied entitlements use the `PAM_NATIVE_APPLICATION_ID` build setting. Never trust shared MIME types or file contents—validate them before processing or uploading.

## Install

```bash
pam add share-extension
pam doctor
```

PAM Native generates the iOS Share Extension and Android intent filters automatically from the package manifest.

## Drain the inbox

```php
use Pam\Native\ShareExtension\ShareInbox;

(new ShareInbox())->drain(function (array $items): void {
    foreach ($items as $item) {
        // $item->kind is a typed enum; $item->value is text, URL, or a copied file path.
        handleSharedItem($item);
    }
});
```

Call `drain()` after launch and whenever the app resumes. Successfully returned entries are consumed, so move any file you need to retain into application-owned storage.


## What installation does

`pam add share-extension` resolves the official compatible package, performs a non-mutating Composer preflight, updates the normal `composer.json` and `composer.lock`, refreshes generated native integration when required, and leaves the project ready for `pam doctor` validation.

Use `pam packages` to inspect availability and `pam remove share-extension` to uninstall the capability safely. Direct Composer commands are an advanced interoperability path; PAM is the supported application workflow.

## API guide

| API | Responsibility |
| --- | --- |
| `ShareInbox` | Drain process-safe items delivered by platform share surfaces. |
| `SharedItem` | Read normalized text, URL, or copied-file content. |
| `SharedItemKind` | Branch on the integer-backed item type. |

All coded states, kinds, and variants are sequential integer-backed enums. Use enum cases in application code; do not depend on raw wire numbers.

## Production checklist

- Drain after launch and every app resume.
- Move files that must survive after the successful drain.
- Validate MIME type, extension, content, and size before parsing or upload.
- Run `pam doctor`, `pam test`, and a signed release build on every supported platform.
- Exercise denial, cancellation, backgrounding, process restart, and offline behavior before release.

## Troubleshooting

- **iOS items do not arrive:** verify the shared app group on both targets.
- **Android app is absent from sharing:** inspect generated intent filters and accepted types.
- **A drained file disappears:** move it into application-owned storage before returning.
- **Native integration is stale:** run `pam doctor --fix`, rebuild the native host, and inspect the first reported diagnostic.

## Compatibility and support

This package targets PAM Native `0.8.x`, Android API 26+, and iOS 15+ unless a platform-specific section above states a stricter requirement. Platform SDKs, credentials, entitlements, physical hardware, and store configuration remain application responsibilities.

- [PAM documentation](https://push-in.github.io/pam-docs/introduction/)
- [PAM Native overview](https://push-in.github.io/pam-docs/native/overview/)
- [Plugin and native capability model](https://push-in.github.io/pam-docs/native/plugins/)
- [Report an issue](https://github.com/push-in/pam-native-share-extension/issues)

Security vulnerabilities should be reported through the repository security policy or GitHub private vulnerability reporting, not a public issue.
