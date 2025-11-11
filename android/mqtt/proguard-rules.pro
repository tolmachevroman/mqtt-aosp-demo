# Add project specific ProGuard rules here.

# MQTT Client - HiveMQ
-keepclassmembernames class io.netty.** { *; }
-keepclassmembers class org.jctools.** { *; }

# Keep MQTT related classes
-keep class com.mqtt.core.** { *; }
-keepclassmembers class com.mqtt.core.** { *; }
