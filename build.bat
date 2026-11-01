@echo off
rem Compiles the program, then the JUnit tests, then runs every test.
rem Needs JDK 17 or later on the PATH. Run from this folder: build.bat
set JUNIT=lib\junit-platform-console-standalone-1.10.5.jar
if exist out rmdir /s /q out
javac --release 17 -d out src\sim\*.java src\airport\*.java src\results\*.java src\app\*.java || exit /b 1
javac --release 17 -d out -cp "out;%JUNIT%" test\sim\*.java test\airport\*.java test\results\*.java test\app\*.java || exit /b 1
java -jar %JUNIT% execute -cp out --scan-classpath --disable-banner
