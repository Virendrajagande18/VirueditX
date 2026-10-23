# VirueditX release rules (the online CI build ships the debug APK; release is optional).
# PDFBox: keep resource loading and font handling intact.
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.fontbox.**
