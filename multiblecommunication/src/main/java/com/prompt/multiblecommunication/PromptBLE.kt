package com.prompt.multiblecommunication

import android.content.Context

class PromptBLE {

    companion object {
        var context: Context? = null

        fun init(context: Context) {
            this.context = context.applicationContext
        }

    }
}