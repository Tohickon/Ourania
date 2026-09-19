# Ourania Windows - clean build script
#
# Compiles all sources to an output directory (never into src\main\java).
#
# Usage:
#   .\build.ps1                                   # build only
#   .\build.ps1 AspectGridCheck                   # build, then run a named suite
#   .\build.ps1 com.zodiacomputing.ourania.gui.AspectGridCheck   # fully qualified works too
#   .\build.ps1 -Out out-claude CompositeCheck    # build into a private directory
#   .\build.ps1 -PurgeSrcClasses                  # also delete stale .class from the source tree
#
# WHY -Out EXISTS. This script deletes its output directory before building. Two agents
# sharing the default "out" therefore delete each other's classes mid-run: on 2026-08-28 a
# 21-suite verification was running from out\ when a second agent rebuilt, and every suite
# after the second returned nothing. If more than one agent may be building, each MUST pass
# its own -Out.

[CmdletBinding()]
param(
    [Parameter(Position = 0)] [string] $Suite,
    [string] $Out = "out",
    [switch] $PurgeSrcClasses
)

$JAVAC = "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
$JAVA  = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$SRC   = "src\main\java"

if (-not (Test-Path $SRC)) {
    Write-Host "ERROR: $SRC not found. Run this from the OuraniaWindows\ directory."
    exit 1
}

# -- stale .class files in the SOURCE tree -------------------------------------------------
# This build ignores them (see the -cp note below), but the run command documented in
# CLAUDE.md and HANDOVER is `java -cp src\main\java ...`, which loads them in preference to
# anything built here. They are gitignored, so whose build they came from is unknowable.
$stale = @(Get-ChildItem -Recurse -Filter *.class $SRC -ErrorAction SilentlyContinue)
if ($stale.Count -gt 0) {
    if ($PurgeSrcClasses) {
        $stale | Remove-Item -Force
        Write-Host "Purged $($stale.Count) stale .class files from $SRC"
    } else {
        Write-Host "WARNING: $($stale.Count) .class files are sitting in $SRC."
        Write-Host "         This build ignores them, but 'java -cp $SRC ...' loads them first."
        Write-Host "         Re-run with -PurgeSrcClasses to remove them (they regenerate)."
    }
}

# -- clean output dir so stale classes cannot survive ---------------------------------------
if (Test-Path $Out) { Remove-Item -Recurse -Force $Out }
New-Item -ItemType Directory -Path $Out | Out-Null

# -- collect sources ------------------------------------------------------------------------
$sources = @(Get-ChildItem -Recurse "$SRC\*.java")
if ($sources.Count -eq 0) {
    Write-Host "ERROR: no .java files found under $SRC"
    exit 1
}
$newestBefore = ($sources | Measure-Object -Property LastWriteTime -Maximum).Maximum

# -- compile ---------------------------------------------------------------------------------
# No -cp. Every source is passed explicitly, so the only thing putting $SRC on the classpath
# achieved was letting javac resolve against the stale .class files warned about above.
# Verified 2026-08-28: 147 sources compile to 241 classes with no classpath at all.
#
# Exit code is the ONLY success signal. The previous version scanned output for the literal
# "error:" and continued when it found none - so a javac failure that did not print that
# string (internal error, OOM, unwritable -d) printed "Build OK" over a failed build.
& $JAVAC -cp "lib\*" -d $Out ($sources | Select-Object -ExpandProperty FullName)
$javacCode = $LASTEXITCODE
if ($javacCode -ne 0) {
    Write-Host ""
    Write-Host "BUILD FAILED - javac exit $javacCode. No suite was run."
    exit 1
}

$classCount = @(Get-ChildItem -Recurse -Filter *.class $Out).Count
Write-Host ""
Write-Host "Build OK -> $Out"
Write-Host "  BUILD FINGERPRINT"
Write-Host "    sources       : $($sources.Count)"
Write-Host "    classes       : $classCount"
Write-Host "    newest source : $newestBefore"
Write-Host "    built at      : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

if (-not $Suite) { exit 0 }

# -- resolve the suite name -------------------------------------------------------------------
if ($Suite -match '\.') {
    $fq = $Suite
} elseif (Test-Path "$Out\com\zodiacomputing\ourania\gui\$Suite.class") {
    $fq = "com.zodiacomputing.ourania.gui.$Suite"
} elseif (Test-Path "$Out\com\zodiacomputing\ourania\astro\$Suite.class") {
    $fq = "com.zodiacomputing.ourania.astro.$Suite"
} else {
    Write-Host "ERROR: no class named '$Suite' under $Out (looked in .gui and .astro)."
    exit 1
}

# -- run, with the protocol rule 9 stamps -------------------------------------------------------
Write-Host ""
Write-Host "Running $fq from $Out"
Write-Host ""
& $JAVA -cp "$Out;lib\*" $fq
$suiteCode = $LASTEXITCODE

$newestAfter = (Get-ChildItem -Recurse "$SRC\*.java" |
    Measure-Object -Property LastWriteTime -Maximum).Maximum
Write-Host ""
Write-Host "  SOURCE MTIME BEFORE : $newestBefore"
Write-Host "  SOURCE MTIME AFTER  : $newestAfter"
if ($newestBefore -ne $newestAfter) {
    Write-Host ""
    Write-Host "RESULT VOID - the source tree changed while the suite ran."
    Write-Host "Protocol rule 9: a measurement taken against a moving tree proves nothing."
    Write-Host "Rebuild and rerun. If another agent is working, pass your own -Out."
    exit 1
}
Write-Host "  stamps match - measurement is valid."
exit $suiteCode
