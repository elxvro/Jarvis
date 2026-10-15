# ELXVRO JARVIS v0.1.1

Android 16 odaklı kişisel, çevrimdışı Türkçe sesli uyandırma prototipi.

## Yetenekler (bu sürümde gerçek)
- **Yeni v0.1.1:** Android TextToSpeech ile Türkçe sesli yanıt ve uygulama içi **Sesi test et** düğmesi.
- **Yeni v0.1.1:** Jarvis başlatıldığında, “Hey Jarvis” uyandırıldığında ve komut alındığında sesli yanıt; kendi konuşmasını algılamaması için mikrofon tanımayı geçici durdurma.
- Kullanıcı uygulamadayken başlatılan mikrofon türünde foreground service.
- Ekran kilitlendikten sonra (işletim sistemi izin verdiği sürece) çevrimdışı Vosk ile "Hey Jarvis" tespiti.
- "Instagram", "Play Console", "site", "arama", "yardım", "dur" komutlarını sınıflandırma.
- Ekran kilitliyken başka uygulama açmaya çalışmak yerine kullanıcı dokunmalı bildirim.
- İlk kullanımda 35 MB civarında ücretsiz Türkçe modeli kullanıcı isteğiyle indirme; sonrasında konuşma tanıma çevrimdışı çalışır.
- Güncel dinleme bildirimi ve durdurma eylemi.

## Bu sürümde YOK
Google Play Console API ile uygulama yayınlama, Instagram Graph API paylaşımı, hosting SSH/SFTP/FTP ile site dosyası düzenleme, otomatik rehber araması veya kendiliğinden telefon görüşmesi başlatma. Bunlar ayrı güvenli entegrasyonlar gerektirir. Bu uygulama henüz genel amaçlı yapay zekâ değil; yerel ses komutları için bir altyapıdır.

## Ücretsiz derleme (telefondan)
1. Bu depodaki **Actions** sekmesine git; "Build debug APK" iş akışını çalıştır veya değişiklikleri main'e aktar.
2. İşlem tamamlandığında **ELXVRO-JARVIS-debug-APK** artefaktını indir.
3. ZIP içindeki \`app-debug.apk\` dosyasını Android'e yükle (bilinmeyen kaynaktan yükleme izni gerekebilir).
4. Uygulamayı aç. Bildirim ve mikrofon izinlerini ver.
5. "Türkçe modeli indir" düğmesine bas; internet yalnızca model indirme için gerekir.
6. Model tamamlanınca "Dinlemeyi başlat" düğmesine bas, ekranı kapat, "Hey Jarvis" de. Ardından "Instagram", "Play Console", "site" veya "yardım" de.

> Android 14+ üzerinde bir mikrofon foreground service'i arka planda keyfi olarak başlatılamaz. Önce uygulama görünürken etkinleştirmek gerekir. Sistem, OEM pil optimizasyonu veya kullanıcı servisi durdurursa, uygulamadan yeniden başlatılmalıdır. Uygulama kendi kendine yeniden başlatılmayacak; açılışta mikrofon dinlemeye başlamaz.

## Mimarisi
- Java Android, minSdk 26, targetSdk 36, Java 17
- Vosk Android 0.3.70 + özgür Türkçe model [vosk-model-small-tr-0.3](https://alphacephei.com/vosk/models)
- Gradle 8.13 / Android Gradle Plugin 8.11.1
- Özel backend / API anahtarı yok. Şifre istemez ve saklamaz.
- Ses kayıtları ve transkriptler diske veya sunucuya kaydedilmez; mikrofondan gerçek zamanlı yerel işlenir.
- Sesli yanıt için **Android TTS** sistemine ihtiyaç vardır. Türkçe konuşma ses paketi telefonda mevcut değilse cihaz ayarlarından indirilmelidir. Çevrimdışı TTS ancak yüklü offline Türkçe sesi olan cihazlarda garanti edilebilir.

## Sürüm yükseltme uyarısı
GitHub Actions **debug** imza anahtarını her çalışmada yeniden üretebilir. v0.1.0 kuruluysa ve v0.1.1 'yüklenemedi' diyorsa eskisini kaldırıp bu APK'yı yükle (uygulamanın yerel indirdiği Vosk modeli silinir ve yeniden yüklenmelidir). Üretim sürümünde kullanıcıya özel kalıcı bir imza anahtarı gereklidir.

## Güvenlik ve sınırlamalar
- Sürekli mikrofon dinleme batarya kullanır. Vosk yazılımsal konuşma tanımadır, donanımsal düşük-güç DSP "hotword" değildir.
- Tanınan sözcükler kusursuz olmayabilir; ilk sürümde "Hey Jarvis" bazı aksanlarda kaçabilir.
- Dinleme, kullanıcı tarafından verilen izinle ve kalıcı bildirim altında sürer. İstendiğinde hemen durdurulur.
- Telefon kilidini aşmaz; işlem için kullanıcı etkileşimi gerektiğinde bildirim açılır.
- APK **debug imzalıdır**, Play Store dağıtımı için değildir. Uygulama güncellemeleri için aynı imzanın korunması gerekir; GitHub Actions debug anahtarı her derlemede değişebilir, bunun için sonraki sürümde sabit özel imza gerekir.

## Sonraki sürüm
İzinli rehberden arama, Instagram profesyonel hesap OAuth, Google Play Developer API servis hesabı yetkilendirmesi, hosting için SSH/SFTP tabanlı dar yetkili erişim ve kritik işlemlerde onay akışı.

## Kaynaklar
- Android FGS: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Vosk modelleri: https://alphacephei.com/vosk/models
- Android Gradle Plugin: https://developer.android.com/build/releases/about-agp
