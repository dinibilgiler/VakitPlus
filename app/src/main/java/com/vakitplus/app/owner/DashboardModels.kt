package com.vakitplus.app.owner

data class DashboardSummary(val users:Int,val devices:Int,val activeDevices:Int,val notificationsEnabled:Int,val notificationsSent:Int,val scheduledNotifications:Int,val drafts:Int,val auditEvents:Int,val maintenance:Boolean)
data class AppSystemSettings(val maintenance:Boolean,val announcement:String,val updatedAt:Long)
