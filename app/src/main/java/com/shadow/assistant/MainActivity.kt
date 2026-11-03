package com.shadow.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
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
        ChatMessage("assistant", "Hi, I'm Shadow. Tell me what you need, and I'll help with safe phone tasks.")
    )
    private val memories = mutableStateListOf<String>()
    private val activityLog = mutableStateListOf<String>()
    private var tts: TextToSpeech? = null

    private val speechResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!recognized.isNullOrBlank()) handleCommand(recognized)
        else respond("I didn't catch that. Type it in or try again.")
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchSpeechRecognition() else respond("Microphone permission is required for voice input.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val saved = getPreferences(MODE_PRIVATE).getStringSet("shadow_memory", emptySet()) ?: emptySet()
        memories.addAll(saved.sorted())

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Shadow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Your personal assistant for the phone", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFFF6F7FB), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(messages) { message ->
                                    val user = message.sender == "user"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (user) Color(0xFF3D7AFE) else Color.White,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = message.text,
                                                color = if (user) Color.White else Color(0xFF1F2937)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            label = { Text("Ask Shadow") },
                            placeholder = { Text("Try: open WhatsApp, search for weather, remember…") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val request = commandText.trim()
                                    commandText = ""
                                    if (request.isNotEmpty()) handleCommand(request)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Send") }

                            OutlinedButton(onClick = ::requestVoiceInput, modifier = Modifier.weight(1f)) { Text("Speak") }
                        }

                        Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { handleCommand("open WhatsApp") }) { Text("WhatsApp") }
                            OutlinedButton(onClick = { handleCommand("open camera") }) { Text("Camera") }
                            OutlinedButton(onClick = { handleCommand("search weather today") }) { Text("Weather") }
                        }

                        if (memories.isNotEmpty()) {
                            Text("Saved preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.fillMaxWidth()) {
                                items(memories) { memory -> Text("• $memory") }
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
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Shadow")
        }
        try {
            speechResult.launch(intent)
        } catch (_: Exception) {
            respond("Speech recognition is not available on this device right now.")
        }
    }

    private fun handleCommand(rawCommand: String) {
        val original = rawCommand.trim()
        if (original.isBlank()) return
        messages.add(ChatMessage("user", original))

        val cleaned = original
            .replace(Regex("^(hey|hello|hi|okay|ok),?\\s*shadow[, ]*", RegexOption.IGNORE_CASE), "")
            .trim()
            .removeSuffix(".")
            .removeSuffix("!")
            .removeSuffix("?")
        val lower = cleaned.lowercase(Locale.ROOT)

        when {
            lower in setOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening") ->
                respond("${greeting()}! I'm Shadow. How can I help you today?")

            lower in setOf("thanks", "thank you", "thx") -> respond("You're welcome. I'm happy to help.")
            lower in setOf("bye", "goodbye", "see you later") -> respond("Take care. I'll be here when you need me.")

            isMoneyTransferRequest(lower) -> respond("I can't make money transfers or process payments. I can help with safer tasks like opening apps, searching, or drafting messages.")

            lower in setOf("help", "what can you do", "what can you help with", "show commands") ->
                respond("I can open apps, search the web, open the dialer or camera, prepare a draft, set reminders, and remember preferences you teach me. I block money transfers and sensitive payment actions.")

            lower == "clear memory" || lower == "forget everything" || lower == "clear saved preferences" -> clearMemories()

            lower == "what do you remember" || lower == "show memory" || lower == "show saved preferences" -> {
                if (memories.isEmpty()) respond("I don't have any saved preferences yet. You can say, ‘Remember that I prefer short replies.’")
                else respond("I remember: ${memories.joinToString("; ")}")
            }

            startsWithAny(lower, "remember ", "save ", "remember that ", "save preference ") -> rememberPreference(cleaned)

            lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") -> {
                val appName = cleaned.replace(Regex("^(open|launch|start)\\s+", RegexOption.IGNORE_CASE), "").trim()
                if (appName.isBlank()) respond("Which app should I open?")
                else if (openApp(appName)) respond("Opening $appName.")
                else respond("I couldn't find $appName. Try the exact name from your app list.")
            }

            lower.startsWith("search ") || lower.startsWith("look up ") || lower.startsWith("google ") -> {
                val query = cleaned.replace(Regex("^(search|look up|google)\\s+", RegexOption.IGNORE_CASE), "").trim()
                if (query.isBlank()) respond("What do you want me to search for?")
                else { startWebSearch(query); respond("Searching for $query.") }
            }

            lower.startsWith("call ") || lower.startsWith("phone ") -> openDialer(cleaned.replace(Regex("^(call|phone)\\s+", RegexOption.IGNORE_CASE), "").trim())

            lower.startsWith("note ") || lower.startsWith("take note ") || lower.startsWith("write note ") -> saveNote(cleaned.replace(Regex("^(note|take note|write note)\\s+", RegexOption.IGNORE_CASE), "").trim())

            lower.startsWith("remind me") || lower.startsWith("set reminder") || lower.startsWith("reminder ") -> setReminder(cleaned)

            lower.startsWith("open maps") || lower.startsWith("map ") || lower.startsWith("navigate to ") -> openMaps(cleaned)

            lower.contains("camera") || lower.startsWith("take photo") || lower.startsWith("take a photo") -> openCamera()

            lower.contains("whatsapp") -> {
                if (openApp("WhatsApp")) respond("Opening WhatsApp. I can help with a draft, but I won't send messages without your review.")
                else respond("I couldn't find WhatsApp on this device.")
            }

            lower.startsWith("draft ") || lower.startsWith("write ") || lower.startsWith("compose ") || lower.startsWith("comment ") || lower.startsWith("message ") || lower.startsWith("text ") -> prepareDraft(cleaned)

            else -> respond("I can open apps, search the web, answer quick questions, create notes, set simple reminders, open maps, and remember your preferences. Try: ‘open WhatsApp’, ‘search for weather’, ‘remember that I prefer short replies’.")
        }
    }

    private fun isMoneyTransferRequest(text: String): Boolean {
        val pattern = Regex("\\b(send|transfer|pay|move|deposit|withdraw)\\b.*\\b(money|cash|funds|payment|rupees|dollars|₹|\\$)\\b|\\b(money transfer|send money|bank transfer|pay someone|transfer money)\\b")
        return pattern.containsMatchIn(text)
    }

    private fun startsWithAny(value: String, vararg prefixes: String): Boolean = prefixes.any { value.startsWith(it) }

    private fun rememberPreference(command: String) {
        val preference = command.replace(Regex("^(remember( that)?|save( preference)?)\\s*", RegexOption.IGNORE_CASE), "").trim()
        if (preference.isBlank()) {
            respond("What do you want me to remember?")
            return
        }
        if (!memories.any { it.equals(preference, ignoreCase = true) }) {
            memories.add(0, preference)
            getPreferences(MODE_PRIVATE).edit().putStringSet("shadow_memory", memories.toSet()).apply()
        }
        respond("Got it. I'll remember that preference on this phone.")
    }

    private fun saveNote(content: String) {
        if (content.isBlank()) {
            respond("What should I save in the note?")
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Save this note with an app"))
            respond("I prepared a note for you. Review and save it before continuing.")
        } catch (_: Exception) {
            respond("I couldn't open a note app on this device.")
        }
    }

    private fun setReminder(command: String) {
        val timeText = command.replace(Regex("^(remind me|set reminder|reminder)\\s*", RegexOption.IGNORE_CASE), "").trim()
        if (timeText.isBlank()) {
            respond("Tell me what to remind you about and when.")
            return
        }
        val reminderText = "Reminder: $timeText"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reminderText)
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Review your reminder"))
            respond("I prepared a reminder for you. Review it and save it in your reminder app.")
        } catch (_: Exception) {
            respond("I couldn't open a reminder app. You can still copy this reminder: $reminderText")
        }
    }

    private fun openMaps(command: String) {
        val place = when {
            command.startsWith("open maps") -> ""
            command.startsWith("map ") -> command.removePrefix("map ").trim()
            command.startsWith("navigate to ") -> command.removePrefix("navigate to ").trim()
            else -> ""
        }

        val uri = if (place.isBlank()) Uri.parse("geo:0,0?q=home")
        else Uri.parse("geo:0,0?q=${Uri.encode(place)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
            respond(if (place.isBlank()) "Opening maps." else "Opening maps for $place.")
        } catch (_: Exception) {
            respond("I couldn't open maps on this device.")
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            respond("Opening the camera.")
        } else {
            respond("The camera app isn't available on this device.")
        }
    }

    private fun prepareDraft(command: String) {
        val text = when {
            command.startsWith("draft ") -> command.removePrefix("draft ").trim()
            command.startsWith("compose ") -> command.removePrefix("compose ").trim()
            command.startsWith("comment ") -> command.removePrefix("comment ").trim()
            command.startsWith("message ") -> command.removePrefix("message ").trim()
            command.startsWith("text ") -> command.removePrefix("text ").trim()
            command.startsWith("write ") -> command.removePrefix("write ").trim()
            else -> command
        }

        if (text.isBlank()) {
            respond("What should the draft say?")
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            startActivity(Intent.createChooser(intent, "Review and choose where to send"))
            respond("I prepared a draft for you. Review it before sending or posting.")
        } catch (_: Exception) {
            respond("I couldn't open a share app for the draft.")
        }
    }

    private fun openDialer(contactOrNumber: String) {
        val number = contactOrNumber.trim()
        if (number.isBlank()) {
            respond("Who should I call?")
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        try {
            startActivity(intent)
            respond("I opened the dialer. Review the final number before placing the call.")
        } catch (_: Exception) {
            respond("I couldn't open the dialer on this device.")
        }
    }

    private fun startWebSearch(query: String): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
        true
    } catch (_: Exception) { false }

    private fun openApp(appName: String): Boolean {
        val normalized = appName.trim()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = packageManager.queryIntentActivities(launcherIntent, 0)
        val app = matches.firstOrNull { info ->
            val label = info.loadLabel(packageManager).toString()
            label.equals(normalized, ignoreCase = true) ||
                label.startsWith(normalized, ignoreCase = true) ||
                normalized.startsWith(label, ignoreCase = true)
        } ?: return false

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.activityInfo.packageName, app.activityInfo.name)
        }
        return try {
            startActivity(intent)
            true
        } catch (_: Exception) { false }
    }

    private fun greeting(): String {
        val current = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            current < 12 -> "Good morning"
            current < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun clearMemories() {
        memories.clear()
        getPreferences(MODE_PRIVATE).edit().remove("shadow_memory").apply()
        respond("I cleared the saved preferences on this phone.")
    }

    private fun respond(text: String) {
        activityLog.add(text)
        messages.add(ChatMessage("assistant", text))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shadow")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
