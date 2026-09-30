# Add project specific ProGuard rules here.
# Rules are grown deliberately, phase by phase, as real shrink/obfuscation
# failures are hit in release builds — not speculatively.

# Keep line numbers (not full source paths) in obfuscated stack traces, so a
# release crash is still symbolicate-able against the uploaded mapping file
# without leaking original file paths -- Google's own recommended baseline,
# not a library-specific workaround.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
