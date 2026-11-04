@echo off
rem Compiles the program, then the JUnit tests, then runs every test.
rem Needs JDK 17 or later on the PATH. Run from this folder: build.bat
set JUNIT=lib\junit-platform-console-standalone-1.10.5.jar
if exist out rmdir /s /q out
javac --release 17 -d out src\sim\*.java src\airport\*.java src\results\*.java src\app\*.java || goto failed
javac --release 17 -d out -cp "out;%JUNIT%" test\sim\*.java test\airport\*.java test\results\*.java test\app\*.java || goto failed
java -jar %JUNIT% execute -cp out --scan-classpath --disable-banner
exit /b

:failed
echo.
echo Build failed. This program needs JDK 17 or later. The javac found first on the PATH is:
javac -version
echo If that is older than 17, put a newer JDK first on the PATH, for example:
echo   set PATH=C:\path\to\jdk-17-or-later\bin;%%PATH%%
exit /b 1
