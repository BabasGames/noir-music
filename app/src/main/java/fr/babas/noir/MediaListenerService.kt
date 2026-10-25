package fr.babas.noir

import android.service.notification.NotificationListenerService

/**
 * Ne fait rien. Android n'autorise MediaSessionManager.getActiveSessions() qu'aux applis
 * dont un NotificationListenerService a reçu l'« accès aux notifications ».
 */
class MediaListenerService : NotificationListenerService()
