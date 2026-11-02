package com.biglv.recitelsland.core.auth

import com.biglv.recitelsland.data.model.UserProfile
import com.biglv.recitelsland.data.remote.ApiException
import com.biglv.recitelsland.data.remote.BindEmailCodeRequest
import com.biglv.recitelsland.data.remote.BindEmailRequest
import com.biglv.recitelsland.data.remote.DeleteAccountRequest
import com.biglv.recitelsland.data.remote.ApiExecutor
import com.biglv.recitelsland.data.remote.ExchangeCodeRequest
import com.biglv.recitelsland.data.remote.LoginRequest
import com.biglv.recitelsland.data.remote.ReciteApi
import com.biglv.recitelsland.data.remote.RegisterRequest
import com.biglv.recitelsland.data.remote.SendCodeRequest
import com.biglv.recitelsland.data.remote.UpdateNicknameRequest
import com.biglv.recitelsland.data.remote.toDomain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class AuthRepository(
    private val api: ReciteApi,
    private val controller: SessionController,
    private val manager: SessionManager,
    private val executor: ApiExecutor,
) {

    suspend fun restore() {
        if (manager.state.value is SessionState.LoggedIn) return
        val stored = controller.readStored()
        if (manager.state.value is SessionState.LoggedIn) return
        if (stored == null) {
            manager.markLoggedOut()
            return
        }

        // 先恢复本地缓存，断网时仍能进入应用；随后静默刷新用户信息。
        manager.markLoggedIn(stored.token, stored.user)
        try {
            val response = api.currentUser()
            if (response.code == 0 && response.data != null) {
                controller.updateUser(response.data.toDomain())
            } else if (response.code / 1000 == 401) {
                controller.clear()
            }
        } catch (error: ApiException) {
            if (error.isUnauthorized) {
                controller.clear()
            }
        } catch (error: HttpException) {
            if (error.code() == 401) controller.clear()
        } catch (_: Exception) {
            // 网络异常时保留本地缓存，页面会在请求时给出错误提示。
        }
    }

    suspend fun login(email: String, password: String): UserProfile {
        val data = executor.unwrapRequired {
            api.login(LoginRequest(email = email.trim(), password = password))
        }
        val user = data.user.toDomain()
        controller.save(data.token, user)
        return user
    }

    suspend fun register(
        username: String,
        nickname: String,
        email: String,
        password: String,
        code: String,
    ): UserProfile {
        val data = executor.unwrapRequired {
            api.register(
                RegisterRequest(
                    username = username.trim(),
                    nickname = nickname.trim(),
                    email = email.trim(),
                    password = password,
                    code = code.trim(),
                ),
            )
        }
        val user = data.user.toDomain()
        controller.save(data.token, user)
        return user
    }

    suspend fun sendCode(email: String) {
        executor.unit { api.sendCode(SendCodeRequest(email.trim())) }
    }

    suspend fun exchangeGithubCode(code: String): UserProfile {
        val data = executor.unwrapRequired {
            api.exchangeGithubCode(ExchangeCodeRequest(code))
        }
        val user = data.user.toDomain()
        controller.save(data.token, user)
        return user
    }

    suspend fun updateNickname(nickname: String): UserProfile {
        val user = executor.unwrapRequired {
            api.updateNickname(UpdateNicknameRequest(nickname.trim()))
        }.toDomain()
        controller.updateUser(user)
        return user
    }

    suspend fun sendBindEmailCode(email: String) {
        executor.unit { api.sendBindEmailCode(BindEmailCodeRequest(email.trim())) }
    }

    suspend fun bindEmail(email: String, code: String): UserProfile {
        val user = executor.unwrapRequired {
            api.bindEmail(BindEmailRequest(email = email.trim(), code = code.trim()))
        }.toDomain()
        controller.updateUser(user)
        return user
    }

    suspend fun uploadAvatar(bytes: ByteArray, contentType: String): UserProfile {
        val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "avatar", body)
        val user = executor.unwrapRequired { api.uploadAvatar(part) }.toDomain()
        controller.updateUser(user)
        return user
    }

    suspend fun deleteAvatar(): UserProfile {
        val user = executor.unwrapRequired { api.deleteAvatar() }.toDomain()
        controller.updateUser(user)
        return user
    }

    suspend fun deleteAccount(password: String) {
        executor.unit { api.deleteAccount(DeleteAccountRequest(password)) }
        controller.clear()
    }

    suspend fun refreshUser(): UserProfile {
        val user = executor.unwrapRequired { api.currentUser() }.toDomain()
        controller.updateUser(user)
        return user
    }

    suspend fun logout() {
        runCatching { executor.unit { api.logout() } }
        controller.clear()
    }
}
