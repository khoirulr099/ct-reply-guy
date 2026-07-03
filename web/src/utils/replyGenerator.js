// Models matching the Kotlin MainViewModel mappings
export const MODELS = [
  { label: "Gemini 2.5 Pro", value: "gemini-2.5-pro", provider: "google" },
  { label: "Gemini 2.5 Flash", value: "gemini-2.5-flash", provider: "google" },
  { label: "Gemini 2.0 Flash", value: "gemini-2.0-flash", provider: "google" },
  { label: "Gemini 1.5 Pro", value: "gemini-1.5-pro", provider: "google" },
  { label: "Gemini 1.5 Flash", value: "gemini-1.5-flash", provider: "google" },
  { label: "Gemini 3.5 Flash", value: "gemini-3.5-flash", provider: "google" },
  { label: "Gemini 3.1 Pro Preview", value: "gemini-3.1-pro-preview", provider: "google" },
  { label: "Gemini 3.1 Flash Lite Preview", value: "gemini-3.1-flash-lite-preview", provider: "google" },
  { label: "GPT-4o", value: "gpt-4o", provider: "openai" },
  { label: "GPT-4o Mini", value: "gpt-4o-mini", provider: "openai" },
  { label: "Claude 3.5 Sonnet", value: "claude-3-5-sonnet-latest", provider: "anthropic" },
  { label: "Claude 3.5 Haiku", value: "claude-3-5-haiku-latest", provider: "anthropic" },
  { label: "DeepSeek V3", value: "deepseek-chat", provider: "deepseek" },
  { label: "DeepSeek R1", value: "deepseek-reasoner", provider: "deepseek" },
  { label: "DeepSeek V3 (OpenRouter)", value: "deepseek/deepseek-chat", provider: "openrouter" },
  { label: "DeepSeek R1 (OpenRouter)", value: "deepseek/deepseek-r1", provider: "openrouter" },
  { label: "Claude 3.5 Sonnet (OpenRouter)", value: "anthropic/claude-3.5-sonnet", provider: "openrouter" },
  { label: "Claude 3.5 Haiku (OpenRouter)", value: "anthropic/claude-3.5-haiku", provider: "openrouter" },
  { label: "Gemini 2.5 Pro (OpenRouter)", value: "google/gemini-2.5-pro", provider: "openrouter" },
  { label: "Gemini 2.5 Flash (OpenRouter)", value: "google/gemini-2.5-flash", provider: "openrouter" },
];

export const TONES = [
  { label: "Degen", description: "Bold, price & risk action, high conviction." },
  { label: "Alpha Hunter", description: "Tech, Ecosystem upgrades, dev traction." },
  { label: "Shitposter", description: "Direct logic checks, sarcastic peer observations." },
  { label: "Casual", description: "Standard balanced peer opinion, chill." },
  { label: "Organic", description: "100% natural, empathetic, morphs to the tweet." }
];

