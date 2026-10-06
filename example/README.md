# Share inbox demo

A one-screen PAM Native app for `pushinbr/pam-native-share-extension`. Share
text, links, photos or videos from any app (Android share sheet or the iOS
Share Extension) and they appear in the list. The inbox is drained at launch,
on every resume and on demand.

```bash
cd example
pam composer install
pam doctor --fix
pam dev            # or: pam build
```

`pam-native.json` accepts text, links, images and videos
(`plugins.shareExtension`), with several images or videos at once; remove
that block to accept every type.

The app installs the released package from Packagist. On iOS, register the
App Group `group.dev.pam.examples.share.pam-native` for the app and its
`.share` extension before building for a device. Prefer the core
`IncomingShares` API when you need shared files as sandbox `FileReference`s.
