# Android share receiving with limits (0.4.4)

Moved out of the README; nothing was removed. Translated from Portuguese.

Configure the limits on the first read, before the listener. The Activity callback only marks the intent and stores the request; a bounded queue performs the copies off the main thread. File order is preserved and the cut happens before copying. Any video MIME type selects `maximumVideoItems`, including in mixed shares.

```php
IncomingShares::initial($onShare, new IncomingShareLimits(
    maximumBytes: 314_572_800,
    maximumItems: 20,
    maximumVideoItems: 1,
));
IncomingShares::listen($onShare);
```

Import these classes from `Pam\Native\ShareExtension`. `maximumBytes` is positive; `maximumItems` accepts 1–32; `maximumVideoItems` accepts 1–`maximumItems`. The 300 MiB per-file limit is the default. Streaming uses a 64 KiB buffer and checks the real size, even without reliable metadata. A failure removes the files copied in that request.

`IncomingShare::eventId` identifies the intent. A new intentional share gets another ID, even with the same files. A recreation that keeps the intent and its extras keeps the ID and the consumption marker; the request is received only once. There is no delivery guarantee after process death during a copy. The legacy `ShareInbox` API uses the same default limits and consumption marker.

Size/copy/queue failures arrive as `IncomingShareException`, with `reason` (`IncomingShareError::FileTooLarge`, `CopyFailed`, `QueueFull`) and `eventId`. The native codes are the integers 1, 2 and 3. Exceptions from asynchronous callbacks follow the runtime's `App::onError()` handling; after an input failure, the listener is re-armed to receive the next share. These limits and the new identifier are implemented on Android; the PHP contract keeps the previous constructor compatible.

Local tests without Gradle (JDK, Android SDK 35 and Kotlin/JUnit jars already available in the cache):

```sh
PAM_NATIVE_SOURCE=/path/to/pam-native python3 tests/run-jvm.py
PAM_NATIVE_PHP_ROOT=/path/to/pam-native/packages/native/src php tests/run.php
```

Isolated Android harness (tests `Activity.recreate()`, copying with wrong metadata, partial removal, order and real limits):

```sh
PAM_NATIVE_SOURCE=/path/to/pam-native python3 tests/build-device-test.py
# Under the exclusive emulator lock, install build/device/test.apk and open:
adb -s emulator-5554 install -r build/device/test.apk
adb -s emulator-5554 shell am start -n dev.pam.sharetest/dev.pam.shareextension.ShareDeviceTestActivity
adb -s emulator-5554 logcat -d -s PamShareN6:I '*:S'
adb -s emulator-5554 uninstall dev.pam.sharetest
```

The harness build uses SDK Build Tools 36.1.0, without Gradle. The expected result ends in `ALL_PASS`. This APK is for tests only.
