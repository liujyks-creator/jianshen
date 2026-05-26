package com.liujyks.trainflow.core.media

/**
 * E0.2 package boundary for workout event media consumers.
 *
 * Prompt sounds, future voice output, and follow-along media playback belong
 * behind this boundary and must not drive workout state directly.
 */
internal object MediaBoundary
