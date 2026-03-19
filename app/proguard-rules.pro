# Keep the libxposed module entry point and its specific instantiation constructor
-keep public class io.github.jjhitel.dezux.MainHook {
    public <init>(io.github.libxposed.api.XposedModuleInterface, io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
}

# Keep hooker implementations to prevent R8 from stripping or aggressively renaming them
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker {
    *;
}

-repackageclasses ""
-allowaccessmodification
-dontwarn java.lang.reflect.AnnotatedType