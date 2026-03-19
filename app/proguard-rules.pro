# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Сохраняем JavaScript-интерфейс, используемый в WebView
-keepclassmembers class com.safelogj.ch4ir.JavaScriptBridge {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class androidx.activity.EdgeToEdge { *; }

# Защищает от удаления тем и стилей
-keep class **.R$styleable { *; }
-keep class **.R$style { *; }
-keep class **.R$attr { *; }

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

