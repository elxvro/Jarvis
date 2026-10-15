package com.elxvro.jarvis

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-selected GGUF is loaded locally through the official llama.cpp Android JNI
 * library. This class makes no network calls and does not store prompts remotely.
 */
class LocalAiBridge(context: Context, private val listener: Listener) {
    interface Listener {
        fun onStatus(message: String)
        fun onToken(text: String)
        fun onDone()
        fun onError(message: String)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine: InferenceEngine = AiChat.getInferenceEngine(context.applicationContext)
    @Volatile private var modelReady = false
    @Volatile private var busy = false

    fun loadModel(filePath: String) {
        if (busy) {
            listener.onStatus("Bir işlem sürüyor. Tamamlanmasını bekle.")
            return
        }
        busy = true
        modelReady = false
        scope.launch {
            try {
                report("Yerel model yükleniyor. İlk yükleme biraz sürebilir...")
                engine.loadModel(filePath)
                engine.setSystemPrompt(
                    "Sen ELXVRO MIA isimli Türkçe konuşan, kullanıcının kendi telefonunda çalışan " +
                    "yardımcı bir yapay zekâsın. Kısa, açık ve doğru Türkçe cevap ver. " +
                    "Kullanıcı duraksayabilir; bunu sorun etme. Telefon, Instagram, web veya " +
                    "Google Play hesaplarında bir işlem yaptığını asla iddia etme; " +
                    "hesaplara henüz bağlı değilsin. Cevapların cihazdan dışarı çıkmaz."
                )
                modelReady = true
                report("MIA yerel yapay zekâ hazır. Artık internetsiz soru sorabilirsin.")
            } catch (error: Exception) {
                modelReady = false
                reportError("Model yüklenemedi: " + (error.message ?: error.javaClass.simpleName))
            } finally {
                busy = false
            }
        }
    }

    fun ask(question: String) {
        if (question.isBlank()) {
            listener.onStatus("Bir soru yaz.")
            return
        }
        if (!modelReady) {
            listener.onStatus("Önce bir GGUF modeli seçip yükle.")
            return
        }
        if (busy) {
            listener.onStatus("MIA hâlâ yanıt üretiyor.")
            return
        }
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.Main) { listener.onToken("") }
                engine.sendUserPrompt(question, 256).collect { token ->
                    withContext(Dispatchers.Main) { listener.onToken(token) }
                }
                withContext(Dispatchers.Main) { listener.onDone() }
            } catch (error: Exception) {
                reportError("Yerel yanıt hatası: " + (error.message ?: error.javaClass.simpleName))
            } finally {
                busy = false
            }
        }
    }

    private suspend fun report(message: String) = withContext(Dispatchers.Main) {
        listener.onStatus(message)
    }

    private suspend fun reportError(message: String) = withContext(Dispatchers.Main) {
        listener.onError(message)
    }

    fun close() {
        scope.cancel()
        // Clean up model memory when app closes. Runs on a worker thread.
        Thread { try { engine.cleanUp() } catch (_: Exception) {} }.start()
    }
}
