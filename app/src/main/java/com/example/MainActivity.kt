package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ReplyHistory
import com.example.data.ReplyRepository
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup local database & repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ReplyRepository(database.replyDao())
        val viewModel = ViewModelProvider(this, MainViewModelFactory(repository, applicationContext))[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ImmersiveObsidian
                ) { innerPadding ->
                    ReplyGuyApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReplyGuyApp(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe States from ViewModel
    val tweetInput by viewModel.tweetInput.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedTone by viewModel.selectedTone.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generatedReply by viewModel.generatedReply.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val historyLog by viewModel.historyLog.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeApiKey by viewModel.activeApiKey.collectAsState()

    // 0 = Reply, 1 = History log, 2 = Configurations
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveObsidian)
    ) {
        // --- HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Lightning Bolt Badge (Matches HTML precisely)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ImmersiveLavender, ImmersiveDeepViolet)
                            )
                        )
                        .border(1.dp, ImmersiveBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Glow bolt logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "degenreply",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "active on chain neural core",
                        color = ImmersivePlaceholder,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- MODEL SELECTION pill bar (Always visible, clean responsive horizontal scroll) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val models = listOf(
                    "Gemini 2.5 Pro",
                    "Gemini 2.5 Flash",
                    "Gemini 2.0 Flash",
                    "Gemini 1.5 Pro",
                    "Gemini 1.5 Flash",
                    "Gemini 3.5 Flash",
                    "Gemini 3.1 Pro Preview",
                    "Gemini 3.1 Flash Lite Preview",
                    "GPT-4o",
                    "GPT-4o Mini",
                    "Claude 3.5 Sonnet",
                    "Claude 3.5 Haiku",
                    "DeepSeek V3",
                    "DeepSeek R1",
                    "DeepSeek V3 (OpenRouter)",
                    "DeepSeek R1 (OpenRouter)",
                    "Claude 3.5 Sonnet (OpenRouter)",
                    "Claude 3.5 Haiku (OpenRouter)",
                    "Gemini 2.5 Pro (OpenRouter)",
                    "Gemini 2.5 Flash (OpenRouter)"
                )
                models.forEach { model ->
                    val isSelected = selectedModel == model
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ImmersiveLavender else ImmersiveCharcoal)
                            .clickable { viewModel.onModelSelected(model) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Active motor icon",
                                tint = ImmersiveDeepViolet,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = model.lowercase(),
                            color = if (isSelected) ImmersiveDeepViolet else ImmersiveTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- MAIN WORKSPACE VIEW ROUTER ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> ReplyWorkspace(
                    viewModel = viewModel,
                    tweetInput = tweetInput,
                    selectedTone = selectedTone,
                    isGenerating = isGenerating,
                    generatedReply = generatedReply,
                    apiError = apiError,
                    context = context,
                    clipboardManager = clipboardManager
                )
                1 -> HistoryWorkspace(
                    viewModel = viewModel,
                    historyLog = historyLog,
                    context = context,
                    clipboardManager = clipboardManager
                )
                2 -> ConfigWorkspace(
                    viewModel = viewModel,
                    selectedModel = selectedModel,
                    selectedTone = selectedTone,
                    historyCount = historyLog.size,
                    activeModel = activeModel,
                    activeApiKey = activeApiKey
                )
            }
        }

        // --- IMMERSIVE FOOTER: ACTIONS & BOTTOM NAVIGATION ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveObsidian)
                .padding(16.dp)
        ) {
            // Main Generate Action (Only visible in Reply Workspace and if not already generating)
            if (selectedTab == 0) {
                Button(
                    onClick = { viewModel.generateReplyGuyResponse() },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveLavender,
                        disabledContainerColor = ImmersiveCharcoal,
                        contentColor = ImmersiveDeepViolet,
                        disabledContentColor = ImmersivePlaceholder
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal action",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isGenerating) "forging reply..." else "generate reply",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Material Immersive Navigation Bar Row (Exactly like HTML)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Reply
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = 0 }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Reply workspace",
                        tint = if (selectedTab == 0) ImmersiveLavender else ImmersivePlaceholder,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "reply",
                        color = if (selectedTab == 0) ImmersiveLavender else ImmersivePlaceholder,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Tab 1: History
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = 1 }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History log",
                        tint = if (selectedTab == 1) ImmersiveLavender else ImmersivePlaceholder,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "history",
                        color = if (selectedTab == 1) ImmersiveLavender else ImmersivePlaceholder,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Tab 2: Config
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = 2 }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (selectedTab == 2) Icons.Default.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings configuration",
                        tint = if (selectedTab == 2) ImmersiveLavender else ImmersivePlaceholder,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "config",
                        color = if (selectedTab == 2) ImmersiveLavender else ImmersivePlaceholder,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- TAB 0 VIEW: REPLY ACTIVE WORKSPACE ---
