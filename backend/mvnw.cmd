@echo off
set "MAVEN_BIN=%~dp0.mvn\maven\bin\mvn.cmd"
if exist "%MAVEN_BIN%" (
  call "%MAVEN_BIN%" %*
) else (
  mvn %*
)
