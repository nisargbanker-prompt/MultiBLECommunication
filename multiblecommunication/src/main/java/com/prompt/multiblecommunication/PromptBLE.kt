package com.prompt.multiblecommunication

import android.content.Context

class PromptBLE {
    companion object {
        var context: Context? = null
        var storageManager: FileStorageManager? = null

        fun init(context: Context) {
            this.context = context.applicationContext
            storageManager = FileStorageManager(context)
        }
    }
}