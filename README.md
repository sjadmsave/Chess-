# أرينا الشطرنج — تطبيق شطرنج ذكي باللغة العربية (Kotlin & Jetpack Compose)

تطبيق شطرنج متكامل يعمل **100% بدون إنترنت**، يضم:
- محرك لعب وتحليل متدرج المستويات (`200` إلى `3000` إيلو) مع **10 شخصيات محرك** و **8 أساتذة عالميين**.
- دعم كامل لبروتوكول **Stockfish UCI** (مع الاكتشاف التلقائي لملف `libstockfish.so` أو محركات OEX، ومحرك مدمج احتياطي بمعمارية Zobrist TT + PVS + Null-Move + LMR).
- وضع ملء الشاشة الغامر، النافذة العائمة فوق التطبيقات، سجل النقلات الجبري `SAN`، استيراد/تصدير `PGN / FEN`، والألغاز اليومية.

---

## البناء التلقائي عبر GitHub Actions

المشروع مجهّز بملف سير عمل جاهز في:
`.github/workflows/android-build.yml`

بمجرد رفع المشروع إلى GitHub (أو تشغيله يدوياً من تبويب **Actions**):
1. يتم فك تشفير `debug.keystore.base64` تلقائياً.
2. يقوم سير العمل (اختيارياً وبشكل افتراضي) باستنساخ **Stockfish C++** الرسمي وبنائه عبر **Android NDK** لمعماريتي `arm64-v8a` و`armeabi-v7a` وحزمه داخل `app/src/main/jniLibs/<abi>/libstockfish.so`.
3. تشغيل الاختبارات `./gradlew testDebugUnitTest`.
4. بناء ملف التثبيت `APK` ورفعه في قسم **Artifacts** باسم `EliteChess-Debug-APK`.

### بناء نسخة Release الموقّعة (اختياري)
لبناء ملف `Release APK` و `AAB` عبر GitHub، أضف المتغيرات التالية في **Settings → Secrets and variables → Actions**:
- `KEYSTORE_BASE64`: محتوى ملف مفتاح التوقيع `.jks` مشفّراً بصيغة Base64 (`keyAlias = "upload"`).
- `STORE_PASSWORD`: كلمة مرور مخزن المفاتيح.
- `KEY_PASSWORD`: كلمة مرور المفتاح.

---

## البناء المحلي (Android Studio / سطر الأوامر)

1. فك تشفير مفتاح التجربة قبل أول بناء محلي:
   - Linux / macOS:
     ```bash
     base64 -d debug.keystore.base64 > debug.keystore
     chmod +x gradlew
     ./gradlew assembleDebug
     ```
   - Windows (PowerShell):
     ```powershell
     [IO.File]::WriteAllBytes("debug.keystore", [Convert]::FromBase64String((Get-Content debug.keystore.base64 -Raw)))
     .\gradlew.bat assembleDebug
     ```
2. (اختياري) لتضمين محرك **Stockfish C++** محلياً، ضع ملف `libstockfish.so` داخل:
   `app/src/main/jniLibs/arm64-v8a/libstockfish.so`
