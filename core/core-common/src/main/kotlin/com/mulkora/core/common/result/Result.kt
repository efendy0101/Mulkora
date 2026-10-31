package com.mulkora.core.common.result

/**
 * Mülkora genel sonuç tipi.
 * Repository ve UseCase katmanlarında kullanılır.
 *
 * Kotlin'in sealed class'ı ile güvenli tip yönetimi sağlar.
 */
sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val exception: Throwable,
        val message: String? = exception.message
    ) : Result<Nothing>()

    data object Loading : Result<Nothing>()
}

inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
    onLoading: () -> R = { throw IllegalStateException("Result is Loading") }
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error   -> onFailure(exception)
    is Result.Loading -> onLoading()
}

// --- Extension fonksiyonlar ---

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T> {
    if (this is Result.Error) action(exception, message)
    return this
}

inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

fun <T> Result<T>.getOrNull(): T? =
    if (this is Result.Success) data else null

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success
fun <T> Result<T>.isError(): Boolean = this is Result.Error
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error   -> this
    is Result.Loading -> Result.Loading
}