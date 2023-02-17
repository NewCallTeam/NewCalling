package com.cmcc.newcalllib.manage.entity

/**
 * Simple result wrapper
 * @author jihongfei
 * @createTime 2022/4/20 11:14
 */
data class Results<T>(
    val value: T? = null,
    val exception: Throwable? = null
) {
    public companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */
        public fun <T> success(value: T): Results<T> =
            Results(value = value)
        /**
         * Returns an instance that encapsulates the given Throwable as failure.
         */
        public fun <T> failure(exception: Throwable): Results<T> =
            Results(exception = exception)
        /**
         * Returns an instance that encapsulates the given Throwable as failure.
         */
        public fun <T> failure(errMsg: String): Results<T> =
            Results(exception = NewCallException(errMsg))
    }

    public fun isSuccess(): Boolean {
        return value != null
    }

    public fun value(): T {
        return value!!
    }

    public fun exception(): Throwable? {
        return exception
    }

    public fun getOrNull(): T? {
        return value
    }

    public fun getOrThrow(): T? {
        if (exception != null) {
            throw exception
        }
        return value
    }

    override fun toString(): String {
        return if (value != null) {
            "Success($value)"
        } else {
            "Failure($exception)"
        }
    }
}
