# Forge

Forge source code. Relies on JScribe for audio transcription.

## JRE Version

This targets Java 21. I've come to learn that using a different Java version (even a higher one) means there's problems downloading mappings. This might be due to a gradle wrapper version / Java version mismatch, but I'm not sure.

**TLDR: use JRE 21**

## Test Server

Use the `runServer` task, agree to the eula.txt, and set `online-mode=false` in server.properties. Then spawn as many instances as you want with `runClient`.