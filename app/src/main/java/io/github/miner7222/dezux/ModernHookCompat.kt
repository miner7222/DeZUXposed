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

    fun loadClass(className: String): Class<*> {
        return Class.forName(className, false, classLoader)
    }

    private fun hookMethod(
        className: String,
        methodName: String,
        parameterTypes: Array<out Class<*>>,
        block: (XposedInterface.Chain) -> Any?,
    ) {
        val executable = resolveExecutable(className, methodName, parameterTypes)
        module.hook(executable)
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept(block)
    }

    private fun resolveExecutable(
        className: String,
        methodName: String,
        parameterTypes: Array<out Class<*>>,
    ): Executable {
        val targetClass = loadClass(className)
        val executable = targetClass.declaredMethods.firstOrNull {
            it.name == methodName && it.parameterTypes.contentEquals(parameterTypes)
        } ?: throw NoSuchMethodException("${targetClass.name}#$methodName${describeParameters(parameterTypes)}")
        executable.isAccessible = true
        return executable
    }

    private fun describeParameters(parameterTypes: Array<out Class<*>>): String {
        return parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
    }

    private companion object {
        private const val TAG = "DeZUXHook"
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
