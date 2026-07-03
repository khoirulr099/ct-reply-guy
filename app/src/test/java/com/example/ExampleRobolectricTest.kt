package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ReplyRepository
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CT Reply Guy", appName)
    }

    @Test
    fun `app content compiles and renders without crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup in-memory version or real database for testing
        val database = AppDatabase.getDatabase(context)
        val repository = ReplyRepository(database.replyDao())
        val viewModel = MainViewModel(repository, context)

        // Set the UI content using the real app composables
        composeTestRule.setContent {
            MyApplicationTheme {
                ReplyGuyApp(viewModel = viewModel)
            }
        }

        // Wait for composition and idle state
        composeTestRule.waitForIdle()

        // Verify some static elements are rendered
        composeTestRule.onNodeWithText("degenreply").assertExists()
        composeTestRule.onNodeWithText("active on chain neural core").assertExists()
    }
}

