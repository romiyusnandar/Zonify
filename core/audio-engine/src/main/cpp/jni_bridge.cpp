#include <jni.h>
#include "AudioEngine.h"
#include <vector>

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeInit(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint sampleRate,
    jint channels,
    jboolean bitPerfect
) {
    return AudioEngine::getInstance().initialize(sampleRate, channels, bitPerfect);
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeStart(JNIEnv* /*env*/, jobject /*thiz*/) {
    AudioEngine::getInstance().start();
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativePause(JNIEnv* /*env*/, jobject /*thiz*/) {
    AudioEngine::getInstance().pause();
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeStop(JNIEnv* /*env*/, jobject /*thiz*/) {
    AudioEngine::getInstance().stop();
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeRelease(JNIEnv* /*env*/, jobject /*thiz*/) {
    AudioEngine::getInstance().release();
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeSetBitPerfect(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled
) {
    AudioEngine::getInstance().setBitPerfect(enabled);
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeSetEqualizerEnabled(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled
) {
    AudioEngine::getInstance().getEqualizer().setEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeSetPreamp(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jfloat preampDb
) {
    AudioEngine::getInstance().getEqualizer().setPreamp(preampDb);
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeUpdateBand(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint bandId,
    jboolean enabled,
    jint filterType,
    jfloat freqHz,
    jfloat gainDb,
    jfloat q
) {
    std::vector<BandConfig> singleBand = {
        {bandId, static_cast<bool>(enabled), static_cast<FilterType>(filterType), freqHz, gainDb, q}
    };
    AudioEngine::getInstance().getEqualizer().setBands(singleBand);
}

JNIEXPORT void JNICALL
Java_com_zonify_core_audio_AudioEngineBridge_nativeProcessPcmFloat(
    JNIEnv* env,
    jobject /*thiz*/,
    jobject directBuffer,
    jint numFrames,
    jint channels
) {
    if (!directBuffer) return;
    auto* floatData = static_cast<float*>(env->GetDirectBufferAddress(directBuffer));
    if (floatData) {
        AudioEngine::getInstance().getEqualizer().process(floatData, numFrames, channels);
    }
}

} // extern "C"
