#pragma once

#include <cmath>

enum class FilterType {
    PEAKING = 0,
    LOW_SHELF = 1,
    HIGH_SHELF = 2,
    LOW_PASS = 3,
    HIGH_PASS = 4,
    BAND_PASS = 5,
    NOTCH = 6
};

class BiquadFilter {
public:
    BiquadFilter();
    ~BiquadFilter() = default;

    void configure(FilterType type, float sampleRate, float frequency, float gainDb, float q);
    void reset();

    // In-place processing for float buffer (interleaved stereo or mono channel)
    void process(float* buffer, int numFrames, int channels, int channelIndex);

private:
    float a0, a1, a2, b1, b2;
    float x1, x2, y1, y2;
    bool enabled;
};
