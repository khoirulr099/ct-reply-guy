package com.example

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.OpenAIContentRequest
import com.example.api.OpenAIMessage
import com.example.data.ReplyHistory
import com.example.data.ReplyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ReplyRepository, private val context: Context) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "degenreply_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_ACTIVE_MODEL = "active_model"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_API_BASE_URL = "api_base_url"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun saveApiKey(key: String) {
        sharedPrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    private fun saveSettings() {
        sharedPrefs.edit()
            .putString(KEY_ACTIVE_MODEL, _activeModel.value)
            .putString(KEY_SELECTED_MODEL, _selectedModel.value)
            .apply()
    }

    // Inputs
    private val _tweetInput = MutableStateFlow("")
    val tweetInput = _tweetInput.asStateFlow()

    private val _selectedModel = MutableStateFlow(sharedPrefs.getString(KEY_SELECTED_MODEL, "Gemini 3.5 Flash") ?: "Gemini 3.5 Flash")
    val selectedModel = _selectedModel.asStateFlow()

    private val _selectedTone = MutableStateFlow("Degen")
    val selectedTone = _selectedTone.asStateFlow()

    // Core configurable properties
    private val _activeApiKey = MutableStateFlow(sharedPrefs.getString(KEY_API_KEY, "") ?: "")
    val activeApiKey = _activeApiKey.asStateFlow()

    private val _activeModel = MutableStateFlow(sharedPrefs.getString(KEY_ACTIVE_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash")
    val activeModel = _activeModel.asStateFlow()

    private val _apiBaseUrl = MutableStateFlow(sharedPrefs.getString(KEY_API_BASE_URL, "") ?: "")
    val apiBaseUrl = _apiBaseUrl.asStateFlow()

    init {
        val currentKey = sharedPrefs.getString(KEY_API_KEY, "") ?: ""
        if (currentKey == "default_system_key" || currentKey.contains("default_system_key")) {
            sharedPrefs.edit().putString(KEY_API_KEY, "").apply()
            _activeApiKey.value = ""
        }
    }

    // Generation State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _generatedReply = MutableStateFlow<String?>(null)
    val generatedReply = _generatedReply.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError = _apiError.asStateFlow()

    // History Log flow
    val historyLog: StateFlow<List<ReplyHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onTweetInputChanged(input: String) {
        _tweetInput.value = input
    }

    fun onModelSelected(model: String) {
        _selectedModel.value = model
        _activeModel.value = when (model) {
            "Gemini 2.5 Pro" -> "gemini-2.5-pro"
            "Gemini 2.5 Flash" -> "gemini-2.5-flash"
            "Gemini 2.0 Flash" -> "gemini-2.0-flash"
            "Gemini 1.5 Pro" -> "gemini-1.5-pro"
            "Gemini 1.5 Flash" -> "gemini-1.5-flash"
            "Gemini 3.5 Flash" -> "gemini-3.5-flash"
            "Gemini 3.1 Pro Preview" -> "gemini-3.1-pro-preview"
            "Gemini 3.1 Flash Lite Preview" -> "gemini-3.1-flash-lite-preview"
            "GPT-4o" -> "gpt-4o"
            "GPT-4o Mini" -> "gpt-4o-mini"
            "Claude 3.5 Sonnet" -> "claude-3-5-sonnet-latest"
            "Claude 3.5 Haiku" -> "claude-3-5-haiku-latest"
            "DeepSeek V3" -> "deepseek-chat"
            "DeepSeek R1" -> "deepseek-reasoner"
            "DeepSeek V3 (OpenRouter)" -> "deepseek/deepseek-chat"
            "DeepSeek R1 (OpenRouter)" -> "deepseek/deepseek-r1"
            "Claude 3.5 Sonnet (OpenRouter)" -> "anthropic/claude-3.5-sonnet"
            "Claude 3.5 Haiku (OpenRouter)" -> "anthropic/claude-3.5-haiku"
            "Gemini 2.5 Pro (OpenRouter)" -> "google/gemini-2.5-pro"
            "Gemini 2.5 Flash (OpenRouter)" -> "google/gemini-2.5-flash"
            else -> model.lowercase().replace(" ", "-").replace("(", "").replace(")", "")
        }
        saveSettings()
    }

    fun onToneSelected(tone: String) {
        _selectedTone.value = tone
    }

    fun updateActiveApiKey(key: String) {
        val trimmedKey = key.trim()
        _activeApiKey.value = trimmedKey
        saveApiKey(trimmedKey)
    }

    fun updateApiBaseUrl(url: String) {
        val trimmed = url.trim()
        _apiBaseUrl.value = trimmed
        sharedPrefs.edit().putString(KEY_API_BASE_URL, trimmed).apply()
    }

    fun updateActiveModel(modelName: String) {
        _activeModel.value = modelName
        val matchedUiLabel = when (modelName.lowercase()) {
            "gemini-2.5-pro", "gemini_2.5_pro" -> "Gemini 2.5 Pro"
            "gemini-2.5-flash", "gemini_2.5_flash" -> "Gemini 2.5 Flash"
            "gemini-2.0-flash", "gemini_2.0_flash" -> "Gemini 2.0 Flash"
            "gemini-1.5-pro", "gemini_1.5_pro" -> "Gemini 1.5 Pro"
            "gemini-1.5-flash", "gemini_1.5_flash" -> "Gemini 1.5 Flash"
            "gemini-3.5-flash", "gemini_3.5_flash" -> "Gemini 3.5 Flash"
            "gemini-3.1-pro-preview", "gemini_3.1_pro_preview" -> "Gemini 3.1 Pro Preview"
            "gemini-3.1-flash-lite-preview", "gemini_3.1_flash_lite_preview" -> "Gemini 3.1 Flash Lite Preview"
            "gpt-4o" -> "GPT-4o"
            "gpt-4o-mini" -> "GPT-4o Mini"
            "claude-3-5-sonnet-latest", "claude-3.5-sonnet", "claude_3.5_sonnet" -> "Claude 3.5 Sonnet"
            "claude-3-5-haiku-latest", "claude-3.5-haiku", "claude_3.5_haiku" -> "Claude 3.5 Haiku"
            "deepseek-chat" -> "DeepSeek V3"
            "deepseek-reasoner" -> "DeepSeek R1"
            "deepseek/deepseek-chat" -> "DeepSeek V3 (OpenRouter)"
            "deepseek/deepseek-r1" -> "DeepSeek R1 (OpenRouter)"
            "anthropic/claude-3.5-sonnet" -> "Claude 3.5 Sonnet (OpenRouter)"
            "anthropic/claude-3.5-haiku" -> "Claude 3.5 Haiku (OpenRouter)"
            "google/gemini-2.5-pro" -> "Gemini 2.5 Pro (OpenRouter)"
            "google/gemini-2.5-flash" -> "Gemini 2.5 Flash (OpenRouter)"
            else -> modelName
        }
        _selectedModel.value = matchedUiLabel
        saveSettings()
    }

    fun resetToDefaultSettings() {
        _activeApiKey.value = ""
        _activeModel.value = "gemini-2.5-flash"
        _selectedModel.value = "Gemini 2.5 Flash"
        _apiBaseUrl.value = ""
        saveApiKey("")
        sharedPrefs.edit().putString(KEY_API_BASE_URL, "").apply()
        saveSettings()
    }

    fun clearInput() {
        _tweetInput.value = ""
        _generatedReply.value = null
        _apiError.value = null
    }

    fun clearError() {
        _apiError.value = null
    }

    fun generateReplyGuyResponse() {
        val input = _tweetInput.value.trim()
        if (input.isEmpty()) {
            _apiError.value = "tweet content cannot be empty, anon!"
            return
        }

        // Configuration Command Interception
        if (input.startsWith("[menu:") && input.endsWith("]")) {
            val commandContent = input.substring(6, input.length - 1).trim()
            _isGenerating.value = false
            _apiError.value = null
            
            when {
                commandContent.startsWith("insert api key =") -> {
                    val keyVal = commandContent.substringAfter("insert api key =").trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    _activeApiKey.value = keyVal
                    saveApiKey(keyVal)
                    _generatedReply.value = "status: universal api key registered successfully."
                }
                commandContent.startsWith("insert api base =") -> {
                    val baseVal = commandContent.substringAfter("insert api base =").trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    _apiBaseUrl.value = baseVal
                    sharedPrefs.edit().putString(KEY_API_BASE_URL, baseVal).apply()
                    _generatedReply.value = "status: custom api base URL registered."
                }
                commandContent.startsWith("select model =") -> {
                    val modelVal = commandContent.substringAfter("select model =").trim()
                        .removeSurrounding("\"").removeSurrounding("'")
                    _activeModel.value = modelVal
                    val matchedUiLabel = when (modelVal.lowercase()) {
                        "gemini-2.5-pro", "gemini_2.5_pro" -> "Gemini 2.5 Pro"
                        "gemini-2.5-flash", "gemini_2.5_flash" -> "Gemini 2.5 Flash"
                        "gemini-2.0-flash", "gemini_2.0_flash" -> "Gemini 2.0 Flash"
                        "gemini-1.5-pro", "gemini_1.5_pro" -> "Gemini 1.5 Pro"
                        "gemini-1.5-flash", "gemini_1.5_flash" -> "Gemini 1.5 Flash"
                        "gemini-3.5-flash", "gemini_3.5_flash" -> "Gemini 3.5 Flash"
                        "gemini-3.1-pro-preview", "gemini_3.1_pro_preview" -> "Gemini 3.1 Pro Preview"
                        "gemini-3.1-flash-lite-preview", "gemini_3.1_flash_lite_preview" -> "Gemini 3.1 Flash Lite Preview"
                        "gpt-4o" -> "GPT-4o"
                        "gpt-4o-mini" -> "GPT-4o Mini"
                        "claude-3-5-sonnet-latest", "claude-3.5-sonnet", "claude_3.5_sonnet" -> "Claude 3.5 Sonnet"
                        "claude-3-5-haiku-latest", "claude-3.5-haiku", "claude_3.5_haiku" -> "Claude 3.5 Haiku"
                        "deepseek-chat" -> "DeepSeek V3"
                        "deepseek-reasoner" -> "DeepSeek R1"
                        "deepseek/deepseek-chat" -> "DeepSeek V3 (OpenRouter)"
                        "deepseek/deepseek-r1" -> "DeepSeek R1 (OpenRouter)"
                        "anthropic/claude-3.5-sonnet" -> "Claude 3.5 Sonnet (OpenRouter)"
                        "anthropic/claude-3.5-haiku" -> "Claude 3.5 Haiku (OpenRouter)"
                        "google/gemini-2.5-pro" -> "Gemini 2.5 Pro (OpenRouter)"
                        "google/gemini-2.5-flash" -> "Gemini 2.5 Flash (OpenRouter)"
                        else -> modelVal
                    }
                    _selectedModel.value = matchedUiLabel
                    saveSettings()
                    _generatedReply.value = "status: switched engine to $modelVal."
                }
                commandContent == "show status" -> {
                    _generatedReply.value = "active model: ${_activeModel.value} | api key: ${_activeApiKey.value} | base: ${_apiBaseUrl.value}"
                }
                commandContent == "reset to default" -> {
                    _activeApiKey.value = ""
                    _activeModel.value = "gemini-2.5-flash"
                    _selectedModel.value = "Gemini 2.5 Flash"
                    _apiBaseUrl.value = ""
                    saveApiKey("")
                    sharedPrefs.edit().putString(KEY_API_BASE_URL, "").apply()
                    saveSettings()
                    _generatedReply.value = "status: configuration reset successfully."
                }
                else -> {
                    _apiError.value = "unknown configuration command: $commandContent"
                }
            }
            return
        }

        _isGenerating.value = true
        _apiError.value = null
        _generatedReply.value = null

        viewModelScope.launch {
            try {
                val apiKey = _activeApiKey.value.trim()
                if (apiKey.isEmpty()) {
                    _apiError.value = "api key belum dimasukkan! silakan isi api key anda di tab pengaturan."
                    _isGenerating.value = false
                    return@launch
                }

                val systemInstructionStr = buildSystemInstruction(_selectedModel.value, _selectedTone.value)
                val userPrompt = "---\n$input\n---"

                // Check if we should use OpenAI / OpenRouter / Custom Compat base URL completions
                val isSkKey = apiKey.startsWith("sk-") || _apiBaseUrl.value.isNotEmpty()
                if (isSkKey) {
                    val finalUrl = if (_apiBaseUrl.value.isNotEmpty()) {
                        val base = _apiBaseUrl.value
                        if (base.endsWith("/chat/completions")) base
                        else if (base.endsWith("/")) "${base}chat/completions"
                        else "${base}/chat/completions"
                    } else {
                        // Guess URL based on model name or API Key prefix
                        if (_activeModel.value.contains("/") || apiKey.startsWith("sk-or-")) {
                            "https://openrouter.ai/api/v1/chat/completions"
                        } else if (_activeModel.value.startsWith("deepseek")) {
                            "https://api.deepseek.com/v1/chat/completions"
                        } else {
                            "https://api.openai.com/v1/chat/completions"
                        }
                    }

                    // Translate standard models to OpenRouter or specific platform names if needed
                    val finalModelName = if (finalUrl.contains("openrouter.ai")) {
                        when (_activeModel.value) {
                            "gemini-2.5-pro" -> "google/gemini-2.5-pro"
                            "gemini-2.5-flash" -> "google/gemini-2.5-flash"
                            "gemini-2.0-flash" -> "google/gemini-2.0-flash"
                            "gemini-1.5-pro" -> "google/gemini-1.5-pro"
                            "gemini-1.5-flash" -> "google/gemini-1.5-flash"
                            "gemini-3.5-flash" -> "google/gemini-2.5-flash" // fallback for openrouter
                            "claude-3-5-sonnet-latest" -> "anthropic/claude-3.5-sonnet"
                            "claude-3-5-haiku-latest" -> "anthropic/claude-3.5-haiku"
                            "deepseek-chat" -> "deepseek/deepseek-chat"
                            "deepseek-reasoner" -> "deepseek/deepseek-r1"
                            else -> _activeModel.value
                        }
                    } else {
                        _activeModel.value
                    }

                    val openAiRequest = OpenAIContentRequest(
                        model = finalModelName,
                        messages = listOf(
                            OpenAIMessage(role = "system", content = systemInstructionStr),
                            OpenAIMessage(role = "user", content = userPrompt)
                        )
                    )

                    try {
                        val response = RetrofitClient.service.generateOpenAIContent(
                            url = finalUrl,
                            authHeader = "Bearer $apiKey",
                            request = openAiRequest
                        )
                        val generatedText = response.choices?.firstOrNull()?.message?.content
                        if (generatedText != null) {
                            val cleanedText = sanitizeOutput(generatedText)
                            _generatedReply.value = cleanedText

                            // Save to history log database!
                            repository.insert(
                                ReplyHistory(
                                    tweetContent = input,
                                    replyContent = cleanedText,
                                    modelUsed = _selectedModel.value,
                                    toneChosen = _selectedTone.value
                                )
                            )
                            _apiError.value = null
                        } else {
                            throw Exception("Response dari provider kosong.")
                        }
                    } catch (e: retrofit2.HttpException) {
                        val code = e.code()
                        val errorBody = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                e.response()?.errorBody()?.string() ?: ""
                            } catch (ignored: Exception) {
                                ""
                            }
                        }
                        _apiError.value = "HTTP error $code dari provider: $errorBody"
                    } catch (e: Throwable) {
                        _apiError.value = "Gagal memproses via API Key ini: ${e.localizedMessage}"
                    }
                    _isGenerating.value = false
                    return@launch
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstructionStr)))
                )

                // Call the API model!
                val rawModel = _activeModel.value.lowercase()
                val initialModelName = when {
                    rawModel.contains("gemini-2.5-pro") -> "gemini-2.5-pro"
                    rawModel.contains("gemini-2.5-flash") -> "gemini-2.5-flash"
                    rawModel.contains("gemini-2.0-flash") -> "gemini-2.0-flash"
                    rawModel.contains("gemini-1.5-pro") -> "gemini-1.5-pro"
                    rawModel.contains("gemini-1.5-flash") -> "gemini-1.5-flash"
                    rawModel.contains("gemini-3.5-flash") -> "gemini-3.5-flash"
                    rawModel.contains("gemini-3.1-pro") -> "gemini-3.1-pro-preview"
                    rawModel.contains("gemini-3.1-flash-lite") -> "gemini-3.1-flash-lite-preview"
                    rawModel.contains("gemini-flash-latest") -> "gemini-flash-latest"
                    rawModel == "gemini_pro" || rawModel == "gemini" -> "gemini-2.5-flash"
                    rawModel.startsWith("gemini") -> rawModel.replace("_", "-")
                    else -> "gemini-2.5-flash"
                }

                // Construct candidate try queue: first the initial one, then the rest of the standard fallbacks
                val tryQueue = mutableListOf(initialModelName)
                val standardList = listOf(
                    "gemini-2.5-flash",
                    "gemini-2.5-pro",
                    "gemini-2.0-flash",
                    "gemini-1.5-flash",
                    "gemini-1.5-pro",
                    "gemini-3.5-flash",
                    "gemini-3.1-pro-preview",
                    "gemini-3.1-flash-lite-preview"
                )
                for (fallback in standardList) {
                    if (fallback != initialModelName) {
                        tryQueue.add(fallback)
                    }
                }

                var success = false
                var activeTryIndex = 0
                var lastErrorMessage = ""

                while (activeTryIndex < tryQueue.size && !success) {
                    val currentModelToTry = tryQueue[activeTryIndex]
                    val readableName = when (currentModelToTry) {
                        "gemini-2.5-pro" -> "Gemini 2.5 Pro"
                        "gemini-2.5-flash" -> "Gemini 2.5 Flash"
                        "gemini-2.0-flash" -> "Gemini 2.0 Flash"
                        "gemini-1.5-pro" -> "Gemini 1.5 Pro"
                        "gemini-1.5-flash" -> "Gemini 1.5 Flash"
                        "gemini-3.5-flash" -> "Gemini 3.5 Flash"
                        "gemini-3.1-pro-preview" -> "Gemini 3.1 Pro Preview"
                        "gemini-3.1-flash-lite-preview" -> "Gemini 3.1 Flash Lite Preview"
                        "gemini-flash-latest" -> "Gemini Flash Latest"
                        else -> currentModelToTry
                    }

                    // Quietly switch models in the background without worrying/disturbing the user with a red error card
                    Log.d("MainViewModel", "Model limit or transient error. Seamlessly switching to $readableName...")

                    Log.d("MainViewModel", "Involving API with model: $currentModelToTry")
                    try {
                        val response = RetrofitClient.service.generateContent(
                            model = currentModelToTry,
                            apiKey = apiKey,
                            request = request
                        )

                        val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (generatedText != null) {
                            val cleanedText = sanitizeOutput(generatedText)
                            _generatedReply.value = cleanedText

                            // update model settings to reflect the self-healed working model
                            updateActiveModel(currentModelToTry)

                            // Save to history log database!
                            repository.insert(
                                ReplyHistory(
                                    tweetContent = input,
                                    replyContent = cleanedText,
                                    modelUsed = _selectedModel.value,
                                    toneChosen = _selectedTone.value
                                )
                            )
                            success = true
                            _apiError.value = null
                        } else {
                            throw Exception("Empty content response for $currentModelToTry")
                        }
                    } catch (e: retrofit2.HttpException) {
                        Log.e("MainViewModel", "HTTP Error fetching reply with $currentModelToTry", e)
                        val code = e.code()
                        val errorBody = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                e.response()?.errorBody()?.string() ?: ""
                            } catch (ignored: Exception) {
                                ""
                            }
                        }
                        
                        // For authentication errors (401/403) we should stop immediately and notify bad key
                        if (code == 401 || code == 403) {
                            _apiError.value = "HTTP $code Unauthorized/Forbidden: Kunci API salah atau tidak diizinkan. Gunakan kunci Google Gemini API resmi."
                            break
                        }

                        lastErrorMessage = "HTTP $code: $errorBody"
                        activeTryIndex++
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error fetching reply with $currentModelToTry", e)
                        lastErrorMessage = e.localizedMessage ?: "network timeout"
                        activeTryIndex++
                    }
                }

                if (!success) {
                    _apiError.value = "Semua model limits atau gagal diproses. Error terakhir: $lastErrorMessage"
                }

            } catch (e: Throwable) {
                Log.e("MainViewModel", "Critical generation wrapper exception", e)
                _apiError.value = "error: ${e.localizedMessage ?: "unknown exception"}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private fun sanitizeOutput(text: String): String {
        // Enforce lowercase
        var cleaned = text.lowercase().trim()
        
        // Remove quotes
        cleaned = cleaned.replace("\"", "").replace("'", "").replace("`", "")
        
        // Remove dashes/em-dashes/hyphens at the start or endpoints
        cleaned = cleaned.replace("—", " ").replace("-", " ")
        
        // Remove bullet styles
        cleaned = cleaned.replace("*", "").replace("•", "")
        
        // Remove conversational labels like "reply:" or "response:"
        cleaned = cleaned.replace("reply:", "").replace("response:", "")
        
        // Truncate multiple spaces to single spaces
        cleaned = cleaned.replace("\\s+".toRegex(), " ").trim()
        
        // Remove ending periods if present
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.dropLast(1).trim()
        }
        
        return cleaned
    }

    private fun buildSystemInstruction(model: String, tone: String): String {
        val toneGuide = when (tone) {
            "Degen" -> """
              - Perspective: High-conviction on-chain trends, protocol risk, raw market action, or price movements. Write like an active, bold trader who has deep conviction and strong opinion.
              - Examples/Keywords: trade dynamics, liquidity pools, active accumulation, token utility, conviction. (Avoid overused buzzwords like 'wagmi' or 'lfg' unless absolutely contextual).
            """.trimIndent()
            "Alpha Hunter" -> """
              - Perspective: Focused on underlying value, tech developments, developer activity, ecosystem upgrades, or project roadmaps. Sharp, analytic, and values-driven comments.
              - Examples/Keywords: technical upgrades, mainnet, architectural changes, dev traction, research, structural value.
            """.trimIndent()
            "Shitposter" -> """
              - Perspective: Sarcastic, direct, or pointing out logical fallacies/absurdities in the tweet's claim with a cheeky but context-relevant angle. Real perspective rather than just dry sarcasm.
              - Examples/Keywords: counter-view, logic check, ironical take, realistic expectations.
            """.trimIndent()
            "Casual" -> """
              - Perspective: Conversational, friendly, standard casual opinion. Chill but realistic response from a peer who participates in the space and knows the context.
              - Examples/Keywords: reasonable point, agreed with reservations, interest in details, balanced peer perspective.
            """.trimIndent()
            "Organic" -> """
              - Perspective: Purely organic, deeply empathetic, highly analytical, or humorous depending entirely on the content and sentiment of the tweet. Your mood must dynamically morph to fit the tweet (e.g. matching enthusiasm, skepticism, curiosity, sarcasm, or frustration perfectly).
              - Style: Speak like a real, thoughtful human typing naturally on their phone. Do not sound like an AI assistant. Have a genuine opinion or observation.
              - Examples/Keywords: Use vocabulary and phrasing that a native human speaker would use on social media to express real interest, agreement, or disagreement without clichés.
            """.trimIndent()
            else -> "Witty reply guy style."
        }

        val lengthGuideline = if (tone == "Organic") {
            "5. DYNAMIC & NATURAL LENGTH: The length of the reply MUST be natural and completely variable based on the complexity/vibe of the input tweet (ranging anywhere from a short 4-word reaction to a deeper 18-word observation). There is no strict length limit—write exactly what a real human would write to sound 100% natural, contextual, and authentic."
        } else {
            "5. STRICT LENGTH: The reply length must be exactly between 7 and 10 WORDS. This is an absolute constraint."
        }

        return """
role: highly active, perceptive Twitter (X) participant / reply guy.
task: generate EXACTLY ONE human-like, highly contextual conversational reply to the given tweet.

language routing:
- You must auto-detect and match the tweet's language PERFECTLY (e.g. English, Indonesian, Japanese, Spanish, Arabic, Korean, German, etc.).
- Always reply in the exact same language/locale as the input tweet. Under no circumstances should you reply in a different language than what is provided in the tweet. Do not let instructions set in other languages bias your output language. Match the tweet's language 100%.

guidelines to prevent "AI slop" and sound like an authentic human:
1. NO HEAVY BUZZWORDS: Do not use overused tech/crypto cliché buzzwords (such as lfg, wagmi, we are so back, rwa, absolute cinema, pure brainrot) unless extremely relevant to the original context.
2. NO SLANG ABBREVIATIONS: Avoid lazy, excessive slang abbreviations (e.g., in Indonesian, do NOT use abbreviations like 'udh', 'jg', 'bgt', 'ga', 'lu', 'gw'). Instead, use full, natural, casual words that flow naturally like an ordinary person typing casually on social media.
3. WEAR A REAL PERSPECTIVE / INSIGHT: You must have a clear point of view, sharp observation, and real insight relative to the tweet. Do not just agree passively or praise empty-handed. Add value, opinion, or sharp reaction.
4. STRICT LOWERCASE & NO PERIODS: Write the entire reply in ALL LOWERCASE letters. Do NOT add any periods (.) at the end of the sentence. Write casually and freely, like a quick chat message.
$lengthGuideline
6. NO INTROS/LABEL/PUNCTUATION CRINGE: Do not use quotes, exclamations, or introductory labels like "Reply:". Output ONLY the raw reply text.

tone style guide ($tone):
$toneGuide
""".trimIndent()
    }
}

class MainViewModelFactory(
    private val repository: ReplyRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
