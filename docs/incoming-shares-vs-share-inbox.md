# Core IncomingShares or ShareInbox?

Moved out of the README; nothing was removed.

> Since 1.46 the share APIs ship in this package: `IncomingShares`, `IncomingShare` and `IncomingShareLimits` live in `Pam\Native\ShareExtension` (see `src/`). The core names below remain as deprecated aliases in 1.46 and are removed in 1.47.

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

A runnable minimal app using `ShareInbox` is in [`example/`](../example).
