# Shadow Android Assistant

Shadow is a non-root Android assistant starter with a conversational interface. Talk by typing or tapping **Speak**; speech recognition and text-to-speech use the Android services available on your device.

## Current features

- Natural-language matching for common greetings and simple commands
- Open installed apps by name (for example, “Hey Shadow, open WhatsApp”)
- Web search through the browser
- Open the dialer for a number or a contact name; you confirm the call
- Prepare message/comment text in Android's share chooser; review it before sending or posting
- Open the camera
- Save, list, and clear preferences stored locally on the device
- Block explicit money-transfer requests
- Friendly spoken replies with adjustable Android text-to-speech voice settings

## Build and run

1. Open the repository in a current Android Studio release.
2. Sync Gradle and install Android SDK 35 if prompted.
3. Run the `app` configuration on an Android 8.0+ device or emulator.
4. Grant microphone permission when you tap **Speak**. The speech service and supported languages vary by device.

## Example requests

- “Hey Shadow, open WhatsApp”
- “Search for budget phones under 200”
- “Call +1 555 010 1234” (opens the dialer; you place the call)
- “Draft a message to Alex saying I'll be 10 minutes late”
- “Write a comment saying great video”
- “Remember that I prefer short replies”
- “What do you remember?”
- “Forget everything”

## Limits and safety

This MVP is not connected to an AI model and does not retrain itself. Its “learning” is explicit user-taught preferences saved locally. It cannot see inside other apps, read WhatsApp conversations, or silently watch the screen. App integrations and screen assistance require user-visible permissions and may not be available in every app. It does not send messages, post comments, place calls directly, or initiate money transfers; those actions require you to review and confirm in the relevant app.
