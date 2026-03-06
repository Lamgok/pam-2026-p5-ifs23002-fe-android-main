package org.delcom.pam_p5_ifs23002.network.todos.service

import okhttp3.MultipartBody
import org.delcom.pam_p5_ifs23002.helper.SuspendHelper
import org.delcom.pam_p5_ifs23002.network.data.ResponseMessage
import org.delcom.pam_p5_ifs23002.network.todos.data.*

class TodoRepository(
    private val apiService: TodoApiService
) : ITodoRepository {

    // Helper untuk memastikan format token selalu diawali Bearer
    private fun formatToken(token: String): String {
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    override suspend fun postRegister(request: RequestAuthRegister) = SuspendHelper.safeApiCall {
        apiService.postRegister(request)
    }

    override suspend fun postLogin(request: RequestAuthLogin) = SuspendHelper.safeApiCall {
        apiService.postLogin(request)
    }

    override suspend fun postLogout(request: RequestAuthLogout) = SuspendHelper.safeApiCall {
        apiService.postLogout(request)
    }

    override suspend fun postRefreshToken(request: RequestAuthRefreshToken) = SuspendHelper.safeApiCall {
        apiService.postRefreshToken(request)
    }

    override suspend fun getUserMe(authToken: String) = SuspendHelper.safeApiCall {
        apiService.getUserMe(formatToken(authToken))
    }

    override suspend fun putUserMe(authToken: String, request: RequestUserChange) = SuspendHelper.safeApiCall {
        apiService.putUserMe(formatToken(authToken), request)
    }

    override suspend fun putUserMePassword(authToken: String, request: RequestUserChangePassword) = SuspendHelper.safeApiCall {
        apiService.putUserMePassword(formatToken(authToken), request)
    }

    override suspend fun putUserMePhoto(authToken: String, file: MultipartBody.Part) = SuspendHelper.safeApiCall {
        apiService.putUserMePhoto(formatToken(authToken), file)
    }

    override suspend fun getTodos(authToken: String, search: String?, isDone: Boolean?, urgency: String?, page: Int?, perPage: Int?) = SuspendHelper.safeApiCall {
        apiService.getTodos(formatToken(authToken), search, isDone, urgency, page, perPage)
    }

    override suspend fun postTodo(authToken: String, request: RequestTodo) = SuspendHelper.safeApiCall {
        apiService.postTodo(formatToken(authToken), request)
    }

    override suspend fun getTodoById(authToken: String, todoId: String) = SuspendHelper.safeApiCall {
        apiService.getTodoById(formatToken(authToken), todoId)
    }

    override suspend fun putTodo(authToken: String, todoId: String, request: RequestTodo) = SuspendHelper.safeApiCall {
        apiService.putTodo(formatToken(authToken), todoId, request)
    }

    override suspend fun putTodoCover(authToken: String, todoId: String, file: MultipartBody.Part) = SuspendHelper.safeApiCall {
        apiService.putTodoCover(formatToken(authToken), todoId, file)
    }

    override suspend fun deleteTodo(authToken: String, todoId: String) = SuspendHelper.safeApiCall {
        apiService.deleteTodo(formatToken(authToken), todoId)
    }
}