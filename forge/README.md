# Forge

Forge source code. Relies on common for audio transcription / processing.

## Test Server

Use the `runServer` task, agree to the eula.txt, and set `online-mode=false` in server.properties. Then spawn as many instances as you want with `runClient`.
To spawn as many instances as you want in IntelliJ IDEA, <kbd>CTRL</kbd> + <kbd>OPTION</kbd> + <kbd>R</kbd> to open run configurations. Navigate to the task you want (like `runClient`), **modify options**, and toggle allow multiple instances