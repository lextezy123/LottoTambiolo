# kotlinx.serialization: keep generated serializers for our data classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.lextezy.tambiolo.**$$serializer { *; }
-keepclassmembers class com.lextezy.tambiolo.** {
    *** Companion;
}
-keepclasseswithmembers class com.lextezy.tambiolo.** {
    kotlinx.serialization.KSerializer serializer(...);
}
