package fr.babas.noir

import android.service.notification.NotificationListenerService

/**
 * Does nothing. Android only allows MediaSessionManager.getActiveSessions() for apps
 * whose NotificationListenerService has been granted "notification access".
 */
class MediaListenerService : NotificationListenerService()
