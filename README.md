# PAM Native Share Extension

Receives text, URLs and sandboxed file copies from Android `ACTION_SEND`/`ACTION_SEND_MULTIPLE` and an iOS Share Extension. Call `ShareInbox::drain()` after launch or resume.

iOS requires the generated app and extension targets to share `group.<application-id>.pam-native`; the supplied entitlements use the `PAM_NATIVE_APPLICATION_ID` build setting. Never trust shared MIME types or file contents—validate them before processing or uploading.
