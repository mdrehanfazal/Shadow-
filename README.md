# Shadow Android Assistant

Shadow is a safer, non-root Android assistant starter. It focuses on the kinds of tasks a personal phone assistant can do without bypassing Android's permissions or taking over the device.

## What Shadow can do

- Open installed apps such as WhatsApp, Maps, YouTube, or Chrome
- Speak aloud and listen by tapping the mic
- Search the web using the default browser
- Draft messages or comments for your review
- Start a call with the dialer
- Open the camera or other built-in app flows
- Remember preferences you teach it locally on the phone
- Keep a visible action log and permission-safe behavior

## What Shadow does not do

- No money transfer or bank payments
- No hidden background spying or secret screen capture without your explicit action
- No system-rooted takeover or bypass of Android's security model

## Running the app

1. Open the repo in Android Studio.
2. Let Gradle sync.
3. Run the app on an emulator or actual device.
4. Tap Speak to voice command it, or type a command in the box.

## Example commands

- Open WhatsApp
- Search for best budget travel in Goa
- Call mom
- Draft a message to team: running 10 minutes late
- Remember that I prefer short replies
- Open camera
- What can you do?

## Safety model

The design favors user approval and visible actions. For message sending, comment posting, or anything externally risky, Shadow prepares the action in an app chooser or opens the appropriate screen so you remain in control.
