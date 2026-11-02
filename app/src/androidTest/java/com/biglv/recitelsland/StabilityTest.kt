package com.biglv.recitelsland

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 稳定性冒烟：反复切换四个 Tab、反复滚动题库列表。
 *
 * 目的不是测业务分支，而是确认 ViewModel / NavBackStack / LazyColumn
 * 在持续交互后不崩溃、不出现明显的堆内存持续增长。
 */
@Ignore("需要固定测试账号与测试题库数据；当前接入真实后端，UI 用例暂缓执行")
@RunWith(AndroidJUnit4::class)
class StabilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun repeatedTabSwitchingAndListScrolling() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("全部 8").fetchSemanticsNodes().isNotEmpty()
        }

        val runtime = Runtime.getRuntime()
        val heapBefore = runtime.totalMemory() - runtime.freeMemory()

        repeat(8) { round ->
            composeRule.onNodeWithTag("tab_review").performClick()
            waitForText("今日待复习")

            composeRule.onNodeWithTag("tab_records").performClick()
            waitForText("正确率")

            composeRule.onNodeWithTag("tab_profile").performClick()
            waitForText("偏好与设置")

            composeRule.onNodeWithTag("tab_bank").performClick()
            waitForText("共 8 道题")

            composeRule.onNodeWithTag("bank_list").performScrollToIndex(7)
            composeRule.onNodeWithTag("bank_list").performScrollToIndex(0)
            Log.i(TAG, "round=$round heapUsed=${(runtime.totalMemory() - runtime.freeMemory()) / 1024}KB")
        }

        val heapAfter = runtime.totalMemory() - runtime.freeMemory()
        Log.i(TAG, "heapBefore=${heapBefore / 1024}KB heapAfter=${heapAfter / 1024}KB")
        composeRule.onNodeWithTag("tab_bank").assertExists()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TAG = "StabilityTest"
    }
}
