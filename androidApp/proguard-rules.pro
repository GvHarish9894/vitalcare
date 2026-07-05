# kotlinx.serialization — keep generated serializers for typed navigation
# routes (D-008) and the backup document (D-023). Room/Koin/Ktor/Compose ship
# their own consumer rules.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.techgv.vitalcare.**$$serializer { *; }
-keepclassmembers class com.techgv.vitalcare.** {
    *** Companion;
}
-keepclasseswithmembers class com.techgv.vitalcare.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Play Services authorization result parsing uses reflection-free builders —
# no extra rules needed; keep this file as the single place to add any.
