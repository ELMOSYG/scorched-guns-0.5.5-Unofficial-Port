@echo off
rem Launch the dev client straight into the local dev server so the blindness overlay can be
rem checked with a real player (a dedicated server alone cannot show a client-side overlay).
set JAVA_HOME=D:\jdk-21.0.3
call gradlew.bat runClient "--args=--quickPlayMultiplayer 127.0.0.1:25565" --console=plain > build-logs\client-3-join.txt 2>&1
