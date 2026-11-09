#include "AudioEngine.h"
#include <android/log.h>

#define TAG "ZonifyAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

AudioEngine& AudioEngine::getInstance() {
    static AudioEngine instance;
    return instance;
}

AudioEngine::AudioEngine() = default;

AudioEngine::~AudioEngine() {
    release();
}

bool AudioEngine::initialize(int32_t sampleRate, int32_t channelCount, bool bitPerfect) {
    release();

    currentSampleRate.store(sampleRate);
    currentChannels.store(channelCount);
    bitPerfectMode.store(bitPerfect);
    equalizer.setSampleRate(static_cast<float>(sampleRate));

    AAudioStreamBuilder* builder = nullptr;
    aaudio_result_t result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) {
        LOGE("Failed to create AAudio stream builder: %d", result);
        return false;
    }

    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setSampleRate(builder, sampleRate);
    AAudioStreamBuilder_setChannelCount(builder, channelCount);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);

    // Audiophile low latency and exclusive mode for Direct bit-perfect rendering
    if (bitPerfect) {
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
        AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    } else {
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
        AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    }

    AAudioStreamBuilder_setDataCallback(builder, dataCallback, this);
    AAudioStreamBuilder_setErrorCallback(builder, errorCallback, this);

    result = AAudioStreamBuilder_openStream(builder, &stream);
    AAudioStreamBuilder_delete(builder);

    if (result != AAUDIO_OK) {
        LOGE("Failed to open AAudio stream: %d", result);
        return false;
    }

    LOGI("AAudio stream opened successfully. SampleRate: %d, Channels: %d, Bit-Perfect: %s",
         sampleRate, channelCount, bitPerfect ? "YES" : "NO");
    return true;
}

void AudioEngine::release() {
    stop();
    if (stream) {
        AAudioStream_close(stream);
        stream = nullptr;
    }
}

void AudioEngine::start() {
    if (stream && !isRunning.load()) {
        aaudio_result_t res = AAudioStream_requestStart(stream);
        if (res == AAUDIO_OK) {
            isRunning.store(true);
            LOGI("AAudio stream started.");
        } else {
            LOGE("Failed to start AAudio stream: %d", res);
        }
    }
}

void AudioEngine::pause() {
    if (stream && isRunning.load()) {
        AAudioStream_requestPause(stream);
        isRunning.store(false);
    }
}

void AudioEngine::stop() {
    if (stream && isRunning.load()) {
        AAudioStream_requestStop(stream);
        isRunning.store(false);
    }
}

void AudioEngine::setBitPerfect(bool enabled) {
    if (bitPerfectMode.load() != enabled) {
        bitPerfectMode.store(enabled);
        // Re-initialize stream with new sharing mode
        initialize(currentSampleRate.load(), currentChannels.load(), enabled);
        if (isRunning.load()) {
            start();
        }
    }
}

aaudio_data_callback_result_t AudioEngine::dataCallback(
    AAudioStream* /*stream*/,
    void* userData,
    void* audioData,
    int32_t numFrames
) {
    auto* engine = static_cast<AudioEngine*>(userData);
    auto* floatBuffer = static_cast<float*>(audioData);

    // Process through Parametric Equalizer / DSP
    engine->equalizer.process(floatBuffer, numFrames, engine->currentChannels.load());

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AudioEngine::errorCallback(
    AAudioStream* /*stream*/,
    void* /*userData*/,
    aaudio_result_t error
) {
    LOGE("AAudio error received: %d", error);
}
