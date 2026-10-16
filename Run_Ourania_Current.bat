@echo off
title Ourania - current build

rem ---------------------------------------------------------------------------------------
rem Launches Ourania AND SAYS WHICH BUILD IT IS.
rem
rem Why the banner exists: on 2026-10-10 David ran the app, saw the old twelve-tab rail and
rem said "im not seeing a change at all". Nothing was wrong with the code - Run_Ourania.bat
rem runs the folder it sits beside, that folder was checked out on a branch three commits
rem before the work, and there was no way to tell from the window. An hour went into finding
rem that out. This prints the branch, the commit and the date before it starts, so the
rem question is answered before it is asked.
rem
rem It also says whether any source is newer than the compiled classes, which is the other way
rem to end up looking at yesterday's build: the app runs from src\main\java with the .class
rem files beside the sources, so an edit without a rebuild changes nothing on screen.
rem
rem The banner is one PowerShell call rather than a batch loop. The first version of it nested
rem two `for /f "delims=" %%x in ('dir ...')` loops to compare timestamps and was wrong in a way
rem that would have printed a reassuring line either way - which is precisely the failure this
rem file exists to prevent.
rem ---------------------------------------------------------------------------------------

rem One line to change if this should ever launch a different tree.
set "TREE=%~dp0OuraniaWindows"
set "JAVA=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"

if not exist "%TREE%" (
  echo.
  echo   The tree this launcher points at is not there:
  echo     %TREE%
  echo.
  pause
  exit /b 1
)

cd /d "%TREE%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$t = $env:TREE;" ^
  "Write-Host '';" ^
  "Write-Host '  =================================================================' -ForegroundColor DarkCyan;" ^
  "Write-Host '   OURANIA' -ForegroundColor Cyan;" ^
  "Write-Host '  =================================================================' -ForegroundColor DarkCyan;" ^
  "$g = (Get-Command git -ErrorAction SilentlyContinue).Source;" ^
  "if ($g) {" ^
  "  $b = & $g -C $t rev-parse --abbrev-ref HEAD 2>$null;" ^
  "  $s = & $g -C $t log -1 --format='%%h  %%cd' --date='format:%%Y-%%m-%%d %%H:%%M' 2>$null;" ^
  "  $m = & $g -C $t log -1 --format='%%s' 2>$null;" ^
  "  $d = & $g -C $t status --porcelain 2>$null;" ^
  "  Write-Host ('   branch : ' + $b) -ForegroundColor White;" ^
  "  Write-Host ('   commit : ' + $s);" ^
  "  Write-Host ('   last   : ' + $m) -ForegroundColor Gray;" ^
  "  if ($d) { Write-Host ('   NOTE   : ' + (($d | Measure-Object).Count) + ' uncommitted change(s) in this tree') -ForegroundColor Yellow }" ^
  "} else { Write-Host '   branch : (git not on PATH - cannot say)' -ForegroundColor Yellow }" ^
  "Write-Host ('   folder : ' + $t) -ForegroundColor DarkGray;" ^
  "$cls = Join-Path $t 'src\main\java\com\zodiacomputing\ourania\gui\OuraniaWindow.class';" ^
  "if (-not (Test-Path $cls)) {" ^
  "  Write-Host '   build  : NO CLASSES - run build.ps1 first; this will not start' -ForegroundColor Red" ^
  "} else {" ^
  "  $ct = (Get-Item $cls).LastWriteTime;" ^
  "  $src = Join-Path $t 'src\main\java\com\zodiacomputing\ourania';" ^
  "  $new = Get-ChildItem -Path $src -Recurse -Filter *.java -ErrorAction SilentlyContinue |" ^
  "         Where-Object { $_.LastWriteTime -gt $ct };" ^
  "  if ($new) {" ^
  "    Write-Host ('   build  : STALE - ' + (($new | Measure-Object).Count) + ' source file(s) newer than the classes. Rebuild, or this is an old build.') -ForegroundColor Red;" ^
  "    $new | Sort-Object LastWriteTime -Descending | Select-Object -First 3 |" ^
  "      ForEach-Object { Write-Host ('            ' + $_.Name + '  ' + $_.LastWriteTime.ToString('yyyy-MM-dd HH:mm')) -ForegroundColor DarkYellow }" ^
  "  } else {" ^
  "    Write-Host ('   build  : up to date (compiled ' + $ct.ToString('yyyy-MM-dd HH:mm') + ')') -ForegroundColor Green" ^
  "  }" ^
  "}" ^
  "Write-Host '  =================================================================' -ForegroundColor DarkCyan;" ^
  "Write-Host ''"

"%JAVA%" -cp "out;src\main\java;lib\*" com.zodiacomputing.ourania.gui.OuraniaWindow

echo.
echo   Ourania has closed. This window stays open so any message above can be read.
cmd /k
