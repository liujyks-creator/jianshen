package com.liujyks.trainflow.core.notifications

/**
 * Package boundary for active-workout notifications and legacy reminder cleanup.
 *
 * The application owns the ordinary active-workout notification controller.
 * Foreground service handoff and background heart-rate recording belong to E20-S02.
 */
internal object NotificationBoundary
