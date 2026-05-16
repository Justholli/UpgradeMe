package com.aesthetic.tracker.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

const val DefaultChatUrl = "https://chatgpt.com/share/6a087269-e5c4-8329-9d18-60e101320377"

@Singleton
class AiSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    private val _chatUrl = MutableStateFlow(preferences.getString(KeyChatUrl, DefaultChatUrl).orEmpty())
    val chatUrl: StateFlow<String> = _chatUrl.asStateFlow()

    fun saveChatUrl(value: String) {
        val normalized = value.trim().ifBlank { DefaultChatUrl }
        preferences.edit { putString(KeyChatUrl, normalized) }
        _chatUrl.value = normalized
    }

    private companion object {
        const val KeyChatUrl = "chat_url"
    }
}
