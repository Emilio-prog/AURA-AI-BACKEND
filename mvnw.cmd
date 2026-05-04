@echo off
setlocal
set BASE_DIR=%~dp0
set WRAPPER_DIR=%BASE_DIR%.mvn\wrapper
set WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

if "%JAVA_HOME%"=="" (
  set JAVA_EXE=java
) else (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

if not exist "%WRAPPER_JAR%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 exit /b 1
)

"%JAVA_EXE%" -jar "%WRAPPER_JAR%" %*
endlocal