export function sanitizeOutput(text) {
  if (!text) return "";
  let cleaned = text.toLowerCase().trim();

  // Remove quotes
  cleaned = cleaned.replace(/["'`]/g, "");

  // Remove dashes/em-dashes/hyphens at the start or endpoints
  cleaned = cleaned.replace(/—/g, " ").replace(/-/g, " ");

  // Remove bullet styles
  cleaned = cleaned.replace(/\*/g, "").replace(/•/g, "");

  // Remove conversational labels
  cleaned = cleaned.replace(/reply:/g, "").replace(/response:/g, "");

  // Truncate multiple spaces to single spaces
  cleaned = cleaned.replace(/\s+/g, " ").trim();

  // Remove ending periods if present
  while (cleaned.endsWith(".")) {
    cleaned = cleaned.slice(0, -1).trim();
  }

  return cleaned;
}

export function buildSystemInstruction(tone) {
  let toneGuide = "";
  switch (tone) {
    case "Degen":
      toneGuide = `
        - Perspective: High-conviction on-chain trends, protocol risk, raw market action, or price movements. Write like an active, bold trader who has deep conviction and strong opinion.
        - Examples/Keywords: trade dynamics, liquidity pools, active accumulation, token utility, conviction. (Avoid overused buzzwords like 'wagmi' or 'lfg' unless absolutely contextual).
      `;
      break;
    case "Alpha Hunter":
      toneGuide = `
        - Perspective: Focused on underlying value, tech developments, developer activity, ecosystem upgrades, or project roadmaps. Sharp, analytic, and values-driven comments.
        - Examples/Keywords: technical upgrades, mainnet, architectural changes, dev traction, research, structural value.
      `;
      break;
    case "Shitposter":
      toneGuide = `
        - Perspective: Sarcastic, direct, or pointing out logical fallacies/absurdities in the tweet's claim with a cheeky but context-relevant angle. Real perspective rather than just dry sarcasm.
        - Examples/Keywords: counter-view, logic check, ironical take, realistic expectations.
      `;
      break;
    case "Casual":
      toneGuide = `
        - Perspective: Conversational, friendly, standard casual opinion. Chill but realistic response from a peer who participates in the space and knows the context.
        - Examples/Keywords: reasonable point, agreed with reservations, interest in details, balanced peer perspective.
      `;
      break;
    case "Organic":
      toneGuide = `
        - Perspective: Purely organic, deeply empathetic, highly analytical, or humorous depending entirely on the content and sentiment of the tweet. Your mood must dynamically morph to fit the tweet (e.g. matching enthusiasm, skepticism, curiosity, sarcasm, or frustration perfectly).
        - Style: Speak like a real, thoughtful human typing naturally on their phone. Do not sound like an AI assistant. Have a genuine opinion or observation.
        - Examples/Keywords: Use vocabulary and phrasing that a native human speaker would use on social media to express real interest, agreement, or disagreement without clichés.
      `;
      break;
    default:
      toneGuide = "Witty reply guy style.";
  }

  const lengthGuideline = tone === "Organic"
    ? "5. DYNAMIC & NATURAL LENGTH: The length of the reply MUST be natural and completely variable based on the complexity/vibe of the input tweet (ranging anywhere from a short 4-word reaction to a deeper 18-word observation). There is no strict length limit—write exactly what a real human would write to sound 100% natural, contextual, and authentic."
    : "5. STRICT LENGTH: The reply length must be exactly between 7 and 10 WORDS. This is an absolute constraint.";

  return `role: highly active, perceptive Twitter (X) participant / reply guy.
task: generate EXACTLY ONE human-like, highly contextual conversational reply to the given tweet.

language routing:
- You must auto-detect and match the tweet's language PERFECTLY (e.g. English, Indonesian, Japanese, Spanish, Arabic, Korean, German, etc.).
- Always reply in the exact same language/locale as the input tweet. Under no circumstances should you reply in a different language than what is provided in the tweet. Do not let instructions set in other languages bias your output language. Match the tweet's language 100%.

guidelines to prevent "AI slop" and sound like an authentic human:
1. NO HEAVY BUZZWORDS: Do not use overused tech/crypto cliché buzzwords (such as lfg, wagmi, we are so back, rwa, absolute cinema, pure brainrot) unless extremely relevant to the original context.
2. NO SLANG ABBREVIATIONS: Avoid lazy, excessive slang abbreviations (e.g., in Indonesian, do NOT use abbreviations like 'udh', 'jg', 'bgt', 'ga', 'lu', 'gw'). Instead, use full, natural, casual words that flow naturally like an ordinary person typing casually on social media.
3. WEAR A REAL PERSPECTIVE / INSIGHT: You must have a clear point of view, sharp observation, and real insight relative to the tweet. Do not just agree passively or praise empty-handed. Add value, opinion, or sharp reaction.
4. STRICT LOWERCASE & NO PERIODS: Write the entire reply in ALL LOWERCASE letters. Do NOT add any periods (.) at the end of the sentence. Write casually and freely, like a quick chat message.
${lengthGuideline}
6. NO INTROS/LABEL/PUNCTUATION CRINGE: Do not use quotes, exclamations, or introductory labels like "Reply:". Output ONLY the raw reply text.

tone style guide (${tone}):
${toneGuide.trim()}`;
}

// Seamless self-healing try queue for Google Gemini
const GEMINI_FALLBACKS = [
  "gemini-2.5-flash",
  "gemini-2.5-pro",
  "gemini-2.0-flash",
  "gemini-1.5-flash",
  "gemini-1.5-pro",
  "gemini-3.5-flash",
  "gemini-3.1-pro-preview",
  "gemini-3.1-flash-lite-preview"
];

export async function generateReply({
  tweet,
  model,
  tone,
  apiKey,
  customBaseUrl
}) {
  if (!tweet || !tweet.trim()) {
    throw new Error("Konten tweet tidak boleh kosong, anon!");
  }
  if (!apiKey || !apiKey.trim()) {
    throw new Error("API Key belum dimasukkan! Silakan isi API Key Anda di panel pengaturan.");
  }

  const systemInstruction = buildSystemInstruction(tone);
  const userPrompt = `---\n${tweet.trim()}\n---`;

  // Check if we should use OpenAI / OpenRouter / Custom compatible API
  const isSkKey = apiKey.startsWith("sk-") || (customBaseUrl && customBaseUrl.trim() !== "");

  if (isSkKey) {
    let finalUrl = "";
    const base = customBaseUrl ? customBaseUrl.trim() : "";

    if (base) {
      if (base.endsWith("/chat/completions")) {
        finalUrl = base;
      } else if (base.endsWith("/")) {
        finalUrl = `${base}chat/completions`;
      } else {
        finalUrl = `${base}/chat/completions`;
      }
    } else {
      // Guess URL based on model name / api key
      if (model.includes("/") || apiKey.startsWith("sk-or-")) {
        finalUrl = "https://openrouter.ai/api/v1/chat/completions";
      } else if (model.startsWith("deepseek")) {
        finalUrl = "https://api.deepseek.com/v1/chat/completions";
      } else {
        finalUrl = "https://api.openai.com/v1/chat/completions";
      }
    }

    // Map models to OpenRouter identifiers if using OpenRouter
    let finalModel = model;
    if (finalUrl.includes("openrouter.ai")) {
      switch (model) {
        case "gemini-2.5-pro": finalModel = "google/gemini-2.5-pro"; break;
        case "gemini-2.5-flash": finalModel = "google/gemini-2.5-flash"; break;
        case "gemini-2.0-flash": finalModel = "google/gemini-2.0-flash"; break;
        case "gemini-1.5-pro": finalModel = "google/gemini-1.5-pro"; break;
        case "gemini-1.5-flash": finalModel = "google/gemini-1.5-flash"; break;
        case "gemini-3.5-flash": finalModel = "google/gemini-2.5-flash"; break; // openrouter fallback
        case "claude-3-5-sonnet-latest": finalModel = "anthropic/claude-3.5-sonnet"; break;
        case "claude-3-5-haiku-latest": finalModel = "anthropic/claude-3.5-haiku"; break;
        case "deepseek-chat": finalModel = "deepseek/deepseek-chat"; break;
        case "deepseek-reasoner": finalModel = "deepseek/deepseek-r1"; break;
      }
    }

    const response = await fetch(finalUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: finalModel,
        messages: [
          { role: "system", content: systemInstruction },
          { role: "user", content: userPrompt }
        ]
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP error ${response.status} dari provider: ${errorText || response.statusText}`);
    }

    const data = await response.json();
    const generatedText = data.choices?.[0]?.message?.content;
    if (!generatedText) {
      throw new Error("Response dari provider kosong.");
    }

    return {
      reply: sanitizeOutput(generatedText),
      usedModel: model
    };
  }

  // Otherwise, use direct Gemini API calls (Google AI API)
  // Construct Try queue
  const initialModel = model.startsWith("gemini") ? model : "gemini-2.5-flash";
  const tryQueue = [initialModel, ...GEMINI_FALLBACKS.filter(m => m !== initialModel)];

  let lastError = null;
  for (const modelToTry of tryQueue) {
    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelToTry}:generateContent?key=${apiKey}`;
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          contents: [
            { parts: [{ text: userPrompt }] }
          ],
          systemInstruction: {
            parts: [{ text: systemInstruction }]
          }
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        // Prompt auth failures (401, 403) to exit immediately
        if (response.status === 401 || response.status === 403) {
          throw new Error(`Kunci API salah atau tidak diizinkan (HTTP ${response.status}). Gunakan kunci Google Gemini API resmi.`);
        }
        throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
      }

      const data = await response.json();
      const generatedText = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!generatedText) {
        throw new Error(`Response kosong dari model ${modelToTry}`);
      }

      return {
        reply: sanitizeOutput(generatedText),
        usedModel: modelToTry
      };
    } catch (e) {
      console.warn(`Gagal memproses dengan model ${modelToTry}. Mencoba model cadangan...`, e);
      lastError = e;
      // If it's a security/auth error, don't try other models
      if (e.message.includes("Kunci API salah")) {
        throw e;
      }
    }
  }

  throw new Error(`Semua model limits atau gagal diproses. Error terakhir: ${lastError?.message || lastError}`);
}
