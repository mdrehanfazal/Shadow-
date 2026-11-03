# Shadow Android Assistant

Shadow is a safe, non-root Android assistant starter. It is designed to help with common phone tasks without bypassing Android's security or taking over the device.

## Features in this version

- Natural conversational commands and greetings
- App launching with voice or typed input
- Web search through the browser
- Dialer support with a final review before calling
- Camera access
- Map navigation support
- Draft generation for texts, messages, and comments
- Notes and reminder-style prompt generation
- Local memory with user-defined preferences
- Safe handling of sensitive requests, including payment and money transfer blocking

## Example commands

- “Hey Shadow, open WhatsApp”
- “Search for weather today”
- “Call 9876543210”
- “Take a photo”
- “Navigate to Delhi”
- “Remember that I prefer short replies”
- “What do you remember?”
- “Draft a message to Alex saying I'll be 10 minutes late”
- “Take note buy milk and charger”
- “Set reminder call mom at 7 pm”

## Safety model

Shadow is intentionally limited to what Android allows by default. It cannot secretly read your screen, open every app without review, or handle money transfers. Sensitive actions are blocked or routed through the system app in a user-review flow.

## Run the app

1. Open the repository in Android Studio.
2. Let Gradle sync.
3. Choose an Android emulator or device.
4. Tap Speak or type a request.

## Notes

This is still a local, rule-based assistant MVP. It does not yet connect to a cloud LLM or full AI model, but it gives a strong foundation for a personal Android voice assistant.
