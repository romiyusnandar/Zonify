package com.zonify.core.audio

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import androidx.annotation.RequiresApi
import com.zonify.core.model.UsbDacInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages external USB DAC discovery, hardware capability extraction,
 * and bit-perfect audio stream locking on Android 14+.
 */
class UsbDacManager private constructor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _connectedDac = MutableStateFlow<UsbDacInfo?>(null)
    val connectedDac: StateFlow<UsbDacInfo?> = _connectedDac.asStateFlow()

    private val _isBitPerfectLocked = MutableStateFlow(false)
    val isBitPerfectLocked: StateFlow<Boolean> = _isBitPerfectLocked.asStateFlow()

    private var currentUsbAudioDevice: AudioDeviceInfo? = null

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            detectConnectedDac()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            detectConnectedDac()
        }
    }

    companion object {
        @Volatile
        private var instance: UsbDacManager? = null

        fun getInstance(context: Context): UsbDacManager {
            return instance ?: synchronized(this) {
                instance ?: UsbDacManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startListening() {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        detectConnectedDac()
    }

    fun stopListening() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    /**
     * Inspects active audio outputs and USB subsystem to find connected audio DAC.
     */
    fun detectConnectedDac() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val usbDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }

        currentUsbAudioDevice = usbDevice

        if (usbDevice != null) {
            val sampleRates = usbDevice.sampleRates.takeIf { it.isNotEmpty() }?.toList()
                ?: listOf(44100, 48000, 96000, 192000, 384000)

            // Cross-reference with UsbManager for detailed vendor & product IDs
            val usbHardware = findMatchingUsbDevice(usbDevice.productName.toString())

            val dacInfo = UsbDacInfo(
                deviceName = usbDevice.productName.toString().takeIf { it.isNotBlank() } ?: "External USB DAC",
                manufacturer = usbHardware?.manufacturerName,
                productName = usbHardware?.productName ?: usbDevice.productName.toString(),
                vendorId = usbHardware?.vendorId ?: 0,
                productId = usbHardware?.productId ?: 0,
                isUsbAudioClass = true,
                supportedSampleRates = sampleRates,
                supportsBitPerfect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            )
            _connectedDac.value = dacInfo
        } else {
            _connectedDac.value = null
            _isBitPerfectLocked.value = false
            AudioEngineBridge.setBitPerfect(false)
        }
    }

    private fun findMatchingUsbDevice(productName: String): UsbDevice? {
        val deviceList = usbManager?.deviceList ?: return null
        for (device in deviceList.values) {
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                    return device
                }
            }
        }
        return deviceList.values.firstOrNull {
            it.productName?.equals(productName, ignoreCase = true) == true
        }
    }

    /**
     * Toggles Bit-Perfect playback mode.
     * On Android 14+ (API 34), invokes setPreferredMixerAttributes to lock mixer to BIT_PERFECT.
     */
    fun setBitPerfectMode(enabled: Boolean, sampleRate: Int = 44100): Boolean {
        AudioEngineBridge.setBitPerfect(enabled)
        _isBitPerfectLocked.value = enabled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return applyAndroid14BitPerfect(enabled, sampleRate)
        }
        return enabled
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun applyAndroid14BitPerfect(enabled: Boolean, sampleRate: Int): Boolean {
        val targetDevice = currentUsbAudioDevice ?: return false

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        return try {
            if (enabled) {
                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()

                val mixerAttributes = AudioMixerAttributes.Builder(format)
                    .setMixerBehavior(AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
                    .build()

                audioManager.setPreferredMixerAttributes(audioAttributes, targetDevice, mixerAttributes)
            } else {
                audioManager.clearPreferredMixerAttributes(audioAttributes, targetDevice)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
