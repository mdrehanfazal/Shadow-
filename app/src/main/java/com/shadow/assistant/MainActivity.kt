package com.shadow.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var spokenText by mutableStateOf("")
    private var statusText by mutableStateOf("Hi, I'm Shadow. What would you like to do?")
    private var tts: TextToSpeech? = null
    private val memories = mutableStateListOf<String>()

    private val speechResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val phrase = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!phrase.isNullOrBlank()) {
            spokenText = phrase
            handleCommand(phrase)
        } else {
            statusText = "I didn't catch that. Try again or type a request."
        }
    }

    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchSpeechRecognition() else statusText = "Microphone permission is needed for voice input."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        getPreferences(MODE_PRIVATE).getStringSet("memories", emptySet()).orEmpty()
            .sorted().forEach(memories::add)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Shadow", style = MaterialTheme.typography.headlineLarge)
                        Text("Your voice-first phone assistant", style = MaterialTheme.typography.titleMedium)
                        Text(statusText, style = MaterialTheme.typography.bodyLarge)

                        OutlinedTextField(
                            value = spokenText,
                            onValueChange = { spokenText = it },
                            label = { Text("Ask Shadow") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { submitTypedCommand() }) { Text("Do it") }
                            OutlinedButton(onClick = { requestVoiceInput() }) { Text("🎙 Speak") }
                        }

                        Text("Teach Shadow a preference", style = MaterialTheme.typography.titleMedium)
                        Text("Example: Remember that I prefer short replies")
                        Text("Memories are saved locally and can be cleared below.", style = MaterialTheme.typography.bodySmall)
                        if (memories.isEmpty()) Text("No saved preferences yet.")
                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            items(memories) { memory -> Text("• $memory", modifier = Modifier.padding(vertical = 3.dp)) }
                        }
                        Spacer(Modifier.height(2.dp))
                        OutlinedButton(onClick = { clearMemories() }) { Text("Clear saved preferences") }
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    private fun requestVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognition()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
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
            statusText = "Speech recognition isn't available on this device. You can type instead."
        }
    }

    private fun submitTypedCommand() {
        if (spokenText.isNotBlank()) handleCommand(spokenText)
    }

    private fun handleCommand(raw: String) {
        val command = raw.trim()
        val lower = command.lowercase(Locale.getDefault())

        if (listOf("send money", "transfer money", "money transfer", "pay someone", "bank transfer").any(lower::contains)) {
            reply("I can't make or initiate money transfers.")
            return
        }

        if (lower.startsWith("remember that ") || lower.startsWith("remember ")) {
            val memory = command.substringAfter("remember", "").trim().removePrefix("that ").trim()
            if (memory.isBlank()) {
                reply("Tell me the preference you'd like me to remember.")
            } else {
                memories.remove(memory)
                memories.add(0, memory)
                getPreferences(MODE_PRIVATE).edit().putStringSet("memories", memories.toSet()).apply()
                reply("Okay, I'll remember that preference on this device. You can clear it any time.")
            }
            return
        }

        if (lower.startsWith("draft ") || lower.startsWith("write a message ") || lower.startsWith("write a comment ")) {
            val draft = when {
                lower.startsWith("draft ") -> command.drop(6).trim()
                lower.startsWith("write a message ") -> command.drop("write a message ".length).trim()
                else -> command.drop("write a comment ".length).trim()
            }
            if (draft.isBlank()) {
                reply("Tell me what you'd like in the draft.")
            } else {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, draft)
                }
                startActivity(Intent.createChooser(share, "Review and choose where to share"))
                reply("I've prepared the draft. Review it in the app before sending or posting.")
            }
            return
        }

        if (lower.startsWith("open ")) {
            val requested = command.drop(5).trim()
            if (openInstalledApp(requested)) reply("Opening $requested.")
            else reply("I couldn't find an app called $requested. Check the name or open it manually.")
            return
        }

        reply("I can open apps, prepare message or comment drafts, and remember preferences. Try: open WhatsApp, draft Hello, or remember that I prefer short replies.")
    }

    private fun openInstalledApp(requestedName: String): Boolean {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = packageManager.queryIntentActivities(launcher, 0).firstOrNull { info ->
            val label = info.loadLabel(packageManager).toString()
            label.equals(requestedName, ignoreCase = true) || label.startsWith(requestedName, ignoreCase = true)
        } ?: return false

        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(match.activityInfo.packageName, match.activityInfo.name)
        return try {
            startActivity(launch)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun clearMemories() {
        memories.clear()
        getPreferences(MODE_PRIVATE).edit().remove("memories").apply()
        reply("I've cleared the preferences saved by Shadow.")
    }

    private fun reply(message: String) {
        statusText = message
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "shadow-reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
