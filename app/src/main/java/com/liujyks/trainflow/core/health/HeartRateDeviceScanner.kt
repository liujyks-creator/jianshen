package com.liujyks.trainflow.core.health

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

internal interface HeartRateDeviceScanner : AutoCloseable {
    val providerState: StateFlow<BleHeartRateProviderState>
    val scanState: StateFlow<BleHeartRateScanState>
    val candidates: StateFlow<List<BleHeartRateDeviceCandidate>>

    fun refreshAvailability()
    fun setDisplayEnabled(enabled: Boolean)
    fun setForegroundActive(active: Boolean)
    fun startScan()
    fun stopScan()
    fun selectDevice(identifier: String): BleHeartRateDeviceSelection?
    fun connectSelectedDevice()
    fun stopAndDisconnect()
}

internal class AndroidHeartRateDeviceScanner(
    context: Context
) : HeartRateDeviceScanner {
    private val provider = AndroidBleHeartRateProvider(context.applicationContext)

    override val providerState: StateFlow<BleHeartRateProviderState> = provider.providerState
    override val scanState: StateFlow<BleHeartRateScanState> = provider.scanState
    override val candidates: StateFlow<List<BleHeartRateDeviceCandidate>> = provider.candidates

    override fun refreshAvailability() {
        provider.refreshAvailability()
    }

    override fun setDisplayEnabled(enabled: Boolean) {
        provider.setDisplayEnabled(enabled)
    }

    override fun setForegroundActive(active: Boolean) {
        provider.setForegroundActive(active)
    }

    override fun startScan() {
        provider.startScan()
    }

    override fun stopScan() {
        provider.stopScan()
    }

    override fun selectDevice(identifier: String): BleHeartRateDeviceSelection? {
        return provider.selectDevice(identifier)
    }

    override fun connectSelectedDevice() {
        provider.connectSelectedDevice()
    }

    override fun stopAndDisconnect() {
        provider.stop()
    }

    override fun close() {
        provider.close()
    }
}
