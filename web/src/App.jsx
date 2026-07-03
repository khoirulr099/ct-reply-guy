import React, { useState, useEffect } from 'react';
import { MODELS, TONES, generateReply } from './utils/replyGenerator';

const STORAGE_KEYS = {
  API_KEY: 'ct_reply_guy_api_key',
  BASE_URL: 'ct_reply_guy_base_url',
  MODEL: 'ct_reply_guy_model',
  TONE: 'ct_reply_guy_tone',
  HISTORY: 'ct_reply_guy_history'
};

export default function App() {
  // Config & State
  const [apiKey, setApiKey] = useState(() => localStorage.getItem(STORAGE_KEYS.API_KEY) || '');
  const [customBaseUrl, setCustomBaseUrl] = useState(() => localStorage.getItem(STORAGE_KEYS.BASE_URL) || '');
  const [selectedModel, setSelectedModel] = useState(() => localStorage.getItem(STORAGE_KEYS.MODEL) || 'gemini-3.5-flash');
  const [selectedTone, setSelectedTone] = useState(() => localStorage.getItem(STORAGE_KEYS.TONE) || 'Degen');
  const [showBaseUrl, setShowBaseUrl] = useState(false);

  // Input & Generation State
  const [tweetInput, setTweetInput] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedReply, setGeneratedReply] = useState(null);
  const [usedModel, setUsedModel] = useState(null);
  const [apiError, setApiError] = useState(null);

  // UI state
  const [copiedId, setCopiedId] = useState(null); // 'output' or history item ID
  const [historyLog, setHistoryLog] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.HISTORY)) || [];
    } catch {
      return [];
    }
  });

  // Persist settings
  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.API_KEY, apiKey);
  }, [apiKey]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.BASE_URL, customBaseUrl);
  }, [customBaseUrl]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.MODEL, selectedModel);
  }, [selectedModel]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.TONE, selectedTone);
  }, [selectedTone]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.HISTORY, JSON.stringify(historyLog));
  }, [historyLog]);

  // Command interceptor matching Kotlin logic: e.g. [menu:show status] or [menu:reset to default]
  const handleCommandInterception = (input) => {
    const trimmed = input.trim();
    if (trimmed.startsWith('[menu:') && trimmed.endsWith(']')) {
      const command = trimmed.substring(6, trimmed.length - 1).trim();
      setApiError(null);

      if (command.startsWith('insert api key =')) {
        const keyVal = command.split('insert api key =')[1].trim().replace(/^["']|["']$/g, '');
        setApiKey(keyVal);
        setGeneratedReply('status: universal api key registered successfully.');
      } else if (command.startsWith('insert api base =')) {
        const baseVal = command.split('insert api base =')[1].trim().replace(/^["']|["']$/g, '');
        setCustomBaseUrl(baseVal);
        setGeneratedReply('status: custom api base URL registered.');
      } else if (command.startsWith('select model =')) {
        const modelVal = command.split('select model =')[1].trim().replace(/^["']|["']$/g, '');
        setSelectedModel(modelVal);
        setGeneratedReply(`status: switched engine to ${modelVal}.`);
      } else if (command === 'show status') {
        setGeneratedReply(`active model: ${selectedModel} | api key: ${apiKey ? 'configured (hidden)' : 'not configured'} | base: ${customBaseUrl || 'default'}`);
      } else if (command === 'reset to default') {
        setApiKey('');
        setSelectedModel('gemini-2.5-flash');
        setCustomBaseUrl('');
        setGeneratedReply('status: configuration reset successfully.');
      } else {
        setApiError(`unknown configuration command: ${command}`);
      }
      return true;
    }
    return false;
  };

  const handleGenerate = async () => {
    if (handleCommandInterception(tweetInput)) {
      return;
    }

    setApiError(null);
    setGeneratedReply(null);
    setIsGenerating(true);

    try {
      const result = await generateReply({
        tweet: tweetInput,
        model: selectedModel,
        tone: selectedTone,
        apiKey,
        customBaseUrl
      });

      setGeneratedReply(result.reply);
      setUsedModel(result.usedModel);

      // Save to history
      const newHistoryItem = {
        id: Date.now(),
        tweetContent: tweetInput,
        replyContent: result.reply,
        modelUsed: result.usedModel,
        toneChosen: selectedTone,
        timestamp: new Date().toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })
      };
      setHistoryLog(prev => [newHistoryItem, ...prev]);

    } catch (e) {
      setApiError(e.message);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleCopy = (text, id) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleDeleteHistory = (id) => {
    setHistoryLog(prev => prev.filter(item => item.id !== id));
  };

  const handleClearHistory = () => {
    if (window.confirm("Apakah Anda yakin ingin menghapus seluruh riwayat balasan?")) {
      setHistoryLog([]);
    }
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="header-logo">
          <span className="logo-icon">🤖</span>
          <div className="logo-text">
            <h1>CT REPLY GUY</h1>
            <p>AI Twitter Agent & Engagement Booster</p>
          </div>
        </div>
        <div className={`api-status-badge ${apiKey ? 'connected' : 'disconnected'}`}>
          <span className="status-dot"></span>
          {apiKey ? 'API KEY ACTIVE' : 'API KEY REQUIRED'}
        </div>
      </header>

      {/* Grid Dashboard */}
      <div className="dashboard-grid">
        
        {/* Left Column: Settings Panel */}
        <aside className="glass-panel">
          <h2 className="panel-title">
            <span>⚙️</span> Pengaturan AI
          </h2>
          
          <div className="config-group">
            
            {/* API Key Input */}
            <div className="form-field">
              <label className="form-label" htmlFor="api-key">
                API Key
                <span className="label-info">Gemini, OpenRouter, or DeepSeek</span>
              </label>
              <div className="input-wrapper">
                <span className="input-icon">🔑</span>
                <input
                  id="api-key"
                  className="text-input"
                  type="password"
                  placeholder="Masukkan API Key..."
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                />
              </div>
            </div>

            {/* Custom URL settings toggle */}
            <button className="settings-toggle" onClick={() => setShowBaseUrl(!showBaseUrl)}>
              {showBaseUrl ? '▼ Sembunyikan Custom Base URL' : '▶ Tampilkan Custom Base URL'}
            </button>

            {/* Custom Base URL Input */}
            {showBaseUrl && (
              <div className="form-field" style={{ animation: 'slide-in 0.25s ease-out' }}>
                <label className="form-label" htmlFor="custom-url">
                  Custom Base URL
                  <span className="label-info">Opsional</span>
                </label>
                <div className="input-wrapper">
                  <span className="input-icon">🌐</span>
                  <input
                    id="custom-url"
                    className="text-input"
                    type="text"
                    placeholder="https://api.openai.com/v1"
                    value={customBaseUrl}
                    onChange={(e) => setCustomBaseUrl(e.target.value)}
                  />
                </div>
              </div>
            )}

            {/* Model Select */}
            <div className="form-field">
              <label className="form-label" htmlFor="model-select">
                Engine / Model
              </label>
              <select
                id="model-select"
                className="select-input"
                value={selectedModel}
                onChange={(e) => setSelectedModel(e.target.value)}
              >
                {MODELS.map(m => (
                  <option key={m.value} value={m.value}>
                    {m.label} ({m.provider.toUpperCase()})
                  </option>
                ))}
              </select>
            </div>

            {/* Tone Card Grid */}
            <div className="form-field">
              <label className="form-label">
                Pilih Karakter / Nada Bicara
              </label>
              <div className="tone-grid">
                {TONES.map(t => (
                  <button
                    key={t.label}
                    className={`tone-card ${selectedTone === t.label ? 'active' : ''}`}
                    data-tone={t.label}
                    onClick={() => setSelectedTone(t.label)}
                  >
                    <span className="tone-name">{t.label}</span>
                    <span className="tone-desc">{t.description}</span>
                  </button>
                ))}
              </div>
            </div>

          </div>
        </aside>

        {/* Right Column: Generation Panel & History */}
        <main className="main-column">
          
          {/* Main Generator Box */}
          <section className="glass-panel generator-panel">
            <h2 className="panel-title">
              <span>✍️</span> Input Konten Tweet
            </h2>

            <div className="input-header">
              <label className="form-label" htmlFor="tweet-input">Tempel Tweet yang ingin Anda balas</label>
              <span className="char-counter">{tweetInput.length} karakter</span>
            </div>

            <textarea
              id="tweet-input"
              className="tweet-textarea"
              placeholder="Contoh: bitcoin is dumping again. is this the end of bullrun or just another dip for ants?"
              value={tweetInput}
              onChange={(e) => setTweetInput(e.target.value)}
              disabled={isGenerating}
            />

            <div className="generator-actions">
              <button 
                className="btn btn-primary" 
                onClick={handleGenerate}
                disabled={isGenerating || !tweetInput.trim() || (!apiKey && !tweetInput.trim().startsWith('[menu:'))}
              >
                {isGenerating ? (
                  <>
                    <span className="spinner"></span>
                    Generating...
                  </>
                ) : (
                  <>
                    <span>⚡</span>
                    Generate Reply
                  </>
                )}
              </button>
              <button 
                className="btn btn-secondary"
                onClick={() => {
                  setTweetInput('');
                  setGeneratedReply(null);
                  setApiError(null);
                }}
                disabled={isGenerating || !tweetInput}
              >
                Clear
              </button>
            </div>

            {/* Error UI Card */}
            {apiError && (
              <div className="error-card">
                <span>⚠️ {apiError}</span>
                <button className="error-close-btn" onClick={() => setApiError(null)}>×</button>
              </div>
            )}

            {/* Reply Output Card */}
            {generatedReply && (
              <div className="output-card">
                <div className="output-header">
                  <span className="output-badge">BALASAN DIHASILKAN ({selectedTone})</span>
                  <div className="output-actions">
                    <button 
                      className="action-icon-btn"
                      onClick={() => handleCopy(generatedReply, 'output')}
                      title="Salin Balasan"
                    >
                      {copiedId === 'output' ? '✅' : '📋'}
                    </button>
                  </div>
                </div>
                <p className="output-text">{generatedReply}</p>
                <div className="output-metadata">
                  <span>Engine: {usedModel || selectedModel}</span>
                  <span>•</span>
                  <span>{generatedReply.split(/\s+/).filter(Boolean).length} Kata</span>
                </div>
              </div>
            )}
          </section>

          {/* History Panel */}
          <section className="glass-panel">
            <div className="history-header">
              <h2 className="panel-title" style={{ margin: 0, border: 'none', padding: 0 }}>
                <span>📜</span> Riwayat Balasan
              </h2>
              {historyLog.length > 0 && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <span className="history-count">{historyLog.length} Balasan</span>
                  <button className="clear-history-btn" onClick={handleClearHistory}>Hapus Semua</button>
                </div>
              )}
            </div>

            <div className="history-list">
              {historyLog.length === 0 ? (
                <div className="history-empty">
                  <span>📭</span>
                  <p>Belum ada riwayat balasan. Balasan Anda akan disimpan di sini.</p>
                </div>
              ) : (
                historyLog.map(item => (
                  <article key={item.id} className="history-item">
                    <div className="history-item-header">
                      <p className="history-item-tweet" title={item.tweetContent}>
                        Tweet: "{item.tweetContent}"
                      </p>
                      <div className="history-item-actions">
                        <button
                          className="history-item-btn"
                          onClick={() => handleCopy(item.replyContent, item.id)}
                          title="Salin"
                        >
                          {copiedId === item.id ? '✅' : '📋'}
                        </button>
                        <button
                          className="history-item-btn delete"
                          onClick={() => handleDeleteHistory(item.id)}
                          title="Hapus"
                        >
                          🗑️
                        </button>
                      </div>
                    </div>
                    <p className="history-item-reply">{item.replyContent}</p>
                    <div className="history-item-footer">
                      <div className="history-item-tags">
                        <span className="history-tag tone">{item.toneChosen}</span>
                        <span className="history-tag">{item.modelUsed}</span>
                      </div>
                      <span>{item.timestamp}</span>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>

        </main>
      </div>

      {/* Footer */}
      <footer className="app-footer">
        <p>CT Reply Guy Web App © 2026. dibuat oleh Roziqin</p>
      </footer>
    </div>
  );
}
