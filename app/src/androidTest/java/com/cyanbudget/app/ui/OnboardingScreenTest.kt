package com.cyanbudget.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cyanbudget.app.model.AppSettings
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onboardingAdvancesToVoiceFeature() {
        compose.setContent { MaterialTheme { OnboardingScreen(AppSettings()) {} } }
        compose.onNodeWithText("Know where money goes").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Speak naturally").assertIsDisplayed()
    }
}
