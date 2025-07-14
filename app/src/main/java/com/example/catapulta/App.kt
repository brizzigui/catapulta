package com.example.catapulta

import android.content.Context
import android.graphics.drawable.Drawable

data class App(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

fun App.launch(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(intent)
}