@Composable
fun ReplyWorkspace(
    viewModel: MainViewModel,
    tweetInput: String,
    selectedTone: String,
    isGenerating: Boolean,
    generatedReply: String?,
    apiError: String?,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // STYLE AGENT SELECTOR (Tones setup)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ImmersiveContainers)
                .padding(12.dp)
        ) {
            Text(
                text = "select style agent",
                color = ImmersiveLavender,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val tones = listOf(
                "Degen" to "🚀 degen",
                "Alpha Hunter" to "📡 alpha",
                "Shitposter" to "🤡 shitpost",
                "Casual" to "☕ casual",
                "Organic" to "🧠 organic"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tones.forEach { (toneValue, label) ->
                    val isSelected = selectedTone == toneValue
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ImmersiveLavender else ImmersiveCharcoal)
                            .clickable { viewModel.onToneSelected(toneValue) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) ImmersiveDeepViolet else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ORIGINAL INPUT TWEET CARD (Matches bg-[#1D1B20] rounded-3xl p-4)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ImmersiveSlateDim)
                .border(1.dp, ImmersiveBorder, RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "input tweet",
                    color = ImmersiveLavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Inline Paste trigger shortcut
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            try {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrEmpty()) {
                                    viewModel.onTweetInputChanged(clipText)
                                    showToast(context, "pasted from clipboard!")
                                } else {
                                    showToast(context, "clipboard is empty, anon!")
                                }
                            } catch (e: Throwable) {
                                showToast(context, "clipboard access disabled")
                            }
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste clip",
                        tint = ImmersivePlaceholder,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "paste",
                        color = ImmersivePlaceholder,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Beautiful seamless typography input area
            OutlinedTextField(
                value = tweetInput,
                onValueChange = { viewModel.onTweetInputChanged(it) },
                placeholder = {
                    Text(
                        text = "eth is dead for real this time guys pack it up it is over for the l1 thesis and everything we built",
                        color = ImmersivePlaceholder.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = ImmersiveTextMain,
                    unfocusedTextColor = ImmersiveTextMain
                ),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp
                )
            )

            // Bottom control inside Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tweetInput.length} characters",
                    color = ImmersivePlaceholder,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (tweetInput.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearInput() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = NeonBearRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // GENERATIVE LIVE LOADER OVERLAY
        AnimatedVisibility(visible = isGenerating) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ImmersiveContainers)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ImmersiveLavender,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "forging based response...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ERROR NOTIFICATION CONTAINER (Tactical dark theme integration - dismissible and non-disturbing, replaces loud red ones as requested)
        AnimatedVisibility(visible = apiError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ImmersiveContainers)
                    .border(1.dp, ImmersiveBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notification icon",
                    tint = ImmersiveLavender,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = apiError ?: "",
                    color = ImmersiveTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.clearError() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notification",
                        tint = ImmersivePlaceholder,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // DEGEN OUTPUT CARD (Glow-ring border custom display)
        AnimatedVisibility(
            visible = generatedReply != null,
            enter = slideInVertically(initialOffsetY = { 30 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 30 }) + fadeOut()
        ) {
            generatedReply?.let { reply ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .drawBehind {
                            // Immersive purple outline glow canvas decoration
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    listOf(ImmersiveLavender, ImmersiveDeepViolet)
                                ),
                                alpha = 0.25f
                            )
                        }
                        .padding(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(ImmersiveContainers)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "degen output",
                                color = ImmersiveLavender,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // Quick Action Tools
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Copy
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = ImmersiveLavender,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            try {
                                                clipboardManager.setText(AnnotatedString(reply))
                                                showToast(context, "copied raw reply!")
                                            } catch (e: Throwable) {
                                                showToast(context, "failed to copy to clipboard")
                                            }
                                        }
                                )
                                // Share
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = ImmersiveLavender,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            showToast(context, "simulating post sharing...")
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big lowercased exact text matching CT styling
                        Text(
                            text = reply,
                            color = ImmersiveTextMain,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom telemetry metadata indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ImmersiveCharcoal)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${reply.split("\\s+".toRegex()).size} words",
                                        color = ImmersiveTextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "checked & clean",
                                    color = NeonGainGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "@replyguy_bot",
                                color = ImmersivePlaceholder,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 1 VIEW: PERSISTED OFFLINE HISTORY LOGS ---
@Composable
fun HistoryWorkspace(
    viewModel: MainViewModel,
    historyLog: List<ReplyHistory>,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "saved logs history",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ImmersiveLavender)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = historyLog.size.toString(),
                        color = ImmersiveDeepViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (historyLog.isNotEmpty()) {
                Text(
                    text = "clear all logs",
                    color = NeonBearRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable {
                            viewModel.clearHistory()
                            showToast(context, "cleared all cached history logs")
                        }
                        .padding(4.dp)
                )
            }
        }

        if (historyLog.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = ImmersivePlaceholder,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "the logbook is empty, anon",
                    color = ImmersiveTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "run reply generation outputs to automatically save them locally",
                    color = ImmersivePlaceholder,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = historyLog,
                    key = { it.id }
                ) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ImmersiveContainers)
                            .border(1.dp, ImmersiveBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(ImmersiveLavender),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.toneChosen.firstOrNull()?.lowercase() ?: "d",
                                        color = ImmersiveDeepViolet,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "agent: ${item.toneChosen.lowercase()}",
                                    color = ImmersiveLavender,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Delete Specific Log Item
                            IconButton(
                                onClick = { viewModel.deleteHistoryItem(item.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete item",
                                    tint = ImmersivePlaceholder,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Past Input Tweet
                        Text(
                            text = "twt: ${item.tweetContent}",
                            color = ImmersiveTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Saved lowercase response
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "rep: ${item.replyContent}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    try {
                                        clipboardManager.setText(AnnotatedString(item.replyContent))
                                        showToast(context, "copied cached reply!")
                                    } catch (e: Throwable) {
                                        showToast(context, "failed to copy to clipboard")
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy log action",
                                    tint = ImmersiveLavender,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2 VIEW: API CONFIGURATION & CORE STATUS PANEL ---
@Composable
fun ConfigWorkspace(
    viewModel: MainViewModel,
    selectedModel: String,
    selectedTone: String,
    historyCount: Int,
    activeModel: String,
    activeApiKey: String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    var localApiKey by remember(activeApiKey) { mutableStateOf(activeApiKey) }
    var localModelName by remember(activeModel) { mutableStateOf(activeModel) }
    var localApiBaseUrl by remember(apiBaseUrl) { mutableStateOf(apiBaseUrl) }

    var apiKeyVisible by remember { mutableStateOf(false) }
    var apiBaseUrlVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECURE API KEY & MODEL CONTROLS PANEL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ImmersiveContainers)
                .border(1.dp, ImmersiveBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "quick configuration console",
                color = ImmersiveLavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // API key textfield
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = localApiKey,
                    onValueChange = { localApiKey = it },
                    label = { Text("Universal API Key", color = ImmersivePlaceholder, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. AIzaSy...", color = ImmersivePlaceholder.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = "Key Icon", tint = ImmersiveLavender, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        val icon = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (apiKeyVisible) "Hide API Key" else "Show API Key"
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(imageVector = icon, contentDescription = description, tint = ImmersiveLavender, modifier = Modifier.size(20.dp))
                        }
                    },
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ImmersiveLavender,
                        unfocusedBorderColor = ImmersiveBorder,
                        focusedContainerColor = ImmersiveCharcoal,
                        unfocusedContainerColor = ImmersiveCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.updateActiveApiKey(localApiKey)
                        showToast(context, "Kunci API universal berhasil didaftarkan!")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveLavender,
                        contentColor = ImmersiveDeepViolet
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Register Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // API Base URL textfield
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = localApiBaseUrl,
                    onValueChange = { localApiBaseUrl = it },
                    label = { Text("API Base URL (Optional / Compatible with sk-)", color = ImmersivePlaceholder, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. https://openrouter.ai/api/v1/", color = ImmersivePlaceholder.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = "Base URL Icon", tint = ImmersiveLavender, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        val icon = if (apiBaseUrlVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (apiBaseUrlVisible) "Hide API Base URL" else "Show API Base URL"
                        IconButton(onClick = { apiBaseUrlVisible = !apiBaseUrlVisible }) {
                            Icon(imageVector = icon, contentDescription = description, tint = ImmersiveLavender, modifier = Modifier.size(20.dp))
                        }
                    },
                    visualTransformation = if (apiBaseUrlVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ImmersiveLavender,
                        unfocusedBorderColor = ImmersiveBorder,
                        focusedContainerColor = ImmersiveCharcoal,
                        unfocusedContainerColor = ImmersiveCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.updateApiBaseUrl(localApiBaseUrl)
                        showToast(context, "API Base URL berhasil disimpan!")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveLavender,
                        contentColor = ImmersiveDeepViolet
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Register Base URL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Model Name textfield
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = localModelName,
                    onValueChange = { localModelName = it },
                    label = { Text("Active AI Engine (Model)", color = ImmersivePlaceholder, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. gemini_pro", color = ImmersivePlaceholder.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = "Robot Icon", tint = ImmersiveLavender, modifier = Modifier.size(16.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ImmersiveLavender,
                        unfocusedBorderColor = ImmersiveBorder,
                        focusedContainerColor = ImmersiveCharcoal,
                        unfocusedContainerColor = ImmersiveCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.updateActiveModel(localModelName)
                        showToast(context, "Mesin aktif dialihkan ke $localModelName!")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveLavender,
                        contentColor = ImmersiveDeepViolet
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Switch Engine", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

                // Direct System Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    showToast(context, "Engine: $activeModel\nAPI Key: ${if (activeApiKey.isEmpty()) "belum diatur (manual)" else "custom_key"}")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersiveCharcoal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(14.dp), tint = ImmersiveLavender)
                    Text("Show Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    viewModel.resetToDefaultSettings()
                    localApiKey = ""
                    localModelName = "gemini-2.5-flash"
                    localApiBaseUrl = ""
                    showToast(context, "Seluruh pengaturan di-reset ke bawaan!")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBearRed.copy(alpha = 0.15f),
                    contentColor = NeonBearRed
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Reset Bawaan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SPECIFICATIONS SPECS TAB ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ImmersiveContainers)
                .padding(16.dp)
        ) {
            Text(
                text = "active server specifications",
                color = ImmersiveLavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val details = listOf(
                "application engine" to "DegenReply Core",
                "default neural motor" to activeModel,
                "active model key" to (if (activeApiKey.isEmpty()) "belum diatur (manual)" else "configured (custom)"),
                "active personality" to selectedTone,
                "offline storage state" to "Room DB active ($historyCount entries)",
                "strict constraint code" to "7-10 words • lowercase raw",
                "character encoding" to "UTF-8 adaptive engine"
            )

            details.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key,
                        color = ImmersivePlaceholder,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = value.lowercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
                HorizontalDivider(color = ImmersiveBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        }

        // --- ADVANCED TERMINAL USAGE HELP PANEL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ImmersiveContainers)
                .border(1.dp, ImmersiveBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "advanced terminal commands",
                color = ImmersiveLavender,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Anda dapat mengetikkan perintah di bawah langsung pada kolom input teks utama menggunakan sintaks khusus:",
                color = ImmersiveTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            val terminalCommands = listOf(
                "[menu: insert api key = \"KUNCI_API\"]" to "Mendaftarkan kunci API pribadi secara universal.",
                "[menu: select model = \"nama_model\"]" to "Mengalihkan mesin AI aktif secara instan.",
                "[menu: show status]" to "Menampilkan status mesin aktif dan kunci API saat ini.",
                "[menu: reset to default]" to "Mengembalikan seluruh pengaturan ke konfigurasi bawaan."
            )

            terminalCommands.forEach { (commandText, desc) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ImmersiveObsidian)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = commandText,
                            color = NeonGainGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy command syntax",
                            tint = ImmersivePlaceholder,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    try {
                                        clipboardManager.setText(AnnotatedString(commandText))
                                        showToast(context, "Command disalin!")
                                    } catch (e: Throwable) {
                                        showToast(context, "Gagal menyalin")
                                    }
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = desc,
                        color = ImmersivePlaceholder,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Live stats/latency panel cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Latency card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ImmersiveContainers)
                    .padding(12.dp)
            ) {
                Text(
                    text = "api latency",
                    color = ImmersivePlaceholder,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "142 ms",
                    color = NeonGainGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Temperature card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ImmersiveContainers)
                    .padding(12.dp)
            ) {
                Text(
                    text = "degen heat rate",
                    color = ImmersivePlaceholder,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "1.0 extreme",
                    color = ImmersiveLavender,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Informative tip inside config
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ImmersiveCharcoal)
                .padding(14.dp)
        ) {
            Text(
                text = "tip: you are running a server-side gemini pipeline. the reply guy generates 7-10 word lowercase responses which align to crypto lingo automatically. select 'degen' for heavy solana alpha, shitpost for ironic responses.",
                color = ImmersiveTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

private fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
