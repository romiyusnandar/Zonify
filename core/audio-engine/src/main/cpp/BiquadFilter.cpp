#include "BiquadFilter.h"
#include <numbers>

BiquadFilter::BiquadFilter() 
    : a0(1.0f), a1(0.0f), a2(0.0f), b1(0.0f), b2(0.0f),
      x1(0.0f), x2(0.0f), y1(0.0f), y2(0.0f),
      enabled(false) {}

void BiquadFilter::reset() {
    x1 = x2 = y1 = y2 = 0.0f;
}

void BiquadFilter::configure(FilterType type, float sampleRate, float frequency, float gainDb, float q) {
    if (frequency <= 0.0f || frequency >= sampleRate * 0.499f || sampleRate <= 0.0f) {
        enabled = false;
        return;
    }

    constexpr float pi = 3.14159265358979323846f;
    const float omega = 2.0f * pi * (frequency / sampleRate);
    const float sn = std::sin(omega);
    const float cs = std::cos(omega);
    const float alpha = sn / (2.0f * std::max(q, 0.001f));
    const float A = std::pow(10.0f, gainDb / 40.0f); // sqrt of linear gain

    float b0 = 1.0f;

    switch (type) {
        case FilterType::PEAKING: {
            b0 = 1.0f + alpha / A;
            a0 = (1.0f + alpha * A) / b0;
            a1 = (-2.0f * cs) / b0;
            a2 = (1.0f - alpha * A) / b0;
            b1 = (-2.0f * cs) / b0;
            b2 = (1.0f - alpha / A) / b0;
            break;
        }
        case FilterType::LOW_SHELF: {
            const float beta = 2.0f * std::sqrt(A) * alpha;
            b0 = (A + 1.0f) + (A - 1.0f) * cs + beta;
            a0 = (A * ((A + 1.0f) - (A - 1.0f) * cs + beta)) / b0;
            a1 = (2.0f * A * ((A - 1.0f) - (A + 1.0f) * cs)) / b0;
            a2 = (A * ((A + 1.0f) - (A - 1.0f) * cs - beta)) / b0;
            b1 = (-2.0f * ((A - 1.0f) + (A + 1.0f) * cs)) / b0;
            b2 = ((A + 1.0f) + (A - 1.0f) * cs - beta) / b0;
            break;
        }
        case FilterType::HIGH_SHELF: {
            const float beta = 2.0f * std::sqrt(A) * alpha;
            b0 = (A + 1.0f) - (A - 1.0f) * cs + beta;
            a0 = (A * ((A + 1.0f) + (A - 1.0f) * cs + beta)) / b0;
            a1 = (-2.0f * A * ((A - 1.0f) + (A + 1.0f) * cs)) / b0;
            a2 = (A * ((A + 1.0f) + (A - 1.0f) * cs - beta)) / b0;
            b1 = (2.0f * ((A - 1.0f) - (A + 1.0f) * cs)) / b0;
            b2 = ((A + 1.0f) - (A - 1.0f) * cs - beta) / b0;
            break;
        }
        case FilterType::LOW_PASS: {
            b0 = 1.0f + alpha;
            a0 = ((1.0f - cs) / 2.0f) / b0;
            a1 = (1.0f - cs) / b0;
            a2 = ((1.0f - cs) / 2.0f) / b0;
            b1 = (-2.0f * cs) / b0;
            b2 = (1.0f - alpha) / b0;
            break;
        }
        case FilterType::HIGH_PASS: {
            b0 = 1.0f + alpha;
            a0 = ((1.0f + cs) / 2.0f) / b0;
            a1 = (-(1.0f + cs)) / b0;
            a2 = ((1.0f + cs) / 2.0f) / b0;
            b1 = (-2.0f * cs) / b0;
            b2 = (1.0f - alpha) / b0;
            break;
        }
        default:
            enabled = false;
            return;
    }

    enabled = true;
}

void BiquadFilter::process(float* buffer, int numFrames, int channels, int channelIndex) {
    if (!enabled) return;

    for (int i = 0; i < numFrames; ++i) {
        const int idx = i * channels + channelIndex;
        const float in = buffer[idx];
        const float out = a0 * in + a1 * x1 + a2 * x2 - b1 * y1 - b2 * y2;

        x2 = x1;
        x1 = in;
        y2 = y1;
        y1 = out;

        buffer[idx] = out;
    }
}
