package io.github.zzunsik.nextset.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsAppNameAndPlaceholder() {
        val app = RuntimeEnvironment.getApplication()

        compose.setContent { NextSetTheme { HomeScreen() } }

        compose.onNodeWithText(app.getString(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.home_placeholder)).assertIsDisplayed()
    }
}
