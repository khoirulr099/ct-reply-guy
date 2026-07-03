import React, { useState, useEffect } from 'react';
import { MODELS, TONES, generateReply } from './utils/replyGenerator';

const STORAGE_KEYS = {
  PROFILES: 'ct_reply_guy_profiles_list',
  ACTIVE_PROFILE_ID: 'ct_reply_guy_active_profile_id',
  TONE: 'ct_reply_guy_tone',
  HISTORY: 'ct_reply_guy_history',
  // Old keys for migration
  OLD_API_KEY: 'ct_reply_guy_api_key',
  OLD_BASE_URL: 'ct_reply_guy_base_url',
  OLD_MODEL: 'ct_reply_guy_model'
};

export default function App() {
  // Load Profiles & Migration Logic
  const [profiles, setProfiles] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEYS.PROFILES);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (e) {
      console.error("Failed to parse profiles", e);
    }

    // Migration from old single-profile settings if they exist
    const oldKey = localStorage.getItem(STORAGE_KEYS.OLD_API_KEY) || '';
    const oldBase = localStorage.getItem(STORAGE_KEYS.OLD_BASE_URL) || '';
    const oldModel = localStorage.getItem(STORAGE_KEYS.OLD_MODEL) || 'gemini-3.5-flash';

    return [
      {
        id: 'default-profile',
        name: 'Profil Utama',
        apiKey: oldKey,
        customBaseUrl: oldBase,
        model: oldModel,
        isCustomModel: !MODELS.some(m => m.value === oldModel)
      }
    ];
  });

  const [activeProfileId, setActiveProfileId] = useState(() => {
    return localStorage.getItem(STORAGE_KEYS.ACTIVE_PROFILE_ID) || 'default-profile';
  });

  // Current active profile
  const activeProfile = profiles.find(p => p.id === activeProfileId) || profiles[0] || {
    id: 'default-profile',
    name: 'Profil Utama',
    apiKey: '',
    customBaseUrl: '',
    model: 'gemini-3.5-flash',
    isCustomModel: false
  };

  // Profile Editor Form State (binds to activeProfile and changes locally before saving)
  const [profileName, setProfileName] = useState(activeProfile.name);
  const [profileApiKey, setProfileApiKey] = useState(activeProfile.apiKey);
  const [profileBaseUrl, setProfileBaseUrl] = useState(activeProfile.customBaseUrl);
  const [profileModel, setProfileModel] = useState(activeProfile.model);
  const [isCustomModel, setIsCustomModel] = useState(activeProfile.isCustomModel);
  
  // Collapsible setting for Custom Base URL
  const [showBaseUrl, setShowBaseUrl] = useState(!!activeProfile.customBaseUrl);

  // Sync editor form when active profile changes
  useEffect(() => {
    setProfileName(activeProfile.name);
    setProfileApiKey(activeProfile.apiKey);
    setProfileBaseUrl(activeProfile.customBaseUrl);
    setProfileModel(activeProfile.model);
    setIsCustomModel(activeProfile.isCustomModel);
    setShowBaseUrl(!!activeProfile.customBaseUrl);
  }, [activeProfileId, profiles]);

  // Tone Selection
  const [selectedTone, setSelectedTone] = useState(() => localStorage.getItem(STORAGE_KEYS.TONE) || 'Degen');

  // Input & Generation State
  const [tweetInput, setTweetInput] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedReply, setGeneratedReply] = useState(null);
  const [usedModel, setUsedModel] = useState(null);
  const [apiError, setApiError] = useState(null);

  // UI state
  const [copiedId, setCopiedId] = useState(null);
  const [profileSaveSuccess, setProfileSaveSuccess] = useState(false);
  const [historyLog, setHistoryLog] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.HISTORY)) || [];
    } catch {
      return [];
    }
  });

  // Persist values
  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.PROFILES, JSON.stringify(profiles));
  }, [profiles]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.ACTIVE_PROFILE_ID, activeProfileId);
  }, [activeProfileId]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.TONE, selectedTone);
  }, [selectedTone]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.HISTORY, JSON.stringify(historyLog));
  }, [historyLog]);

  // Create new profile helper
  const handleCreateProfile = () => {
    const newId = `profile-${Date.now()}`;
    const newProfile = {
      id: newId,
      name: `Profil Baru #${profiles.length + 1}`,
      apiKey: '',
      customBaseUrl: '',
      model: 'gemini-3.5-flash',
      isCustomModel: false
    };

    setProfiles(prev => [...prev, newProfile]);
    setActiveProfileId(newId);
  };

  // Save profile changes to the list
  const handleSaveProfile = () => {
    setProfiles(prev => prev.map(p => {
      if (p.id === activeProfileId) {
        return {
          ...p,
          name: profileName.trim() || 'Tanpa Nama',
          apiKey: profileApiKey.trim(),
          customBaseUrl: profileBaseUrl.trim(),
          model: profileModel.trim() || 'gemini-3.5-flash',
          isCustomModel: isCustomModel
        };
      }
      return p;
    }));

    setProfileSaveSuccess(true);
    setTimeout(() => setProfileSaveSuccess(false), 2000);
  };

  // Delete profile helper
  const handleDeleteProfile = (idToDelete) => {
    if (profiles.length <= 1) {
      alert("Anda harus menyisakan minimal satu profil!");
      return;
    }

    if (window.confirm(`Hapus profil "${activeProfile.name}"?`)) {
      const remaining = profiles.filter(p => p.id !== idToDelete);
      setProfiles(remaining);
      setActiveProfileId(remaining[0].id);
    }
  };

  // Command interceptor matching Kotlin logic: e.g. [menu:show status] or [menu:reset to default]
  const handleCommandInterception = (input) => {
    const trimmed = input.trim();
    if (trimmed.startsWith('[menu:') && trimmed.endsWith(']')) {
      const command = trimmed.substring(6, trimmed.length - 1).trim();
      setApiError(null);

      if (command.startsWith('insert api key =')) {
        const keyVal = command.split('insert api key =')[1].trim().replace(/^["']|["']$/g, '');
        setProfileApiKey(keyVal);
        setProfiles(prev => prev.map(p => p.id === activeProfileId ? { ...p, apiKey: keyVal } : p));
        setGeneratedReply('status: universal api key registered successfully.');
      } else if (command.startsWith('insert api base =')) {
        const baseVal = command.split('insert api base =')[1].trim().replace(/^["']|["']$/g, '');
        setProfileBaseUrl(baseVal);
        setProfiles(prev => prev.map(p => p.id === activeProfileId ? { ...p, customBaseUrl: baseVal } : p));
        setGeneratedReply('status: custom api base URL registered.');
      } else if (command.startsWith('select model =')) {
        const modelVal = command.split('select model =')[1].trim().replace(/^["']|["']$/g, '');
        setProfileModel(modelVal);
        setIsCustomModel(!MODELS.some(m => m.value === modelVal));
        setProfiles(prev => prev.map(p => p.id === activeProfileId ? { 
          ...p, 
          model: modelVal,
          isCustomModel: !MODELS.some(m => m.value === modelVal)
        } : p));
        setGeneratedReply(`status: switched engine to ${modelVal}.`);
      } else if (command === 'show status') {
        setGeneratedReply(`active profile: ${activeProfile.name} | model: ${activeProfile.model} | api key: ${activeProfile.apiKey ? 'configured (hidden)' : 'not configured'} | base: ${activeProfile.customBaseUrl || 'default'}`);
      } else if (command === 'reset to default') {
        setProfileApiKey('');
        setProfileModel('gemini-3.5-flash');
        setProfileBaseUrl('');
        setIsCustomModel(false);
        setProfiles(prev => prev.map(p => p.id === activeProfileId ? {
          ...p,
          apiKey: '',
          model: 'gemini-3.5-flash',
          customBaseUrl: '',
          isCustomModel: false
        } : p));
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

    // Auto-save unsaved profile configurations on generate if changed
    const hasUnsavedChanges = 
      profileName !== activeProfile.name ||
      profileApiKey !== activeProfile.apiKey ||
      profileBaseUrl !== activeProfile.customBaseUrl ||
      profileModel !== activeProfile.model ||
      isCustomModel !== activeProfile.isCustomModel;

    if (hasUnsavedChanges) {
      handleSaveProfile();
    }

    setApiError(null);
    setGeneratedReply(null);
    setIsGenerating(true);

    try {
      const result = await generateReply({
        tweet: tweetInput,
        model: profileModel,
        tone: selectedTone,
        apiKey: profileApiKey,
        customBaseUrl: profileBaseUrl
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
        <div className={`api-status-badge ${profileApiKey ? 'connected' : 'disconnected'}`}>
          <span className="status-dot"></span>
          {profileApiKey ? `PROFIL: ${profileName.toUpperCase()}` : 'API KEY BELUM DISET'}
        </div>
      </header>

      {/* Grid Dashboard */}
      <div className="dashboard-grid">
        
        {/* Left Column: Settings Panel (Profiles) */}
        <aside className="glass-panel">
          <div className="profile-selector-section">
            <h2 className="panel-title" style={{ border: 'none', marginBottom: '1rem', paddingBottom: 0 }}>
              <span>⚙️</span> Profil Konfigurasi
            </h2>
            
            {/* Choose Profile Dropdown & New Button */}
            <div className="profile-selection-row">
              <select
                className="select-input"
                style={{ flex: 1 }}
                value={activeProfileId}
                onChange={(e) => setActiveProfileId(e.target.value)}
              >
                {profiles.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
              <button 
                className="action-icon-btn" 
                onClick={handleCreateProfile} 
                title="Tambah Profil Baru"
                style={{ height: '42px', width: '42px' }}
              >
                ➕
              </button>
            </div>
          </div>

          {/* Profile Details Editor */}
          <div className="config-group" style={{ marginTop: '1.5rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)' }}>
            
            {/* Profile Name Input */}
            <div className="form-field">
              <label className="form-label" htmlFor="profile-name">
                Nama Profil
              </label>
              <div className="input-wrapper">
                <span className="input-icon">👤</span>
                <input
                  id="profile-name"
                  className="text-input"
                  type="text"
                  placeholder="Contoh: Gemini Personal, OpenRouter..."
                  value={profileName}
                  onChange={(e) => setProfileName(e.target.value)}
                />
              </div>
            </div>

            {/* API Key Input */}
            <div className="form-field">
              <label className="form-label" htmlFor="api-key">
                API Key
                <span className="label-info">Disimpan di browser Anda</span>
              </label>
              <div className="input-wrapper">
                <span className="input-icon">🔑</span>
                <input
                  id="api-key"
                  className="text-input"
                  type="password"
                  placeholder="Masukkan API Key..."
                  value={profileApiKey}
                  onChange={(e) => setProfileApiKey(e.target.value)}
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
                  <span className="label-info">Kosongkan jika menggunakan Gemini langsung</span>
                </label>
                <div className="input-wrapper">
                  <span className="input-icon">🌐</span>
                  <input
                    id="custom-url"
                    className="text-input"
                    type="text"
                    placeholder="https://api.openai.com/v1"
                    value={profileBaseUrl}
                    onChange={(e) => setProfileBaseUrl(e.target.value)}
                  />
                </div>
              </div>
            )}

            {/* Model Select Options Toggle */}
            <div className="form-field">
              <div className="form-label">
                <span>Engine / Model</span>
                <button 
                  className="settings-toggle" 
                  onClick={() => setIsCustomModel(!isCustomModel)}
                  style={{ textDecoration: 'underline' }}
                >
                  {isCustomModel ? 'Pilih dari Daftar' : 'Tulis Manual'}
                </button>
              </div>

              {isCustomModel ? (
                // Custom model manual text input
                <div className="input-wrapper" style={{ animation: 'slide-in 0.25s ease-out' }}>
                  <span className="input-icon">⚙️</span>
                  <input
                    className="text-input"
                    type="text"
                    placeholder="Contoh: grok-beta, deepseek-chat..."
                    value={profileModel}
                    onChange={(e) => setProfileModel(e.target.value)}
                  />
                </div>
              ) : (
                // Predefined dropdown list
                <select
                  className="select-input"
                  value={MODELS.some(m => m.value === profileModel) ? profileModel : 'gemini-3.5-flash'}
                  onChange={(e) => setProfileModel(e.target.value)}
                >
                  {MODELS.map(m => (
                    <option key={m.value} value={m.value}>
                      {m.label} ({m.provider.toUpperCase()})
                    </option>
                  ))}
                </select>
              )}
            </div>

            {/* Save & Delete Profile Buttons */}
            <div className="profile-actions-row">
              <button 
                className="btn btn-primary" 
                onClick={handleSaveProfile}
                style={{ flex: 1, padding: '0.7rem' }}
              >
                {profileSaveSuccess ? '✅ Tersimpan' : '💾 Simpan Profil'}
              </button>
              <button 
                className="btn btn-secondary" 
                onClick={() => handleDeleteProfile(activeProfileId)}
                disabled={profiles.length <= 1}
                title="Hapus Profil Ini"
                style={{ padding: '0.7rem 1rem' }}
              >
                🗑️
              </button>
            </div>

            {/* Tone Card Grid */}
            <div className="form-field" style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
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
                disabled={isGenerating || !tweetInput.trim() || (!profileApiKey && !tweetInput.trim().startsWith('[menu:'))}
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
                  <span>Profil: {activeProfile.name}</span>
                  <span>•</span>
                  <span>Engine: {usedModel || profileModel}</span>
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
