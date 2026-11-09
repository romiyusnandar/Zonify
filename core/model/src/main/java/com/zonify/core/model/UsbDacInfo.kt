package com.zonify.core.model

/**
 * Hardware information for connected USB Digital-to-Analog Converters (DAC).
 */
data class UsbDacInfo(
    val deviceName: String,
    val manufacturer: String? = null,
    val productName: String? = null,
    val vendorId: Int = 0,
    val productId: Int = 0,
    val isUsbAudioClass: Boolean = true,
    val supportedSampleRates: List<Int> = listOf(44100, 48000, 96000, 192000, 384000, 768000),
    val supportsBitPerfect: Boolean = true
) {
    val displayName: String
        get() = productName ?: deviceName
}
