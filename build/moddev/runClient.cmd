@echo off
setlocal
for /f "tokens=2 delims=:." %%x in ('chcp') do set _codepage=%%x
chcp 65001>nul
cd C:\Users\PR1TCHA\Documents\Riftborne Mod\run
"C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\java.exe" "@C:\Users\PR1TCHA\Documents\Riftborne Mod\build\moddev\clientRunClasspath.txt" "@C:\Users\PR1TCHA\Documents\Riftborne Mod\build\moddev\clientRunVmArgs.txt" "-Dfml.modFolders=eventmod%%%%C:\Users\PR1TCHA\Documents\Riftborne Mod\build\classes\java\main;eventmod%%%%C:\Users\PR1TCHA\Documents\Riftborne Mod\build\resources\main" net.neoforged.devlaunch.Main "@C:\Users\PR1TCHA\Documents\Riftborne Mod\build\moddev\clientRunProgramArgs.txt"
if not ERRORLEVEL 0 (  echo Minecraft failed with exit code %ERRORLEVEL%  pause)
chcp %_codepage%>nul
endlocal