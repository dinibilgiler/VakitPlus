package com.vakitplus.app

/** Device registration contract for the central owner backend.
 * The app can call register() after obtaining an FCM token and a configured backend.
 */
object DeviceRegistration {
    data class Payload(val token:String,val appVersion:String,val city:String,val notificationsEnabled:Boolean=true)
}
