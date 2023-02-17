package com.cmcc.newcalllib.tool.thread

import bolts.Task
import com.cmcc.newcalllib.tool.LogUtil
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author jihongfei
 * @createTime 2022/3/16 17:49
 */
object ThreadPoolUtil {
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    private val CORE_POOL_SIZE = CPU_COUNT + 1
    private val MAX_POOL_SIZE = CPU_COUNT * 2 + 1
    private const val KEEP_ALIVE_TIME = 5L
    private const val TIME_OUT_MILLIS = 2000L
    private const val THREAD_NAME_NETWORK = "io-thread"

    private val mIOExecutor = ThreadPoolExecutor(
        CORE_POOL_SIZE, MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            NamedTreadFactory(THREAD_NAME_NETWORK)
    )

    internal class NamedTreadFactory(private val mThreadName: String) : ThreadFactory {
        private val mThreadNum = AtomicInteger(1)
        override fun newThread(runnable: Runnable): Thread {
            val t = Thread(runnable, mThreadName + mThreadNum.getAndIncrement())
            LogUtil.d(TAG, "Thread [${t.name}] has been created")
            return t
        }

        companion object {
            private const val TAG = "NameTreadFactory"
        }
    }

    fun getIOExecutor(): ExecutorService {
        return mIOExecutor
    }

    fun <T> runOnUiThread(task: Callable<T>) {
        Task.call(task, Task.UI_THREAD_EXECUTOR)
    }

    fun <T, P> runOnUiThread(task: Callable<T>, callback: TaskCallback<T, P>?) {
        Task.call(task, Task.UI_THREAD_EXECUTOR)
            .continueWith { t ->
                callback?.process(t.result)
            }
    }

    fun <T, P> runOnIOThread(task: Callable<T>, callback: TaskCallback<T, P>?) {
        Task.call(task, mIOExecutor)
            .continueWith { t ->
                callback?.process(t.result)
            }
    }

    fun <T> runOnIOThread(task: Callable<T>) {
        Task.call(task, mIOExecutor)
    }

    fun <T, P> runOnIOThenUIThread(task: Callable<T>, callback: TaskCallback<T, P>?) {
        Task.call(task, mIOExecutor)
            .continueWith({ t ->
                callback?.process(t.result)
            }, Task.UI_THREAD_EXECUTOR)
    }

    fun shutdownAll() {
        mIOExecutor.shutdown()
        try {
            if (!mIOExecutor.awaitTermination(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS)) {
                mIOExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            mIOExecutor.shutdownNow()
        }
    }
}