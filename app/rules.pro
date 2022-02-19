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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 7
-dontpreverify
-forceprocessing

-allowaccessmodification
-optimizations
-dontoptimize
#-keeppackagenames
-dontnote
-dontwarn
-ignorewarnings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/var
-optimizations !code/simplification/cast,!code/simplification/advanced,!field/*,!class/merging/*,!method/removal/parameter,!method/propagation/parameter
-optimizations !code/allocation/variable
-optimizations !method/inlining/*
-optimizations !code/simplification/arithmetic
-keepattributes *Annotation*
#-repackageclasses 'o'
-obfuscationdictionary 'C:\Users\TeslaX\Documents\Source\dicdex.txt'
-classobfuscationdictionary 'C:\Users\TeslaX\Documents\Source\dicdex.txt'
-packageobfuscationdictionary 'C:\Users\TeslaX\Documents\Source\dicdex.txt'
-mergeinterfacesaggressively
-overloadaggressively
-verbose
-keepattributes Signature

#TODO need to remove SourceFile to hide file name, but what about crashlytics??
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

-keep class com.dzboot.ovpn.data.models.* {*;}
