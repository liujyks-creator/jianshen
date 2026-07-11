package com.liujyks.trainflow.core.health

/** Android-free generation gate used by the provider before queued callback and sink execution. */
internal class HeartRateProviderCallbackGate {
    data class Token(val lifecycleGeneration: Long, val scanGeneration: Long? = null)

    private var closed = false
    private var lifecycleGeneration = 0L
    private var scanGeneration = 0L

    fun lifecycleToken(): Token = Token(lifecycleGeneration)

    fun beginScan(): Token {
        scanGeneration += 1
        return Token(lifecycleGeneration, scanGeneration)
    }

    fun invalidateScan() {
        scanGeneration += 1
    }

    fun invalidateLifecycle() {
        lifecycleGeneration += 1
        scanGeneration += 1
    }

    fun close() {
        if (closed) return
        closed = true
        invalidateLifecycle()
    }

    fun isOpen(): Boolean = !closed

    fun accepts(token: Token): Boolean =
        !closed && token.lifecycleGeneration == lifecycleGeneration &&
            (token.scanGeneration == null || token.scanGeneration == scanGeneration)
}
