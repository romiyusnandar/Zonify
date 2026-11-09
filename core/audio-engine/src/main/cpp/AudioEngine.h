#pragma once

#include "ParametricEqualizer.h"
#include <aaudio/AAudio.h>
#include <atomic>
#include <memory>

class AudioEngine {
public:
    static AudioEngine& getInstance();

    bool initialize(int32_t sampleRate, int32_t channelCount, bool bitPerfect);
    void release();

    void start();
    void pause();
    void stop();

    void setBitPerfect(bool enabled);
    bool isBitPerfect() const { return bitPerfectMode.load(); }

    int32_t getCurrentSampleRate() const { return currentSampleRate.load(); }
    int32_t getChannelCount() const { return currentChannels.load(); }

    ParametricEqualizer& getEqualizer() { return equalizer; }

private:
    AudioEngine();
    ~AudioEngine();

    AAudioStream* stream = nullptr;
    std::atomic<bool> isRunning{false};
    std::atomic<bool> bitPerfectMode{false};
    std::atomic<int32_t> currentSampleRate{44100};
    std::atomic<int32_t> currentChannels{2};

    ParametricEqualizer equalizer;

    static aaudio_data_callback_result_t dataCallback(
        AAudioStream* stream,
        void* userData,
        void* audioData,
        int32_t numFrames
    );

    static void errorCallback(
        AAudioStream* stream,
        void* userData,
        aaudio_result_t error
    );
};
