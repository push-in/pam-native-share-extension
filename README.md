# PAM Native Share Extension

Receives text, URLs and sandboxed file copies from Android `ACTION_SEND`/`ACTION_SEND_MULTIPLE` and an iOS Share Extension. Call `ShareInbox::drain()` after launch or resume.

iOS requires the generated app and extension targets to share `group.<application-id>.pam-native`; the supplied entitlements use the `PAM_NATIVE_APPLICATION_ID` build setting. Never trust shared MIME types or file contents—validate them before processing or uploading.

## Install

```bash
composer require pushinbr/pam-native-share-extension
pam mobile prepare
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
