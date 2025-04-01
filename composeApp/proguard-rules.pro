-dontobfuscate
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keep class com.google.api.services.sheets.v4.model.UpdateValuesResponse { *; }
-keep class com.google.api.services.sheets.v4.model.ValueRange { *; }