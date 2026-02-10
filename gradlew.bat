@echo off
setlocal

set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0

set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar

java %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*