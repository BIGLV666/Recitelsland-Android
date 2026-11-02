package com.biglv.recitelsland.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.biglv.recitelsland.feature.admin.AdminFailedRequestsScreen
import com.biglv.recitelsland.feature.admin.AdminFeedbacksScreen
import com.biglv.recitelsland.feature.admin.AdminQuestionsScreen
import com.biglv.recitelsland.feature.admin.AdminScreen
import com.biglv.recitelsland.feature.admin.AdminUsersScreen
import com.biglv.recitelsland.feature.admin.ImportQuestionsScreen
import com.biglv.recitelsland.feature.admin.QuestionEditorScreen
import com.biglv.recitelsland.feature.bank.BankScreen
import com.biglv.recitelsland.feature.favorites.FavoritesScreen
import com.biglv.recitelsland.feature.feedback.FeedbackScreen
import com.biglv.recitelsland.feature.legal.AboutScreen
import com.biglv.recitelsland.feature.legal.LegalDocuments
import com.biglv.recitelsland.feature.legal.LegalScreen
import com.biglv.recitelsland.feature.profile.EditProfileScreen
import com.biglv.recitelsland.feature.profile.ProfileScreen
import com.biglv.recitelsland.feature.reviewplans.ReviewPlansScreen
import com.biglv.recitelsland.feature.question.QuestionDetailScreen
import com.biglv.recitelsland.feature.records.RecordsScreen
import com.biglv.recitelsland.feature.review.ReviewScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    TopLevelDestination(route = "bank", label = "题库", icon = Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelDestination(route = "review", label = "复习", icon = Icons.Outlined.EventRepeat),
    TopLevelDestination(route = "records", label = "记录", icon = Icons.Outlined.History),
    TopLevelDestination(route = "profile", label = "我的", icon = Icons.Outlined.Person),
)

@Composable
fun ReciteApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination == null || destinations.any { destination ->
        currentDestination.hierarchy.any { it.route == destination.route }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            modifier = Modifier.testTag("tab_${destination.route}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "bank",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable("bank") {
                BankScreen(
                    onQuestionClick = { questionId ->
                        navController.navigate("question/$questionId")
                    },
                )
            }
            composable("review") { ReviewScreen() }
            composable("records") { RecordsScreen() }
            composable("profile") {
                ProfileScreen(
                    onOpenAbout = { navController.navigate("about") },
                    onOpenFavorites = { navController.navigate("favorites") },
                    onOpenReviewPlans = { navController.navigate("review-plans") },
                    onOpenEditProfile = { navController.navigate("edit-profile") },
                    onOpenFeedback = { navController.navigate("feedback") },
                    onOpenAdmin = { navController.navigate("admin") },
                )
            }
            composable("edit-profile") {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }
            composable("feedback") {
                FeedbackScreen(onBack = { navController.popBackStack() })
            }
            composable("admin") {
                AdminScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUsers = { navController.navigate("admin/users") },
                    onOpenFeedbacks = { navController.navigate("admin/feedbacks") },
                    onOpenFailedRequests = { navController.navigate("admin/failed") },
                    onOpenQuestions = { navController.navigate("admin/questions") },
                )
            }
            composable("admin/users") {
                AdminUsersScreen(onBack = { navController.popBackStack() })
            }
            composable("admin/feedbacks") {
                AdminFeedbacksScreen(onBack = { navController.popBackStack() })
            }
            composable("admin/failed") {
                AdminFailedRequestsScreen(onBack = { navController.popBackStack() })
            }
            composable("admin/questions") {
                AdminQuestionsScreen(
                    onBack = { navController.popBackStack() },
                    onNew = { navController.navigate("admin/questions/new") },
                    onEdit = { id -> navController.navigate("admin/questions/edit/$id") },
                    onImport = { navController.navigate("admin/questions/import") },
                )
            }
            composable("admin/questions/new") {
                QuestionEditorScreen(
                    questionId = null,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "admin/questions/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                QuestionEditorScreen(
                    questionId = entry.arguments?.getLong("id"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable("admin/questions/import") {
                ImportQuestionsScreen(onBack = { navController.popBackStack() })
            }
            composable("favorites") {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuestion = { questionId ->
                        navController.navigate("question/$questionId")
                    },
                )
            }
            composable("review-plans") {
                ReviewPlansScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuestion = { questionId ->
                        navController.navigate("question/$questionId")
                    },
                )
            }
            composable("about") {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLegal = { key -> navController.navigate("legal/$key") },
                )
            }
            composable("legal/{docKey}") { entry ->
                val docKey = entry.arguments?.getString("docKey").orEmpty()
                LegalScreen(
                    doc = LegalDocuments.byKey[docKey] ?: LegalDocuments.privacy,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "question/{questionId}",
                arguments = listOf(navArgument("questionId") { type = NavType.LongType }),
            ) { entry ->
                val questionId = entry.arguments?.getLong("questionId") ?: 0L
                QuestionDetailScreen(
                    questionId = questionId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
