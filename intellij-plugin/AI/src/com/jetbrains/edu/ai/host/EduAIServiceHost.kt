package com.jetbrains.edu.ai.host

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.ai.EDU_AI_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.EDU_AI_SERVICE_PRODUCTION_URL
import com.jetbrains.edu.ai.EDU_AI_SERVICE_STAGING_URL
import com.jetbrains.edu.ai.messages.EduAIBundle
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

enum class EduAIServiceHost(@Nls private val visibleName: Supplier<String>, val url: String) {
  PRODUCTION(EduAIBundle.lazyMessage("ai.service.production.server"), EDU_AI_SERVICE_PRODUCTION_URL),
  STAGING(EduAIBundle.lazyMessage("ai.service.staging.server"), EDU_AI_SERVICE_STAGING_URL),
  OTHER(EduAIBundle.lazyMessage("ai.service.other"), "http://localhost:8080");

  override fun toString(): String = visibleName.get()

  companion object {
    @JvmStatic
    fun getSelectedHost(): EduAIServiceHost {
      val selectedUrl = getSelectedUrl()
      return EduAIServiceHost.values().firstOrNull { it.url == selectedUrl } ?: OTHER
    }

    @JvmStatic
    fun getSelectedUrl(): String = PropertiesComponent.getInstance().getValue(EDU_AI_SERVICE_HOST_PROPERTY, PRODUCTION.url)
  }
}