package com.theveloper.pixelplay.data.ai

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiLogger @Inject constructor() {

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).d(message)
        }
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).w(throwable, message)
        } else {
            Timber.tag(tag).w(message)
        }
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }

    fun debug(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }
}
