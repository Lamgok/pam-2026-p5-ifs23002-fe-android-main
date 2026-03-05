package org.delcom.pam_p5_ifs23002.helper

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.delcom.pam_p5_ifs23002.network.data.ResponseMessage
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object SuspendHelper {
    enum class SnackBarType(val title: String) {
        ERROR(title = "error"),
        SUCCESS(title = "success"),
        INFO(title = "info"),
        WARNING(title = "warning")
    }

    suspend fun showSnackBar(snackbarHost: SnackbarHostState, type: SnackBarType,  message: String){
        coroutineScope {
            launch {
                snackbarHost.showSnackbar(
                    message = "${type.title}|$message",
                    actionLabel = "Close",
                    duration = SnackbarDuration.Indefinite
                )
            }

            launch {
                delay(5_000)
                snackbarHost.currentSnackbarData?.dismiss()
            }
        }
    }

    suspend fun <T> safeApiCall(apiCall: suspend () -> ResponseMessage<T?>): ResponseMessage<T?> {
        return try {
            apiCall()
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            try {
                // Parse error response dari backend
                val jsonError = Gson().fromJson(errorBody, ResponseMessage::class.java)
                ResponseMessage(
                    status = "error",
                    message = jsonError?.message ?: "Server Error (${e.code()})"
                )
            } catch (parseException: Exception) {
                ResponseMessage(
                    status = "error",
                    message = "Terjadi kesalahan pada server (${e.code()})"
                )
            }
        } catch (e: ConnectException) {
            ResponseMessage(
                status = "error",
                message = "Gagal terhubung ke server. Periksa koneksi internet atau URL backend."
            )
        } catch (e: SocketTimeoutException) {
            ResponseMessage(
                status = "error",
                message = "Koneksi lambat (Timeout). Silakan coba lagi."
            )
        } catch (e: UnknownHostException) {
            ResponseMessage(
                status = "error",
                message = "Server tidak ditemukan. Periksa URL di build.gradle."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseMessage(
                status = "error",
                message = e.message ?: "Terjadi kesalahan yang tidak diketahui"
            )
        }
    }
}