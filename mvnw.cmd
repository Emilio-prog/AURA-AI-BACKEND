@echo off
setlocal
set BASE_DIR=%~dp0
set MAVEN_PROJECTBASEDIR=%BASE_DIR:~0,-1%
set WRAPPER_DIR=%BASE_DIR%.mvn\wrapper
set WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
if "%MAVEN_USER_HOME%"=="" set MAVEN_USER_HOME=%BASE_DIR%.mvn\maven-user-home

if "%JAVA_HOME%"=="" (
  for /d %%D in ("%USERPROFILE%\.jdks\ms-21*" "%USERPROFILE%\.jdks\jdk-21*" "%USERPROFILE%\.jdks\temurin-21*" "%USERPROFILE%\.jdks\*21*") do (
    if exist "%%~fD\bin\java.exe" (
      set "JAVA_HOME=%%~fD"
      goto java_home_detected
    )
  )
)
:java_home_detected

if "%JAVA_HOME%"=="" (
  set JAVA_EXE=java
) else (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -version >NUL 2>&1
if errorlevel 1 (
  echo Java 21 is required and was not found in JAVA_HOME or PATH.
  exit /b 1
)

if not exist "%WRAPPER_JAR%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 exit /b 1
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
set EXIT_CODE=%ERRORLEVEL%
endlocal & exit /b %EXIT_CODE%
