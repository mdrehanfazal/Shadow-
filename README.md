# Shadow for Android

Shadow is a voice-first Android assistant starter app. It can listen when you tap the microphone, speak replies, open installed apps, prepare message drafts for your review, and remember preferences you explicitly teach it.

## Safety and scope

This is a non-root starter MVP, not unrestricted phone control. Android and other apps control what an assistant can see or automate. Shadow does not secretly monitor the screen, bypass permissions, send messages automatically, or perform money transfers. Drafts are handed to Android's share chooser so you can select the app and review/send them yourself.

## Build

1. Open this repository in the current stable Android Studio.
2. Let Gradle sync and install the Android SDK 35 platform if prompted.
3. Run the `app` configuration on an Android 8.0+ device or emulator.
4. Tap the microphone and grant microphone access to use speech recognition. Speech recognition availability and language depend on the device's installed services.

You can also build from a machine with Android SDK and Gradle available:

```sh
gradle assembleDebug
```

## Included

- Kotlin + Jetpack Compose interface
- Tap-to-talk speech recognition and spoken replies
- Opening installed apps by their displayed name
- Message draft handoff through Android's share sheet (you approve any sending)
- Explicitly taught preferences stored locally on the device, with a clear-memory action
- A basic action boundary that rejects money-transfer requests

## Planned next steps

Add user-started, Android-compliant screen sharing; calendar/reminder integrations; optional AI provider configuration; and a visible action history. Those integrations need additional permissions and should keep confirmation before external actions.
