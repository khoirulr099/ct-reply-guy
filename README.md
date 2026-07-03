# CT Reply Guy - AI Twitter Agent

Proyek ini berisi aplikasi **Android** dan aplikasi **Web (React + Vite)** untuk menghasilkan balasan Twitter (X) secara otomatis dan cerdas menggunakan berbagai model AI (Gemini, Claude, GPT, dan DeepSeek).

---

## 🌐 1. Web Application (React + Vite)

Aplikasi Web dirancang dengan tampilan antarmuka bertema gelap (*cyberpunk degen*) modern menggunakan glassmorphism, sangat responsif, dan berjalan sepenuhnya di sisi klien (*client-side*).

### Fitur Utama:
* **Pilihan Karakter / Nada Bicara**: Degen, Alpha Hunter, Shitposter, Casual, dan Organic.
* **Mendukung Banyak Model**: Gemini 1.5/2.0/2.5/3.5, GPT-4o, Claude 3.5, DeepSeek V3/R1 (baik langsung maupun via OpenRouter).
* **Riwayat Balasan**: Otomatis tersimpan secara lokal di browser (`localStorage`) Anda.
* **Auto-Healing**: Sistem fallback otomatis pada Gemini jika model utama terkena batas limit/error.

### Cara Menjalankan Lokal:
1. Masuk ke folder `web`:
   ```bash
   cd web
   ```
2. Instal semua dependensi:
   ```bash
   npm install
   ```
3. Jalankan server lokal:
   ```bash
   npm run dev
   ```
4. Buka browser Anda dan akses: [http://localhost:3007](http://localhost:3007)


---

## 🤖 2. Android Application (Kotlin)

Aplikasi Android bawaan asli (*native*) yang siap diimpor ke Android Studio.

### Langkah Menjalankan:
1. Buka **Android Studio**.
2. Pilih **Open** dan arahkan ke folder utama proyek ini.
3. Buat file bernama `.env` di direktori utama proyek Android dan tambahkan kunci API Gemini Anda:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
4. Hapus baris berikut dari file `build.gradle.kts` di dalam folder `app`:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
5. Jalankan aplikasi di emulator atau perangkat fisik Anda.

---

*Dibuat oleh Roziqin.*
