package com.korentingeorget.troccampus.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class PermissionState {
    NotAsked,
    Granted,
    DeniedOnce,
    DeniedForever,
}

class PermissionFlow(
    private val activity: Activity,
    val permissions: List<String>,
    private val requireAll: Boolean,
    private val getAskedBefore: () -> Boolean,
    private val setAskedBefore: () -> Unit,
) {
    var state by mutableStateOf(compute(afterResult = false))
        private set

    private var launcher: ActivityResultLauncher<Array<String>>? = null

    fun attach(launcher: ActivityResultLauncher<Array<String>>) {
        this.launcher = launcher
    }

    fun request() {
        setAskedBefore()
        launcher?.launch(permissions.toTypedArray())
    }

    fun openSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
        activity.startActivity(intent)
    }

    fun refresh() {
        state = compute(afterResult = false)
    }

    fun onResult(result: Map<String, Boolean>) {
        val granted = if (requireAll) {
            permissions.all { result[it] == true }
        } else {
            permissions.any { result[it] == true }
        }

        state = if (granted) {
            PermissionState.Granted
        } else {
            compute(afterResult = true)
        }
    }

    private fun compute(afterResult: Boolean): PermissionState {
        val allGranted = if (requireAll) {
            permissions.all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            permissions.any {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        if (allGranted) return PermissionState.Granted

        val anyRationale = permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        if (anyRationale) return PermissionState.DeniedOnce

        return if (getAskedBefore() || afterResult) {
            PermissionState.DeniedForever
        } else {
            PermissionState.NotAsked
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun rememberPermissionFlow(
    vararg permissions: String,
    requireAll: Boolean = true,
): PermissionFlow {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() ?: error("Activity not found") }
    var askedBefore by rememberSaveable { mutableStateOf(false) }

    val flow = remember(activity, permissions.toList(), requireAll) {
        PermissionFlow(
            activity = activity,
            permissions = permissions.toList(),
            requireAll = requireAll,
            getAskedBefore = { askedBefore },
            setAskedBefore = { askedBefore = true },
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        flow.onResult(result)
    }

    DisposableEffect(flow, launcher) {
        flow.attach(launcher)
        onDispose {}
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, flow) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                flow.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return flow
}
