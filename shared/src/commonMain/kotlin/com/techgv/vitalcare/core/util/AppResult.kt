package com.techgv.vitalcare.core.util

/**
 * Result type returned by repositories and use cases at layer boundaries (04 §8).
 * Exceptions are caught in the data layer and mapped to [AppError]; they never
 * cross into presentation.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data object NotFound : AppError

    /** The operation is not permitted by a business rule (e.g. editing a past record, BR-2). */
    data object NotAllowed : AppError

    data class Unknown(val cause: Throwable? = null) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)

fun AppError.asFailure(): AppResult.Failure = AppResult.Failure(this)
