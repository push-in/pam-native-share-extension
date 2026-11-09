# Native integration (Android and iOS)

Moved out of the README; nothing was removed.

## Android

PAM Native adds one intent filter per accepted type to `PamActivity`:
`android.intent.action.SEND`, plus `SEND_MULTIPLE` for the types in
`multiple`. The default is `*/*` with both actions. No permissions are needed:
shared content URIs are copied into the app sandbox. Types listed in the
core `android.shareTargets` are merged into the same filters.

## iOS

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
