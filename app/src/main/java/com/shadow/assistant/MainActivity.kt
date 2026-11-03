package com.shadow.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private data class ChatMessage(val sender: String, val text: String)

    private var commandText by mutableStateOf("")
    private val messages = mutableStateListOf(
        ChatMessage("assistant", "Hi, I'm Shadow. Tell me what you need, or tap Speak. I can open apps, search, and help prepare things for you.")
    )
    private val memories = mutableStateListOf<String>()
    private var tts: TextToSpeech? = null

    private val speechResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!recognized.isNullOrBlank()) handleCommand(recognized)
        else respond("I didn't catch that. Want to try again or type it?")
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchSpeechRecognition()
        else respond("Microphone access is needed for voice input. You can still type to me.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        getPreferences(MODE_PRIVATE).getStringSet("shadow_memory", emptySet())
            .orEmpty().sorted().forEach(memories::add)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Shadow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("A helpful assistant, on your terms", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)

                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .background(Color(0xFFF6F7FB), RoundedCornerShape(18.dp)).padding(12.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(messages) { message ->
                                    val user = message.sender == "user"
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                                        Box(
                                            Modifier.background(if (user) Color(0xFF3D7AFE) else Color.White, RoundedCornerShape(16.dp))
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(message.text, color = if (user) Color.White else Color(0xFF1F2937))
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            label = { Text("Talk to Shadow") },
                            placeholder = { Text("Try ‘open WhatsApp’ or ‘search for…’") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { val text = commandText; commandText = ""; handleCommand(text) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Send") }
                            OutlinedButton(onClick = ::requestVoiceInput, modifier = Modifier.weight(1f)) { Text("Speak") }
                        }
                        Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { handleCommand("open WhatsApp") }) { Text("WhatsApp") }
                            OutlinedButton(onClick = { handleCommand("open camera") }) { Text("Camera") }
                            OutlinedButton(onClick = { handleCommand("search for weather today") }) { Text("Weather") }
                        }
                        if (memories.isNotEmpty()) {
                            Text("What Shadow remembers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.fillMaxWidth().weight(0.35f, fill = false)) {
                                items(memories) { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                        OutlinedButton(onClick = ::clearMemories) { Text("Clear saved preferences") }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(0.96f)
            tts?.setPitch(1.02f)
        }
    }

    private fun requestVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) launchSpeechRecognition()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Shadow")
        }
        try { speechResult.launch(intent) }
        catch (_: Exception) { respond("Speech recognition isn't available right now. You can type your request instead.") }
    }

    private fun handleCommand(rawCommand: String) {
        val original = rawCommand.trim()
        if (original.isBlank()) return
        messages.add(ChatMessage("user", original))
        val command = original
            .replace(Regex("^(hey|okay|ok),?\\s+shadow[, ]*", RegexOption.IGNORE_CASE), "")
            .trim().trimEnd('.', '!', '?', ',')
        val lower = command.lowercase(Locale.ROOT)

        when {
            lower in setOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening") ->
                respond("${greeting()}, I'm here. What can I help you with?")

            lower in setOf("thanks", "thank you", "thx") -> respond("You're welcome.")
            lower in setOf("bye", "goodbye", "see you") -> respond("Take care. I'll be here when you need me.")

            isMoneyTransferRequest(lower) ->
                respond("I can't make or initiate a money transfer. I can help with other tasks, but you'll need to handle payments yourself.")

            lower in setOf("help", "what can you do", "what can you help with", "commands") ->
                respond("I can open apps, search the web, open the dialer or camera, prepare a message or comment draft, and remember preferences you teach me. I ask you to review anything before it is sent or posted.")

            lower == "clear memory" || lower == "forget everything" || lower == "clear saved memory" -> clearMemories()
            lower == "what do you remember" || lower == "show memory" -> {
                if (memories.isEmpty()) respond("I don't have any saved preferences yet. You can say, ‘Remember that I prefer short replies.’")
                else respond("Here's what I remember: ${memories.joinToString("; ")}.")
            }

            startsWithAny(lower, "remember ", "save preference ", "remember that ") -> rememberPreference(command)

            lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start app ") || lower.startsWith("go to app ") -> {
                val appName = command.replace(Regex("^(open|launch|start app|go to app)\\s+", RegexOption.IGNORE_CASE), "").trim()
                if (appName.isBlank()) respond("Which app should I open?")
                else if (openApp(appName)) respond("Opening $appName.")
                else respond("I couldn't find $appName among your installed apps. Check the app name and try again.")
            }

            lower.startsWith("search") || lower.startsWith("look up ") || lower.startsWith("google ") -> {
                val query = command.replace(Regex("^(search( for)?|look up|google)\\s*", RegexOption.IGNORE_CASE), "").trim()
                if (query.isBlank()) respond("What would you like me to search for?")
                else if (startWebSearch(query)) respond("Here's a web search for $query.")
                else respond("I couldn't open a browser on this device.")
            }

            lower.startsWith("call ") || lower.startsWith("phone ") -> openDialer(command.substringAfter(' ').trim())

            lower.startsWith("draft ") || lower.startsWith("write ") || lower.startsWith("compose ") ||
                lower.startsWith("comment ") || lower.startsWith("message ") || lower.startsWith("text ") -> prepareDraft(command)

            lower.contains("camera") || lower.startsWith("take a photo") || lower.startsWith("take photo") -> openCamera()

            lower.contains("whatsapp") -> {
                if (openApp("WhatsApp")) respond("Opening WhatsApp. I can't see its conversations from here, but I can help you prepare a reply if you tell me what you'd like to say.")
                else respond("I couldn't find WhatsApp on this device.")
            }

            lower.contains("weather") -> {
                val query = if (lower.startsWith("what's") || lower.startsWith("what is") || lower.startsWith("is it")) command else "weather ${command.substringAfter("weather", "today").trim()}"
                if (startWebSearch(query)) respond("I'll look that up for you.") else respond("I couldn't open a browser on this device.")
            }

            else -> respond("I'm not sure how to do that yet. I can open apps, search, open the dialer or camera, draft text, and remember preferences. Say ‘help’ for examples.")
        }
    }

    private fun isMoneyTransferRequest(text: String): Boolean {
        val money = Regex("\\b(send|transfer|pay|move)\\b.*\\b(money|cash|funds|payment|rupees|dollars|₹|\\$)\\b|\\b(bank transfer|money transfer|send money|pay someone)\\b")
        return money.containsMatchIn(text)
    }

    private fun startsWithAny(value: String, vararg prefixes: String) = prefixes.any(value::startsWith)

    private fun rememberPreference(command: String) {
        val preference = command.replace(Regex("^(remember( that)?|save preference)\\s*", RegexOption.IGNORE_CASE), "").trim()
        if (preference.isBlank()) respond("What would you like me to remember?")
        else {
            val existing = memories.firstOrNull { it.equals(preference, ignoreCase = true) }
            if (existing == null) memories.add(0, preference)
            getPreferences(MODE_PRIVATE).edit().putStringSet("shadow_memory", memories.toSet()).apply()
            respond("Got it. I'll remember that on this phone. You can ask what I remember or clear it any time.")
        }
    }

    private fun prepareDraft(command: String) {
        val messageMatch = Regex("^(?:draft|write|compose|message|text)\\s+(?:a\\s+)?(?:message|text)\\s+to\\s+(.+?)\\s+(?:saying|that)\\s+(.+)$", RegexOption.IGNORE_CASE).find(command)
        val commentMatch = Regex("^(?:draft|write|compose|comment)\\s+(?:a\\s+)?comment\\s+(?:saying\\s+)?(.+)$", RegexOption.IGNORE_CASE).find(command)
        val content = when {
            messageMatch != null -> "To ${messageMatch.groupValues[1]}: ${messageMatch.groupValues[2]}"
            commentMatch != null -> commentMatch.groupValues[1]
            else -> command.replace(Regex("^(draft|write|compose|comment|message|text)\\s*", RegexOption.IGNORE_CASE), "").trim()
        }
        if (content.isBlank()) {
            respond("What should the draft say?")
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        try {
            startActivity(Intent.createChooser(intent, "Review your draft and choose an app"))
            respond("I've prepared that draft. Please check the recipient and text in the app before sending or posting.")
        } catch (_: Exception) { respond("I couldn't open an app to share the draft.") }
    }

    private fun openDialer(contactOrNumber: String) {
        if (contactOrNumber.isBlank()) {
            respond("Who would you like to call?")
            return
        }
        val isNumber = contactOrNumber.matches(Regex("[+0-9() .-]{3,}"))
        val intent = if (isNumber) Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contactOrNumber, null))
        else Intent(Intent.ACTION_DIAL)
        try {
            startActivity(intent)
            respond(if (isNumber) "I opened the dialer with that number. Please review it before placing the call." else "I opened the dialer. Please choose $contactOrNumber from your contacts and confirm the call.")
        } catch (_: Exception) { respond("I couldn't open the phone dialer.") }
    }

    private fun startWebSearch(query: String): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
        true
    } catch (_: Exception) { false }

    private fun openCamera() {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        try {
            startActivity(intent)
            respond("Opening the camera.")
        } catch (_: Exception) { respond("The camera app isn't available on this device.") }
    }

    private fun openApp(appName: String): Boolean {
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = packageManager.queryIntentActivities(launchIntent, 0)
        val match = matches
            .map { it to it.loadLabel(packageManager).toString() }
            .sortedBy { (_, label) -> if (label.equals(appName, true)) 0 else if (label.startsWith(appName, true)) 1 else 2 }
            .firstOrNull { (_, label) -> label.equals(appName, true) || label.startsWith(appName, true) }
            ?.first ?: return false
        return try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(match.activityInfo.packageName, match.activityInfo.name))
            true
        } catch (_: Exception) { false }
    }

    private fun greeting(): String {
        val name = memories.firstNotNullOfOrNull { Regex("\\bcall me\\s+(.+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
        return if (name.isNullOrBlank()) "Hello" else "Hello, $name"
    }

    private fun clearMemories() {
        memories.clear()
        getPreferences(MODE_PRIVATE).edit().remove("shadow_memory").apply()
        respond("Done. I've cleared the preferences saved by Shadow on this phone.")
    }

    private fun respond(text: String) {
        messages.add(ChatMessage("assistant", text))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shadow-${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
