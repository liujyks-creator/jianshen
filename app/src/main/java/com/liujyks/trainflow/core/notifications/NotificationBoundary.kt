package com.liujyks.trainflow.core.notifications

/**
 * E0.2 package boundary for plan reminders and active-workout notifications.
 *
 * Notification scheduling and foreground notifications are deferred to later
 * stories and must consume workout events instead of owning the engine.
 */
internal object NotificationBoundary
