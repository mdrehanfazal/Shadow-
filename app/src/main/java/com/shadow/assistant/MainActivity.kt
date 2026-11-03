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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private data class ChatMessage(val sender: String, val text: String)

    private var commandText by mutableStateOf("")
    private val messages = mutableStateListOf(
        ChatMessage("assistant", "Hi, I'm Shadow. I can open apps, draft messages, search, call, and learn your preferences.")
    )
    private val memories = mutableStateListOf<String>()
    private var tts: TextToSpeech? = null

    private val speechResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!recognized.isNullOrBlank()) {
            handleCommand(recognized)
        } else {
            respond("I couldn't catch that. Please type it or try again.")
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchSpeechRecognition()
        } else {
            respond("Microphone permission is needed for voice input.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val savedMemories = getPreferences(MODE_PRIVATE).getStringSet("shadow_memory", emptySet())
        memories.addAll(savedMemories.sorted())

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Shadow",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Your private assistant for the phone",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFFF6F7FB), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(messages) { message ->
                                    val isUser = message.sender == "user"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isUser) Color(0xFF3D7AFE) else Color.White,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = message.text,
                                                color = if (isUser) Color.White else Color(0xFF1F2937),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            label = { Text("Tell Shadow what to do") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (commandText.isNotBlank()) {
                                        handleCommand(commandText)
                                        commandText = ""
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Run")
                            }

                            OutlinedButton(
                                onClick = { requestVoiceInput() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Speak")
                            }
                        }

                        Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { handleCommand("Open WhatsApp") }) { Text("WhatsApp") }
                            OutlinedButton(onClick = { handleCommand("Open camera") }) { Text("Camera") }
                            OutlinedButton(onClick = { handleCommand("Search for weather today") }) { Text("Weather") }
                        }

                        if (memories.isNotEmpty()) {
                            Text("Saved memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(memories) { memory ->
                                    Text("• $memory", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        OutlinedButton(onClick = { clearMemories() }) {
                            Text("Clear saved memory")
                        }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Shadow")
        }

        try {
            speechResult.launch(intent)
        } catch (_: Exception) {
            respond("Speech recognition is not available on this device right now.")
        }
    }

    private fun handleCommand(rawCommand: String) {
        val command = rawCommand.trim()
        if (command.isBlank()) return

        messages.add(ChatMessage("user", command))
        val lower = command.lowercase(Locale.getDefault())

        when {
            lower.contains("money transfer") || lower.contains("transfer money") || lower.contains("pay someone") || lower.contains("bank transfer") -> {
                respond("I cannot make money transfers or bank payments. I can help with safer tasks like opening apps, drafting messages, or searching.")
            }

            lower.startsWith("remember ") || lower.startsWith("save ") -> {
                val memory = command.removePrefix("remember").removePrefix("save").trim().removePrefix("that").trim()
                if (memory.isBlank()) {
                    respond("Tell me what you want me to remember.")
                } else {
                    if (!memories.contains(memory)) {
                        memories.add(0, memory)
                        getPreferences(MODE_PRIVATE).edit().putStringSet("shadow_memory", memories.toSet()).apply()
                    }
                    respond("Saved. I'll remember that preference on this device.")
                }
            }

            lower == "clear memory" || lower == "forget everything" || lower.startsWith("clear saved memory") -> {
                clearMemories()
            }

            lower == "help" || lower == "what can you do" || lower == "commands" -> {
                respond("I can open apps, search the web, call, open camera, draft comments or messages, and remember your preferences. I block money transfer and bank actions.")
            }

            lower.startsWith("open ") -> {
                val appName = command.removePrefix("open").trim()
                if (appName.isBlank()) {
                    respond("Tell me which app you want to open.")
                } else if (openApp(appName)) {
                    respond("Opening $appName.")
                } else {
                    respond("I couldn't find an app named $appName. Try the exact app name or open it manually.")
                }
            }

            lower.startsWith("search ") -> {
                val query = command.removePrefix("search").trim()
                if (query.isBlank()) {
                    respond("What do you want me to search for?")
                } else {
                    val url = "https://www.google.com/search?q=${Uri.encode(query)}"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    respond("Searching for $query.")
                }
            }

            lower.startsWith("call ") -> {
                val nameOrNumber = command.removePrefix("call").trim()
                if (nameOrNumber.isBlank()) {
                    respond("Who do you want to call?")
                } else {
                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$nameOrNumber"))
                    startActivity(dial)
                    respond("Calling $nameOrNumber.")
                }
            }

            lower.startsWith("message ") || lower.startsWith("text ") -> {
                val target = command.removePrefix("message").removePrefix("text").trim()
                if (target.isBlank()) {
                    respond("Who do you want to message?")
                } else {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:")
                        putExtra("sms_body", "")
                    }
                    startActivity(Intent.createChooser(intent, "Choose an app to message"))
                    respond("I opened your messaging options for $target. Review it before sending.")
                }
            }

            lower.startsWith("draft ") || lower.startsWith("write ") || lower.startsWith("compose ") -> {
                val text = when {
                    lower.startsWith("draft ") -> command.removePrefix("draft").trim()
                    lower.startsWith("write ") -> command.removePrefix("write").trim()
                    lower.startsWith("compose ") -> command.removePrefix("compose").trim()
                    else -> command
                }
                if (text.isBlank()) {
                    respond("Tell me what the draft should say.")
                } else {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Review and share your draft"))
                    respond("I prepared the draft. Review it before sending or posting.")
                }
            }

            lower.startsWith("open camera") || lower.startsWith("take photo") || lower.startsWith("take a photo") -> {
                val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                if (cameraIntent.resolveActivity(packageManager) != null) {
                    startActivity(cameraIntent)
                    respond("Opening the camera.")
                } else {
                    respond("The camera app isn't available on this device.")
                }
            }

            lower.contains("whatsapp") -> {
                if (openApp("WhatsApp")) respond("Opening WhatsApp.") else respond("I couldn't find WhatsApp on this device.")
            }

            lower.contains("weather") -> {
                val query = if (command.contains(" for ")) command.substringAfter(" for ").trim() else "weather today"
                val url = "https://www.google.com/search?q=${Uri.encode(query)}"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                respond("Checking the weather.")
            }

            else -> {
                respond("I can open apps, search, call, draft content, and remember preferences. Example: open WhatsApp, search for travel, call mom, draft a message, remember that I prefer short replies.")
            }
        }
    }

    private fun respond(message: String) {
        messages.add(ChatMessage("assistant", message))
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "shadow")
    }

    private fun clearMemories() {
        memories.clear()
        getPreferences(MODE_PRIVATE).edit().remove("shadow_memory").apply()
        respond("I cleared the saved preferences on this device.")
    }

    private fun openApp(appName: String): Boolean {
        val normalized = appName.trim()
        val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val match = apps.firstOrNull { info ->
            val label = info.loadLabel(packageManager).toString()
            label.equals(normalized, ignoreCase = true) ||
                label.contains(normalized, ignoreCase = true) ||
                normalized.contains(label, ignoreCase = true)
        } ?: return false

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(match.activityInfo.packageName, match.activityInfo.name)
        }

        return try {
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
