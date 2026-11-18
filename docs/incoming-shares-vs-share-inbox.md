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

## Lifetime of shared files

`IncomingShares` copies each shared file into the private file sandbox
(`pam-files/imports/incoming/<eventId>/`), because a `FileReference` must
resolve there. The system "Clear cache" does not reach that folder, so:

- Call `IncomingShares::release($share)` once the app is done with the files
  (published, sent, copied elsewhere or discarded). It deletes that share's
  folder; `$then` receives whether anything was deleted.
- Files that were never released are deleted on a later start, before the
  initial share is read, once they are older than
  `IncomingShareLimits::$retentionHours` (24 by default, 1–720). Copies left
  by older versions directly in `imports/` (`incoming-*`) are swept the same
  way.

```php
IncomingShares::initial($this->routeShare(...), new IncomingShareLimits(retentionHours: 24));
// after publishing or leaving the composer:
IncomingShares::release($share);
```
