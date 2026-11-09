#pragma once

#include "BiquadFilter.h"
#include <vector>
#include <memory>
#include <mutex>

struct BandConfig {
    int id;
    bool enabled;
    FilterType type;
    float frequency;
    float gainDb;
    float q;
};

class ParametricEqualizer {
public:
    ParametricEqualizer();
    ~ParametricEqualizer() = default;

    void setSampleRate(float sampleRate);
    void setPreamp(float preampDb);
    void setBands(const std::vector<BandConfig>& bands);
    void setEnabled(bool enabled);
    bool isEnabled() const { return enabled; }

    // Process interleaved stereo 32-bit float audio buffer
    void process(float* buffer, int numFrames, int channels);

private:
    float sampleRate;
    float preampLinear;
    bool enabled;
    std::mutex filterMutex;

    // Separate filter instances for Left (0) and Right (1) channels
    std::vector<BiquadFilter> leftFilters;
    std::vector<BiquadFilter> rightFilters;
    std::vector<BandConfig> currentBands;

    void recomputeFilters();
};
