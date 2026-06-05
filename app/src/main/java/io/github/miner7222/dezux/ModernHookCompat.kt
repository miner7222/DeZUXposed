package io.github.miner7222.dezux

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable

internal class ModernHookScope(
    private val module: XposedModule,
    val classLoader: ClassLoader,
) {
    fun install(name: String, block: ModernHookScope.() -> Unit) {
        runCatching { block() }
            .onSuccess { module.log(Log.INFO, TAG, "Installed $name") }
            .onFailure { module.log(Log.ERROR, TAG, "Failed to install $name", it) }
    }

    fun beforeMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
        block: HookCall.() -> Unit,
    ) {
        hookMethod(className, methodName, parameterTypes) { chain ->
            HookChainRunner.runBefore(chain, block)
        }
    }

    fun afterMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
        block: HookCall.() -> Unit,
    ) {
        hookMethod(className, methodName, parameterTypes) { chain ->
            HookChainRunner.runAfter(chain, block)
        }
    }

    fun replaceMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
        block: HookCall.() -> Any?,
    ) {
        hookMethod(className, methodName, parameterTypes) { chain ->
            HookChainRunner.runReplace(chain, block)
        }
    }

    fun beforeConstructor(
        className: String,
        vararg parameterTypes: Class<*>,
        block: HookCall.() -> Unit,
    ) {
        hookConstructor(className, parameterTypes) { chain ->
            HookChainRunner.runBefore(chain, block)
        }
    }

    fun afterConstructor(
        className: String,
        vararg parameterTypes: Class<*>,
        block: HookCall.() -> Unit,
    ) {
        hookConstructor(className, parameterTypes) { chain ->
            HookChainRunner.runAfter(chain, block)
        }
    }

    fun replaceConstructor(
        className: String,
        vararg parameterTypes: Class<*>,
        block: HookCall.() -> Any?,
    ) {
        hookConstructor(className, parameterTypes) { chain ->
            HookChainRunner.runReplace(chain, block)
        }
    }

    fun loadClass(className: String): Class<*> {
        return Class.forName(className, false, classLoader)
    }

    private fun hookMethod(
        className: String,
        methodName: String,
        parameterTypes: Array<out Class<*>>,
        block: (XposedInterface.Chain) -> Any?,
    ) {
        val executable = HookExecutableResolver.resolveMethod(
            classLoader = classLoader,
            className = className,
            methodName = methodName,
            parameterTypes = parameterTypes,
        )
        hookExecutable(executable, block)
    }

    private fun hookConstructor(
        className: String,
        parameterTypes: Array<out Class<*>>,
        block: (XposedInterface.Chain) -> Any?,
    ) {
        val executable = HookExecutableResolver.resolveConstructor(
            classLoader = classLoader,
            className = className,
            parameterTypes = parameterTypes,
        )
        hookExecutable(executable, block)
    }

    private fun hookExecutable(
        executable: Executable,
        block: (XposedInterface.Chain) -> Any?,
    ) {
        module.hook(executable)
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept(block)
    }

    private companion object {
        private const val TAG = "DeZUXHook"
    }
}

internal object HookExecutableResolver {
    fun resolveMethod(
        classLoader: ClassLoader,
        className: String,
        methodName: String,
        parameterTypes: Array<out Class<*>>,
    ): Executable {
        val targetClass = Class.forName(className, false, classLoader)
        val executable = runCatching {
            targetClass.getDeclaredMethod(methodName, *parameterTypes)
        }.getOrElse {
            throw missingExecutable("${targetClass.name}#$methodName${describeParameters(parameterTypes)}", it)
        }
        executable.isAccessible = true
        return executable
    }

    fun resolveConstructor(
        classLoader: ClassLoader,
        className: String,
        parameterTypes: Array<out Class<*>>,
    ): Executable {
        val targetClass = Class.forName(className, false, classLoader)
        val executable = runCatching {
            targetClass.getDeclaredConstructor(*parameterTypes)
        }.getOrElse {
            throw missingExecutable("${targetClass.name}#<init>${describeParameters(parameterTypes)}", it)
        }
        executable.isAccessible = true
        return executable
    }

    private fun missingExecutable(message: String, cause: Throwable): NoSuchMethodException {
        return NoSuchMethodException(message).apply { initCause(cause) }
    }

    private fun describeParameters(parameterTypes: Array<out Class<*>>): String {
        return parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
    }
}

internal object HookChainRunner {
    fun runBefore(chain: XposedInterface.Chain, block: HookCall.() -> Unit): Any? {
        val call = HookCall(chain)
        call.block()
        return chain.proceed(call.args)
    }

    fun runAfter(chain: XposedInterface.Chain, block: HookCall.() -> Unit): Any? {
        val call = HookCall(chain, chain.proceed())
        call.block()
        return call.result
    }

    fun runReplace(chain: XposedInterface.Chain, block: HookCall.() -> Any?): Any? {
        return HookCall(chain).block()
    }
}

internal class HookCall(
    private val chain: XposedInterface.Chain,
    initialResult: Any? = null,
) {
    val args: Array<Any?> = chain.args.toTypedArray()
    val instanceOrNull: Any? = chain.thisObject
    var result: Any? = initialResult

    fun callOriginal(): Any? = chain.proceed(args)
}
