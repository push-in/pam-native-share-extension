# PAM Native Share Extension

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

This package targets PAM Native `0.6.x`, Android API 26+, and iOS 15+ unless a platform-specific section above states a stricter requirement. Platform SDKs, credentials, entitlements, physical hardware, and store configuration remain application responsibilities.

- [PAM documentation](https://push-in.github.io/pam-docs/introduction/)
- [PAM Native overview](https://push-in.github.io/pam-docs/native/overview/)
- [Plugin and native capability model](https://push-in.github.io/pam-docs/native/plugins/)
- [Report an issue](https://github.com/push-in/pam-native-share-extension/issues)

Security vulnerabilities should be reported through the repository security policy or GitHub private vulnerability reporting, not a public issue.
