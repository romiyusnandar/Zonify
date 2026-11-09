#include "ParametricEqualizer.h"
#include <algorithm>

ParametricEqualizer::ParametricEqualizer()
    : sampleRate(44100.0f),
      preampLinear(1.0f),
      enabled(false) {}

void ParametricEqualizer::setSampleRate(float sr) {
    std::lock_guard<std::mutex> lock(filterMutex);
    sampleRate = sr;
    recomputeFilters();
}

void ParametricEqualizer::setPreamp(float preampDb) {
    std::lock_guard<std::mutex> lock(filterMutex);
    preampLinear = std::pow(10.0f, preampDb / 20.0f);
}

void ParametricEqualizer::setEnabled(bool en) {
    std::lock_guard<std::mutex> lock(filterMutex);
    enabled = en;
}

void ParametricEqualizer::setBands(const std::vector<BandConfig>& bands) {
    std::lock_guard<std::mutex> lock(filterMutex);
    currentBands = bands;
    recomputeFilters();
}

void ParametricEqualizer::recomputeFilters() {
    leftFilters.clear();
    rightFilters.clear();

    for (const auto& band : currentBands) {
        if (!band.enabled) continue;

        BiquadFilter lFilter;
        lFilter.configure(band.type, sampleRate, band.frequency, band.gainDb, band.q);
        leftFilters.push_back(lFilter);

        BiquadFilter rFilter;
        rFilter.configure(band.type, sampleRate, band.frequency, band.gainDb, band.q);
        rightFilters.push_back(rFilter);
    }
}

void ParametricEqualizer::process(float* buffer, int numFrames, int channels) {
    if (!enabled || (leftFilters.empty() && rightFilters.empty() && preampLinear == 1.0f)) {
        return;
    }

    std::lock_guard<std::mutex> lock(filterMutex);

    // Apply preamp gain
    if (preampLinear != 1.0f) {
        for (int i = 0; i < numFrames * channels; ++i) {
            buffer[i] *= preampLinear;
        }
    }

    // Process Left Channel (channel 0)
    for (auto& filter : leftFilters) {
        filter.process(buffer, numFrames, channels, 0);
    }

    // Process Right Channel (channel 1 if stereo)
    if (channels > 1) {
        for (auto& filter : rightFilters) {
            filter.process(buffer, numFrames, channels, 1);
        }
    }

    // Soft-knee limiter to prevent digital clipping (0 dBFS ceiling)
    for (int i = 0; i < numFrames * channels; ++i) {
        buffer[i] = std::clamp(buffer[i], -1.0f, 1.0f);
    }
}
