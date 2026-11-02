package com.biglv.recitelsland

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Ignore("需要固定测试账号与测试题库数据；当前接入真实后端，UI 用例暂缓执行")
@RunWith(AndroidJUnit4::class)
class ReciteAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForBank() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("全部 8").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bottomNavigation_switchesAllFourTabs() {
        composeRule.onNodeWithTag("tab_review").performClick()
        waitForText("按 SM-2 计划安排今日复习")

        composeRule.onNodeWithTag("tab_records").performClick()
        waitForText("每次作答都会留下一条历史记录")

        composeRule.onNodeWithTag("tab_profile").performClick()
        waitForText("偏好与设置")

        composeRule.onNodeWithTag("tab_bank").performClick()
        waitForText("共 8 道题")
    }

    @Test
    fun tagFilter_narrowsQuestionList() {
        composeRule.onNodeWithTag("action_tags").performClick()
        composeRule.onNodeWithTag("tag_filter_Go").performClick()
        waitForText("标签 #Go")

        composeRule.onNodeWithTag("question_8").assertExists()
        composeRule.onNodeWithTag("question_1").assertDoesNotExist()

        composeRule.onNodeWithTag("tag_filter_all").performClick()
        waitForText("共 8 道题")
    }

    @Test
    fun search_filtersQuestionsByKeyword() {
        composeRule.onNodeWithTag("action_search").performClick()
        composeRule.onNodeWithTag("search_field").performTextInput("TCP")
        waitForText("搜索“TCP”")

        composeRule.onNodeWithTag("question_1").assertExists()
        composeRule.onNodeWithTag("question_2").assertDoesNotExist()
    }

    @Test
    fun multipleChoice_doesNotRevealAnswerUntilSubmitted() {
        openQuestion(1)

        composeRule.onNodeWithTag("explanation_1").assertDoesNotExist()
        composeRule.onNodeWithTag("result_1").assertDoesNotExist()

        composeRule.onNodeWithTag("option_1_A").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_1_B").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_1_D").performScrollTo().performClick()
        composeRule.onNodeWithTag("submit_1").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("回答正确").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("result_1").assertExists()
        composeRule.onNodeWithTag("explanation_1").assertExists()
    }

    @Test
    fun questionDetail_recordsTabShowsOnlyThisQuestion() {
        openQuestion(1)
        composeRule.onNodeWithTag("detail_tab_records").performClick()

        waitForText("这道题的作答记录")
        composeRule.onNodeWithTag("question_record_101").assertExists()
    }

    @Test
    fun trueFalse_resetAnswerAllowsSecondAttempt() {
        composeRule.onNodeWithTag("filter_TrueFalse").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("判断 2").fetchSemanticsNodes().isNotEmpty()
        }

        openQuestion(3)
        composeRule.onNodeWithTag("option_3_B").performScrollTo().performClick()
        composeRule.onNodeWithTag("submit_3").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("回答正确").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("result_3").assertExists()

        composeRule.onNodeWithTag("retry_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("result_3").assertDoesNotExist()
        composeRule.onNodeWithTag("submit_3").performScrollTo().assertExists()
    }

    @Test
    fun reading_revealsExplanationOnlyAfterCompletingStudy() {
        composeRule.onNodeWithTag("filter_Reading").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("阅读 2").fetchSemanticsNodes().isNotEmpty()
        }

        openQuestion(5)
        composeRule.onNodeWithTag("complete_reading_5").assertExists()
        composeRule.onNodeWithTag("explanation_5").assertDoesNotExist()

        composeRule.onNodeWithTag("complete_reading_5").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("已完成学习").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("explanation_5").assertExists()
    }

    @Test
    fun records_filterAndOpenRecordDetail() {
        composeRule.onNodeWithTag("tab_records").performClick()
        waitForText("正确率")

        composeRule.onNodeWithTag("record_filter_Incorrect").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Kotlin 协程中 withContext 的主要作用")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("record_102").performClick()
        waitForText("作答结果")
        composeRule.onNodeWithText("正确答案：B").assertExists()
    }

    @Test
    fun review_opensReadOnlyReviewSheet() {
        composeRule.onNodeWithTag("tab_review").performClick()
        waitForText("今日待复习")

        composeRule.onNodeWithTag("review_list").performScrollToNode(hasTestTag("start_review_201"))
        composeRule.onNodeWithTag("start_review_201").performClick()

        waitForText("提交复习")
        composeRule.onNodeWithText("复习提交接口将在下一阶段接入。当前页面用于确认复习流程和布局。")
            .assertExists()
    }

    private fun openQuestion(id: Long) {
        composeRule.onNodeWithTag("bank_list").performScrollToNode(hasTestTag("question_$id"))
        composeRule.onNodeWithTag("question_$id").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("detail_tab_question").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
