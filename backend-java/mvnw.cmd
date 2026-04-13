@echo off
setlocal

set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12"
if not defined RECIPE_BOOK_STORAGE_PATH (
  for %%I in ("%~dp0..\data\db.json") do set "RECIPE_BOOK_STORAGE_PATH=%%~fI"
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Maven not found at "%MAVEN_HOME%".
  echo Update backend-java\mvnw.cmd with the correct local Maven path.
  exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
