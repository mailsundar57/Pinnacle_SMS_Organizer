package com.pinnacle.smsorganizer.diagnostics

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class SmsHandlerDiagnosticsReport(
    val packageName: String,
    val androidRelease: String,
    val sdkInt: Int,
    val targetSdk: Int,
    val installerPackage: String?,
    val isLikelySideloaded: Boolean,
    val isSmsRoleAvailable: Boolean,
    val isSmsRoleHeld: Boolean,
    val systemDefaultSmsPackage: String?,
    val readSmsGranted: Boolean,
    val receiveSmsGranted: Boolean,
    val sendSmsGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val hasSmsDeliverReceiver: Boolean,
    val hasWapPushDeliverReceiver: Boolean,
    val hasRespondViaMessageService: Boolean,
    val hasSendToActivity: Boolean
) {
    val allRoleComponentsValid: Boolean
        get() = hasSmsDeliverReceiver &&
            hasWapPushDeliverReceiver &&
            hasRespondViaMessageService &&
            hasSendToActivity

    fun toDisplayText(): String = buildString {
        appendLine("Environment")
        appendLine("• Android: $androidRelease (API $sdkInt)")
        appendLine("• Target SDK: $targetSdk")
        appendLine("• Package: $packageName")
        appendLine("• Installer: ${installerPackage ?: "Unknown / Direct APK"}")
        if (isLikelySideloaded && sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            appendLine("• Note: Sideloaded APK on Android 15+ may require 'Allow restricted settings' in App info.")
        }
        appendLine()

        appendLine("Default SMS Role")
        appendLine("• ROLE_SMS available: ${yesNo(isSmsRoleAvailable)}")
        appendLine("• ROLE_SMS held by Pinnacle: ${yesNo(isSmsRoleHeld)}")
        appendLine("• System default SMS package: ${systemDefaultSmsPackage ?: "Not visible / None"}")
        appendLine()

        appendLine("Runtime Permissions")
        appendLine("• READ_SMS: ${grantedText(readSmsGranted)}")
        appendLine("• RECEIVE_SMS: ${grantedText(receiveSmsGranted)}")
        appendLine("• SEND_SMS: ${grantedText(sendSmsGranted)}")
        appendLine("• POST_NOTIFICATIONS: ${grantedText(postNotificationsGranted)}")
        appendLine()

        appendLine("Required Role Manifest Components")
        appendLine("• SMS_DELIVER receiver (BROADCAST_SMS): ${okText(hasSmsDeliverReceiver)}")
        appendLine("• WAP_PUSH_DELIVER receiver (BROADCAST_WAP_PUSH): ${okText(hasWapPushDeliverReceiver)}")
        appendLine("• RESPOND_VIA_MESSAGE service (SEND_RESPOND_VIA_MESSAGE): ${okText(hasRespondViaMessageService)}")
        appendLine("• SENDTO smsto: activity: ${okText(hasSendToActivity)}")
        appendLine("• Overall manifest eligibility: ${if (allRoleComponentsValid) "PASS" else "FAIL"}")
    }.trimEnd()

    private fun yesNo(value: Boolean): String = if (value) "Yes" else "No"
    private fun grantedText(value: Boolean): String = if (value) "Granted" else "Denied"
    private fun okText(value: Boolean): String = if (value) "OK" else "Missing"
}

object SmsHandlerDiagnostics {

    private val TRUSTED_STORE_INSTALLERS = setOf(
        "com.android.vending", // Google Play Store
        "com.sec.android.app.samsungapps", // Samsung Galaxy Store
        "com.amazon.venezia" // Amazon Appstore
    )

    fun inspect(context: Context): SmsHandlerDiagnosticsReport {
        val pm = context.packageManager
        val pkg = context.packageName
        val roleManager = context.getSystemService(RoleManager::class.java)

        val isRoleAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true
        val isRoleHeld = isRoleAvailable && roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true

        val defaultSmsPackage = runCatching {
            Telephony.Sms.getDefaultSmsPackage(context)
        }.getOrNull()

        val installer = runCatching {
            val info = pm.getInstallSourceInfo(pkg)
            info.installingPackageName ?: info.initiatingPackageName
        }.getOrNull()

        val isLikelySideloaded = installer == null || installer !in TRUSTED_STORE_INSTALLERS

        val hasSmsDeliver = pm.queryBroadcastReceivers(
            Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION).setPackage(pkg),
            PackageManager.ResolveInfoFlags.of(0L)
        ).any { it.activityInfo?.permission == Manifest.permission.BROADCAST_SMS }

        val hasWapPushDeliver = pm.queryBroadcastReceivers(
            Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION)
                .setType("application/vnd.wap.mms-message")
                .setPackage(pkg),
            PackageManager.ResolveInfoFlags.of(0L)
        ).any { it.activityInfo?.permission == Manifest.permission.BROADCAST_WAP_PUSH }

        val hasRespondViaMessage = pm.queryIntentServices(
            Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.fromParts("smsto", "", null))
                .setPackage(pkg),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        ).any { it.serviceInfo?.permission == Manifest.permission.SEND_RESPOND_VIA_MESSAGE }

        val hasSendTo = pm.queryIntentActivities(
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", "", null))
                .setPackage(pkg),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        ).isNotEmpty()

        return SmsHandlerDiagnosticsReport(
            packageName = pkg,
            androidRelease = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            targetSdk = context.applicationInfo.targetSdkVersion,
            installerPackage = installer,
            isLikelySideloaded = isLikelySideloaded,
            isSmsRoleAvailable = isRoleAvailable,
            isSmsRoleHeld = isRoleHeld,
            systemDefaultSmsPackage = defaultSmsPackage,
            readSmsGranted = isGranted(context, Manifest.permission.READ_SMS),
            receiveSmsGranted = isGranted(context, Manifest.permission.RECEIVE_SMS),
            sendSmsGranted = isGranted(context, Manifest.permission.SEND_SMS),
            postNotificationsGranted = isGranted(context, Manifest.permission.POST_NOTIFICATIONS),
            hasSmsDeliverReceiver = hasSmsDeliver,
            hasWapPushDeliverReceiver = hasWapPushDeliver,
            hasRespondViaMessageService = hasRespondViaMessage,
            hasSendToActivity = hasSendTo
        )
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
