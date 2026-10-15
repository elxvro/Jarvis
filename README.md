# ELXVRO MIA v0.2.1

Android 16 odaklı kişisel, çevrimdışı Türkçe sesli uyandırma prototipi.

## Mia v0.2.1 — Türkçe tanıma modeli onarımı
- Eski Türkçe Vosk modeli **legacy** `final.mdl / mfcc.conf / HCLr.fst / Gr.fst` klasör yapısındadır. Önceki uygulama yanlışlıkla `am/final.mdl / conf/model.conf` kontrolü yaptığı için “İndirilen model geçersiz” hatası oluşuyordu.
- Uygulama artık her iki Vosk model yapısını destekliyor.
- 35 MB Türkçe model ZIP, APK derlemesinde resmî kaynaktan SHA-256 ile doğrulanarak uygulamaya ekleniyor.
- İlk açılışta otomatik ve **çevrimdışı** kurulum; tekrar indirme gerekmez. İlk kurulum cihazın hızına göre biraz sürebilir.
- Github Actions, modelin gerçek dosya düzenini ve ZIP CRC'sini doğrulamadan APK yayımlamaz.

## Mia v0.2.0 — erişilebilir kontrol
- **Tek kelimelik uyandırma:** “Mia” (alternatif telaffuzlar “miya”, “miyya”, “miiya”). “Hey” söylemek zorunlu değil.
- **Duraklama toleransı:** komut sırasında 30 saniyeye kadar sessizlik; konuşma devam ettikçe yenilenir, 90 saniyelik toplam güvenlik sınırı vardır.
- **Yazılı komut:** Instagram, konsol, site, arama, dur, yardım.
- **Büyük dokunmatik kontroller:** en az 70dp yüksekliğinde düğmeler, 18sp metin.
- **Ses yanıtları:** telefonda bulunan TTS veya uygulamaya gömülü eSpeak NG ile üretilmiş yanıtlar; donanımda ses çıkışı henüz doğrulanmadı.
- **Sınır:** tek kelimelik uyandırma, yerel Vosk'un “Mia” adını tanımasına bağlıdır. Kekemeliği güvenilir biçimde çözebilen tıbbi/kişiselleştirilmiş konuşma tanıma iddiası yoktur.
- **Gerçek entegrasyonlar henüz YOK:** Play Console yayınlama, Instagram API paylaşımı, web sitesi dosya güncelleme ve otomatik arama; düğmeler ilgili ekranları açar.

## Önceki sürüm
- **Yeni v0.1.2:** Uygulama içine yerleştirilmiş 10 Türkçe WAV ses; temel komutlar için Android TTS motoruna bağlı değil.
- WAV dosyaları GitHub Actions üzerinde ücretsiz, açık kaynak [eSpeak NG](https://github.com/espeak-ng/espeak-ng) ile oluşturulur. Ses robotik olabilir. Gelecekte dinamik ses için [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx/tree/master/android/SherpaOnnxTtsEngine) değerlendirilebilir.
- **Yeni v0.1.1:** Android TextToSpeech ile Türkçe sesli yanıt ve uygulama içi **Sesi test et** düğmesi.
- **Yeni v0.1.1:** Jarvis başlatıldığında, “Hey Jarvis” uyandırıldığında ve komut alındığında sesli yanıt; kendi konuşmasını algılamaması için mikrofon tanımayı geçici durdurma.
- Kullanıcı uygulamadayken başlatılan mikrofon türünde foreground service.
- Ekran kilitlendikten sonra (işletim sistemi izin verdiği sürece) çevrimdışı Vosk ile "Mia" tespiti.
- "Instagram", "Play Console", "site", "arama", "yardım", "dur" komutlarını sınıflandırma.
- Ekran kilitliyken başka uygulama açmaya çalışmak yerine kullanıcı dokunmalı bildirim.
- İlk kullanımda 35 MB civarında ücretsiz Türkçe modeli kullanıcı isteğiyle indirme; sonrasında konuşma tanıma çevrimdışı çalışır.
- Güncel dinleme bildirimi ve durdurma eylemi.

## Bu sürümde YOK
Google Play Console API ile uygulama yayınlama, Instagram Graph API paylaşımı, hosting SSH/SFTP/FTP ile site dosyası düzenleme, otomatik rehber araması veya kendiliğinden telefon görüşmesi başlatma. Bunlar ayrı güvenli entegrasyonlar gerektirir. Bu uygulama henüz genel amaçlı yapay zekâ değil; yerel ses komutları için bir altyapıdır.

## Ücretsiz derleme (telefondan)
1. Bu depodaki **Actions** sekmesine git; "Build debug APK" iş akışını çalıştır veya değişiklikleri main'e aktar.
2. İşlem tamamlandığında **ELXVRO-MIA-debug-APK** artefaktını indir.
3. ZIP içindeki \`app-debug.apk\` dosyasını Android'e yükle (bilinmeyen kaynaktan yükleme izni gerekebilir).
4. Uygulamayı aç. Bildirim ve mikrofon izinlerini ver.
5. Türkçe model uygulamaya gömülüdür; ilk açılışta kendiliğinden kurulur. İnternet gerekmez.
6. Model tamamlanınca "Dinlemeyi başlat" düğmesine bas, ekranı kapat, "Mia" de. Ardından "Instagram", "Play Console", "site" veya "yardım" de.

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
- Tanınan sözcükler kusursuz olmayabilir; ilk sürümde "Mia" bazı aksanlarda kaçabilir.
- Dinleme, kullanıcı tarafından verilen izinle ve kalıcı bildirim altında sürer. İstendiğinde hemen durdurulur.
- Telefon kilidini aşmaz; işlem için kullanıcı etkileşimi gerektiğinde bildirim açılır.
- APK **debug imzalıdır**, Play Store dağıtımı için değildir. Uygulama güncellemeleri için aynı imzanın korunması gerekir; GitHub Actions debug anahtarı her derlemede değişebilir, bunun için sonraki sürümde sabit özel imza gerekir.

## Derleme durumu
Bu sürümde GitHub Actions ile sesli yanıt içeren debug APK oluşturulur. Android cihazda gerçek mikrofon/hoparlör testi ayrıca yapılmalıdır.

## Sonraki sürüm
İzinli rehberden arama, Instagram profesyonel hesap OAuth, Google Play Developer API servis hesabı yetkilendirmesi, hosting için SSH/SFTP tabanlı dar yetkili erişim ve kritik işlemlerde onay akışı.

## Kaynaklar
- Android FGS: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Vosk modelleri: https://alphacephei.com/vosk/models
- Android Gradle Plugin: https://developer.android.com/build/releases/about-agp
