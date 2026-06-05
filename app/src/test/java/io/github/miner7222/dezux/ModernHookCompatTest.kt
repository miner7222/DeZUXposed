package io.github.miner7222.dezux

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernHookCompatTest {

    @Test
    fun beforeRunnerPassesMutatedArgumentsToOriginalChain() {
        val chain = FakeChain(
            initialArgs = listOf("old"),
            original = { args -> "original:${args.single()}" },
        )

        val result = HookChainRunner.runBefore(chain) {
            args[0] = "new"
        }

        assertEquals("original:new", result)
        assertEquals(listOf(listOf("new")), chain.proceededArgs)
    }

    @Test
    fun afterRunnerCanReplaceOriginalResult() {
        val chain = FakeChain(
            initialArgs = listOf("input"),
            original = { "original:${it.single()}" },
        )

        val result = HookChainRunner.runAfter(chain) {
            assertEquals("original:input", this.result)
            this.result = "patched"
        }

        assertEquals("patched", result)
        assertEquals(listOf(listOf("input")), chain.proceededArgs)
    }

    @Test
    fun replaceRunnerSkipsOriginalWhenCallOriginalIsNotRequested() {
        val chain = FakeChain(
            initialArgs = listOf("input"),
            original = { "original:${it.single()}" },
        )

        val result = HookChainRunner.runReplace(chain) {
            args[0] = "manual"
            "replacement"
        }

        assertEquals("replacement", result)
        assertEquals(emptyList<List<Any?>>(), chain.proceededArgs)
    }

    @Test
    fun replaceRunnerCanCallOriginalWithMutatedArguments() {
        val chain = FakeChain(
            initialArgs = listOf("input"),
            original = { "original:${it.single()}" },
        )

        val result = HookChainRunner.runReplace(chain) {
            args[0] = "manual"
            "replace:${callOriginal()}"
        }

        assertEquals("replace:original:manual", result)
        assertEquals(listOf(listOf("manual")), chain.proceededArgs)
    }

    @Test
    fun resolverMatchesExactMethodOverload() {
        val executable = HookExecutableResolver.resolveMethod(
            classLoader = javaClass.classLoader!!,
            className = ResolverTarget::class.java.name,
            methodName = "overloaded",
            parameterTypes = arrayOf(String::class.java),
        )

        assertEquals(listOf(String::class.java), executable.parameterTypes.toList())
    }

    @Test
    fun resolverMatchesExactConstructorOverload() {
        val executable = HookExecutableResolver.resolveConstructor(
            classLoader = javaClass.classLoader!!,
            className = ResolverTarget::class.java.name,
            parameterTypes = arrayOf(Int::class.javaPrimitiveType!!),
        )

        assertEquals(listOf(Int::class.javaPrimitiveType!!), executable.parameterTypes.toList())
    }

    @Test
    fun resolverReportsReadableMissingMethodSignature() {
        val exception = runCatching {
            HookExecutableResolver.resolveMethod(
                classLoader = javaClass.classLoader!!,
                className = ResolverTarget::class.java.name,
                methodName = "overloaded",
                parameterTypes = arrayOf(Boolean::class.javaPrimitiveType!!),
            )
        }.exceptionOrNull()

        assertTrue(exception is NoSuchMethodException)
        assertEquals(
            "${ResolverTarget::class.java.name}#overloaded(boolean)",
            exception?.message,
        )
    }

    @Test
    fun resolverReportsReadableMissingConstructorSignature() {
        val exception = runCatching {
            HookExecutableResolver.resolveConstructor(
                classLoader = javaClass.classLoader!!,
                className = ResolverTarget::class.java.name,
                parameterTypes = arrayOf(Boolean::class.javaPrimitiveType!!),
            )
        }.exceptionOrNull()

        assertTrue(exception is NoSuchMethodException)
        assertEquals(
            "${ResolverTarget::class.java.name}#<init>(boolean)",
            exception?.message,
        )
    }

    private class FakeChain(
        initialArgs: List<Any?>,
        private val original: (List<Any?>) -> Any?,
    ) : XposedInterface.Chain {
        private var currentArgs = initialArgs.toList()
        val proceededArgs = ArrayList<List<Any?>>()

        override fun getExecutable(): Executable = TARGET_EXECUTABLE

        override fun getThisObject(): Any? = null

        override fun getArgs(): List<Any?> = currentArgs

        override fun getArg(index: Int): Any? = currentArgs[index]

        override fun proceed(): Any? = proceed(currentArgs.toTypedArray())

        override fun proceed(args: Array<Any?>): Any? {
            val snapshot = args.toList()
            proceededArgs += snapshot
            currentArgs = snapshot
            return original(snapshot)
        }

        override fun proceedWith(thisObject: Any): Any? = proceed()

        override fun proceedWith(thisObject: Any, args: Array<Any?>): Any? = proceed(args)
    }

    companion object {
        private val TARGET_EXECUTABLE: Executable =
            ModernHookCompatTest::class.java.getDeclaredMethod("targetMethod", String::class.java)

        @Suppress("unused")
        @JvmStatic
        private fun targetMethod(value: String): String = value
    }

    @Suppress("unused")
    private class ResolverTarget {
        constructor(value: String)
        constructor(value: Int)

        fun overloaded(value: String): String = value
        fun overloaded(value: Int): Int = value
    }
}
