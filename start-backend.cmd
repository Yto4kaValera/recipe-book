@echo off
setlocal

cd /d "%~dp0backend-java"
call mvnw.cmd spring-boot:run
