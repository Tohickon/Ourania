# Ourania Windows - clean build script
#
# Compiles all sources to an output directory (never into src\main\java).
#
# Usage:
#   .\build.ps1                                   # build only
#   .\build.ps1 AspectGridCheck                   # build, then run a named suite
#   .\build.ps1 com.zodiacomputing.ourania.gui.AspectGridCheck   # fully qualified works too
#   .\build.ps1 -Out out-claude CompositeCheck    # build into a private directory
#   .\build.ps1 -All                              # build, then run EVERY suite (the regression)
#   .\build.ps1 -All -Only BodyCheck,DataCheck    # the same machinery, for just these suites
#   .\build.ps1 -Jar                              # build dist\Ourania\: Ourania.jar, lib, data, ephe
#   .\build.ps1 -Package                          # -Jar, then a self-contained app with its own
#                                                 # Java runtime (needs jpackage: a full JDK)
#   .\build.ps1 -PurgeSrcClasses                  # also delete stale .class from the source tree
#
# WHY -Out EXISTS. This script deletes its output directory before building. Two agents
# sharing the default "out" therefore delete each other's classes mid-run: on 2026-08-28 a
# 21-suite verification was running from out\ when a second agent rebuilt, and every suite
# after the second returned nothing. If more than one agent may be building, each MUST pass
# its own -Out.
#
# WHICH JDK (J5, 2026-09-19). This used to be one hard-coded path to Android Studio's bundled
# runtime, so it ran on this machine and nowhere else - no CI, no second machine. It now looks
# in JAVA_HOME, then for javac on PATH, then at Android Studio's copy, and takes the first that
# is JDK 21 or newer. It compiles with --release 21, so a newer JDK builds the same program.

[CmdletBinding()]
param(
    [Parameter(Position = 0)] [string] $Suite,
    [string] $Out = "out",
    [switch] $PurgeSrcClasses,
    [switch] $All,
    [string[]] $Only = @(),
    [string[]] $KnownRed = @("known-red.txt"),
    [int]    $SuiteTimeoutMinutes = 45,
    # <b>How many astro suites run at once.</b> The gui block is always one at a time whatever
    # this says - it opens real windows, and three suites have already had to be rewritten after
    # flaking under load. 1 here reproduces the old behaviour exactly.
    #
    # <b>Four, measured twice in both orders.</b> The fourteen slowest astro suites, back to back
    # on one machine state: sequential 1135s against 378s in four lanes, and again with the order
    # reversed so the lanes could not borrow the sequential half's warm page cache, 4160s against
    # 2211s. 0.33x and 0.53x. Both favour lanes.
    #
    # <b>The first answer was the opposite, and was wrong.</b> reg19 at four lanes took 1428s where
    # reg18 sequential took 768s, and that was read as lanes costing nearly twice the wall clock.
    # The two runs were ninety minutes apart. They were not measuring the same machine.
    #
    # <b>What actually dominates every number here is the machine, and by a lot.</b> CalCheck, same
    # suite and same code: 165s in reg18 at 01:07, 182s at 03:40, and 1149s at 05:00 - seven times,
    # in one night, sequential each time. A thin 1.4 GHz laptop chassis sheds clock the longer it is
    # worked, and that swamps anything the lane count does.
    #
    # <b>So do not read a duration in SUMMARY.txt as evidence about the code.</b> Not in lanes and
    # not sequentially. If a suite's time looks meaningful, measure it again in the same session
    # against a control before believing it - the way a red suite is measured against the previous
    # commit. Failure SETS are what survive between runs; the clock does not.
    [int]    $Parallel = 4,
    [switch] $Jar,
    [switch] $Package,
    [ValidateSet("app-image", "exe", "msi")] [string] $PackageType = "app-image",
    [string] $Version = "0.1.0",
    [string] $Ephe = ""
)

$SRC = "src\main\java"
$MIN_JDK = 21

if (-not (Test-Path $SRC)) {
    Write-Host "ERROR: $SRC not found. Run this from the OuraniaWindows\ directory."
    exit 1
}

# -- find a JDK ------------------------------------------------------------------------------
function Get-JdkMajor([string] $javac) {
    $v = (& $javac -version 2>&1 | Out-String)
    $m = [regex]::Match($v, 'javac (\d+)')
    if ($m.Success) { return [int] $m.Groups[1].Value }
    return 0
}

$candidates = @()
if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME "bin") }
$onPath = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($onPath) { $candidates += (Split-Path $onPath.Source) }
$candidates += "C:\Program Files\Android\Android Studio\jbr\bin"

$JDK_BIN = $null
$tried = @()
foreach ($dir in $candidates) {
    $c = Join-Path $dir "javac.exe"
    if (-not (Test-Path $c)) { continue }
    $major = Get-JdkMajor $c
    $tried += "$dir (JDK $major)"
    if ($major -ge $MIN_JDK) { $JDK_BIN = $dir; break }
}
if (-not $JDK_BIN) {
    Write-Host "ERROR: no JDK $MIN_JDK or newer found."
    Write-Host "  Looked in JAVA_HOME, on PATH, and at Android Studio's bundled JDK."
    if ($tried.Count -gt 0) { Write-Host "  Found only: $($tried -join '; ')" }
    Write-Host "  Install one (e.g. Eclipse Temurin 21) and set JAVA_HOME to it."
    exit 1
}
$JAVAC = Join-Path $JDK_BIN "javac.exe"
$JAVA  = Join-Path $JDK_BIN "java.exe"
Write-Host "JDK: $JDK_BIN (JDK $(Get-JdkMajor $JAVAC))"

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
# Only lib\* on the classpath. Every source is passed explicitly, so putting $SRC on the
# classpath would only let javac resolve against the stale .class files warned about above.
#
# Exit code is the ONLY success signal. The previous version scanned output for the literal
# "error:" and continued when it found none - so a javac failure that did not print that
# string (internal error, OOM, unwritable -d) printed "Build OK" over a failed build.
#
# The sources go through an argument file: passed on the command line they come within a few
# hundred characters of Windows' limit once the checkout sits in a longer path, such as a CI
# runner's work directory.
# Absolute: .NET resolves a relative path against the process's directory, not PowerShell's.
$argFile = Join-Path (Resolve-Path $Out).Path "sources.txt"
[System.IO.File]::WriteAllLines($argFile,
    [string[]] ($sources | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' }))
& $JAVAC --release $MIN_JDK -encoding UTF-8 -nowarn -cp "lib\*" -d $Out "@$argFile"
$javacCode = $LASTEXITCODE
Remove-Item $argFile -Force
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

# The protocol rule 9 stamps, shared by every mode that measures something.
function Test-TreeUnchanged {
    $newestAfter = (Get-ChildItem -Recurse "$SRC\*.java" |
        Measure-Object -Property LastWriteTime -Maximum).Maximum
    Write-Host ""
    Write-Host "  SOURCE MTIME BEFORE : $newestBefore"
    Write-Host "  SOURCE MTIME AFTER  : $newestAfter"
    if ($newestBefore -ne $newestAfter) {
        Write-Host ""
        Write-Host "RESULT VOID - the source tree changed while the suites ran."
        Write-Host "Protocol rule 9: a measurement taken against a moving tree proves nothing."
        Write-Host "Rebuild and rerun. If another agent is working, pass your own -Out."
        return $false
    }
    Write-Host "  stamps match - measurement is valid."
    return $true
}

# -- -All: every suite, one verdict ----------------------------------------------------------
# A suite is every class under astro\ or gui\ whose name ends in Check, plus ZodiacSelfTest,
# that has a main - the same definition SettingsIsolationCheck holds every suite to.
#
# known-red.txt lists the suites that are red for a reason already written down. Without it a
# regression with three genuine, understood failures could only ever say "red", and a new
# failure would be invisible behind them. So: a suite going red that is NOT listed fails the
# run; a listed suite going green is reported so the list can be trimmed, but does not fail it.
if ($All) {
    # Each entry is a suite name and, optionally, how many failures are expected - a count
    # or a min-max range. The name alone used to be the whole gate, which meant a listed suite
    # could acquire brand new failures and still pass; reg17 did exactly that.
    $known = @{}
    # More than one list: CI adds known-red-ci.txt, the reds only its runner has.
    foreach ($list in $KnownRed) {
        if (-not (Test-Path $list)) { Write-Host "ERROR: no known-red list '$list'"; exit 1 }
        foreach ($line in Get-Content $list) {
            $entry = ($line -replace '#.*$', '').Trim()
            if (-not $entry) { continue }
            $parts = $entry -split '\s+'
            $name = $parts[0]
            $lo = $null
            $hi = $null
            if ($parts.Count -ge 2) {
                $spec = $parts[1]
                if ($spec -match '^(\d+)-(\d+)$') {
                    $lo = [int] $Matches[1]; $hi = [int] $Matches[2]
                } elseif ($spec -match '^(\d+)$') {
                    $lo = [int] $Matches[1]; $hi = $lo
                } else {
                    Write-Host "ERROR: '$spec' is not a count or a range, for $name in $list"
                    exit 1
                }
                if ($null -ne $hi -and $hi -lt $lo) {
                    Write-Host "ERROR: range runs backwards for $name in $list"; exit 1
                }
            }
            $known[$name] = @{ lo = $lo; hi = $hi }
        }
    }
    $suites = @()
    foreach ($pkg in @("astro", "gui")) {
        foreach ($f in Get-ChildItem "$SRC\com\zodiacomputing\ourania\$pkg\*.java" | Sort-Object Name) {
            if (-not ($f.Name -like "*Check.java" -or $f.Name -eq "ZodiacSelfTest.java")) { continue }
            if (-not (Select-String -Path $f.FullName -Pattern "public static void main" -Quiet)) { continue }
            if ($Only.Count -gt 0 -and $Only -notcontains $f.BaseName) { continue }
            $suites += , @($pkg, $f.BaseName)
        }
    }
    $logs = "$Out-logs"
    if (Test-Path $logs) { Remove-Item -Recurse -Force $logs }
    New-Item -ItemType Directory -Path $logs | Out-Null
    # <b>What the suites took last time, so "time left" is measured rather than guessed.</b>
    # The spread is too wide for a running average to mean anything - CalCheck is 333 seconds and
    # DignityCheck is 1 - so the estimate is the recorded cost of the suites still to run.
    $past = @{}
    # <b>Newest is not good enough.</b> A four-suite -Only run writes a SUMMARY too, and being the
    # newest it became the baseline for the next full run - seventy suites falling back to an
    # average taken from four, and the whole regression estimated at five minutes. A baseline has
    # to carry at least as many suites as the run about to use it.
    $baseline = $null
    $candidates = @(Get-ChildItem -Path "*-logs\SUMMARY.txt" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -ne (Join-Path (Resolve-Path ".").Path "$logs\SUMMARY.txt") } |
        Sort-Object LastWriteTime -Descending)
    foreach ($c in $candidates) {
        $seen = @{}
        foreach ($line in Get-Content $c.FullName) {
            # Out-File -Encoding utf8 writes a BOM in 5.1, so the first line of every summary
            # starts with an invisible character and its suite name never matched.
            $clean = $line.TrimStart([char] 0xFEFF)
            if ($clean -match '^\s*(\S+)\s+.*?(\d+)s\s*$') {
                $seen[$Matches[1]] = [int] $Matches[2]
            }
        }
        if ($seen.Count -ge $suites.Count) {
            $baseline = $c
            $past = $seen
            break
        }
    }
    $typical = 30
    if ($past.Count -gt 0) {
        $typical = [int] (($past.Values | Measure-Object -Average).Average)
    }

    # Seconds still to come, following the lanes: the astro block runs several at a time.
    $remaining = {
        $a = 0
        $g = 0
        foreach ($s in $suites) {
            if ($script:doneNames.Contains($s[1])) { continue }
            $cost = $typical
            if ($past.ContainsKey($s[1])) { $cost = $past[$s[1]] }
            if ($s[0] -eq "astro") { $a += $cost } else { $g += $cost }
        }
        return [int] (($a / [Math]::Max(1, $Parallel)) + $g)
    }
    $doneNames = New-Object 'System.Collections.Generic.HashSet[string]'

    Write-Host ""
    Write-Host "Running $($suites.Count) suites from $Out (logs in $logs, $SuiteTimeoutMinutes min each at most)"
    if ($baseline) {
        $firstGuess = [int] ((& $remaining) / 60)
        Write-Host "Timings from $($baseline.FullName) - about $firstGuess min at -Parallel $Parallel"
    } else {
        Write-Host "No previous SUMMARY.txt to time against, so no estimate this run."
    }
    Write-Host ""

    $rows = @()
    $newRed = @()
    $nowGreen = @()
    $unguarded = @()

    # Starting a suite is just launching its process; nothing waits here.
    $startSuite = {
        param($pkg, $name)
        $fq = "com.zodiacomputing.ourania.$pkg.$name"
        $log = Join-Path $logs "$name.txt"
        $p = Start-Process -FilePath $JAVA -ArgumentList @("-cp", "`"$Out;lib\*`"", $fq) `
            -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
        $null = $p.Handle   # without this, ExitCode reads as null once the process has gone
        return @{ name = $name; log = $log; proc = $p; started = Get-Date }
    }

    # Reading one finished suite: its log, its verdict, and what the gate makes of it. Everything
    # that used to be the body of the loop, so one lane and four lanes judge identically.
    $harvest = {
        param($a)
        $name = $a.name
        $log = $a.log
        $p = $a.proc
        $timedOut = $false
        $left = ($a.started.AddMinutes($SuiteTimeoutMinutes) - (Get-Date)).TotalMilliseconds
        if ($left -lt 0) { $left = 0 }
        if (-not $p.HasExited) {
            $timedOut = -not $p.WaitForExit([int] $left)
            if ($timedOut) { try { $p.Kill() } catch { } }
        }
        # And once more with no timeout: after a timed wait on a process whose output is
        # redirected, ExitCode stays empty until this is called - and every clear suite read red.
        if (-not $timedOut) { $p.WaitForExit() }
        $secs = [int] ((Get-Date) - $a.started).TotalSeconds
        $text = ""
        if (Test-Path $log) { $text = Get-Content $log -Raw }
        if (Test-Path "$log.err") { $text += (Get-Content "$log.err" -Raw) }
        if ($null -eq $text) { $text = "" }

        $verdict = "no verdict"
        $failCount = $null
        $m = [regex]::Match($text, "ALL CLEAR - ([\d,]+) checks")
        if ($m.Success) { $verdict = "clear, $($m.Groups[1].Value) checks" }
        $m = [regex]::Match($text, "FAILURES \((\d+) of ([\d,]+) checks\)")
        if ($m.Success) {
            $verdict = "$($m.Groups[1].Value) of $($m.Groups[2].Value) failed"
            $failCount = [int] $m.Groups[1].Value
        }
        $m = [regex]::Match($text, "FAILURES - ([\d,]+) checks, (\d+) failures")
        if ($m.Success) {
            $verdict = "$($m.Groups[2].Value) of $($m.Groups[1].Value) failed"
            $failCount = [int] $m.Groups[2].Value
        }
        if ($timedOut) { $verdict = "TIMED OUT after $SuiteTimeoutMinutes min" }

        # -cnotmatch, case-sensitive: PowerShell's -notmatch ignores case, and every clear suite
        # ends "0 failures." - which read as FAILURES and turned every one of them red.
        $passed = (-not $timedOut) -and ($p.ExitCode -eq 0) -and ($text -cnotmatch "FAILURES")
        $status = "pass"
        if (-not $passed) {
            if ($known.ContainsKey($name)) {
                $spec = $known[$name]
                if ($null -eq $spec.hi) {
                    # No count on the entry: the old behaviour, and named as such at the end so
                    # nobody mistakes it for a guarded one.
                    $status = "known red (unguarded)"
                    $unguarded += $name
                } elseif ($timedOut -or $null -eq $failCount) {
                    # A listed suite that did not say how many failed cannot be checked against
                    # its count, and waving it through is the hole this closes.
                    $status = "known red, NO COUNT"
                    $newRed += "$name (listed at $($spec.lo)-$($spec.hi) but printed no count)"
                } elseif ($failCount -gt $spec.hi) {
                    $status = "MORE RED THAN KNOWN"
                    $newRed += "$name ($failCount failures, $($spec.hi) expected at most)"
                } elseif ($failCount -lt $spec.lo) {
                    $status = "less red than known"
                    $nowGreen += "$name ($failCount failures, $($spec.lo) expected at least)"
                } else {
                    $status = "known red"
                }
            } else { $status = "RED"; $newRed += $name }
        } elseif ($known.ContainsKey($name)) {
            $status = "pass (listed as known red)"
            $nowGreen += $name
        }
        $row = "{0,-26} {1,-26} {2,-34} {3,5}s" -f $name, $status, $verdict, $secs
        $script:rows += $row

        $null = $script:doneNames.Add($name)
        $done = $script:doneNames.Count
        $total = $suites.Count
        $pct = [int] (100 * $done / $total)
        $left = & $remaining
        $note = ""
        if ($baseline) {
            $note = "  ~{0} min left" -f [Math]::Ceiling($left / 60.0)
        }
        # The console's own bar. It goes to the progress stream, so a redirected log never
        # sees it - which is why the line below goes to stdout as well.
        Write-Progress -Id 1 -Activity "Suites" `
            -Status "$done of $total ($pct%)" -PercentComplete $pct -SecondsRemaining $left
        Write-Host ("  {0}  [{1}/{2}] {3}%{4}" -f $row, $done, $total, $pct, $note)
    }

    # <b>The astro block, several at a time.</b> Each is its own process with its own temp
    # settings file, so there is nothing to share and nothing to collide over.
    $lanes = [Math]::Max(1, $Parallel)
    $astro = @($suites | Where-Object { $_[0] -eq "astro" })
    $gui = @($suites | Where-Object { $_[0] -eq "gui" })
    $active = @()
    $next = 0
    while ($next -lt $astro.Count -or $active.Count -gt 0) {
        while ($active.Count -lt $lanes -and $next -lt $astro.Count) {
            $active += (& $startSuite $astro[$next][0] $astro[$next][1])
            $next++
        }
        Start-Sleep -Milliseconds 150
        $stillRunning = @()
        foreach ($a in $active) {
            if ($a.proc.HasExited -or
                    ((Get-Date) -gt $a.started.AddMinutes($SuiteTimeoutMinutes))) {
                . $harvest $a
            } else {
                $stillRunning += $a
            }
        }
        $active = $stillRunning
    }

    # <b>The gui block, one at a time, deliberately.</b> These open real windows, and this project
    # has already had to rewrite three suites that flaked under regression load. Eighteen minutes
    # is not worth buying that back.
    foreach ($s in $gui) {
        . $harvest (& $startSuite $s[0] $s[1])
    }

    # <b>Sorted, not in completion order.</b> These files get compared between runs to see
    # what moved; an order that depended on which suite finished first would make that useless.
    Write-Progress -Id 1 -Activity "Suites" -Completed
    $summary = Join-Path $logs "SUMMARY.txt"
    $rows | Sort-Object | Out-File -Encoding utf8 $summary
    Write-Host ""
    Write-Host "Summary: $summary"
    $valid = Test-TreeUnchanged
    if (-not $valid) { exit 1 }
    if ($nowGreen.Count -gt 0) {
        Write-Host ""
        Write-Host "Listed as red but did better - tighten or remove the entry: $($nowGreen -join ', ')"
    }
    if ($unguarded.Count -gt 0) {
        Write-Host ""
        Write-Host "Listed with NO expected count, so new failures in them are invisible: $($unguarded -join ', ')"
    }
    if ($newRed.Count -gt 0) {
        Write-Host ""
        Write-Host "REGRESSION RED - $($newRed.Count): $($newRed -join '; ')"
        exit 1
    }
    Write-Host ""
    Write-Host "REGRESSION OK - every suite passed, or failed exactly as known-red.txt expects."
    exit 0
}

# -- -Jar / -Package: the app as something that runs away from this tree (J4) --------------
# dist\Ourania\ holds Ourania.jar with lib\, data\ and ephe\ beside it - the layout AppPaths
# looks for when the app runs from a jar. The data is not packed into the jar: 83 MB of prose
# the loaders read as files, and that one of them rewrites in place.
if ($Jar -or $Package) {
    $JAR_EXE = Join-Path $JDK_BIN "jar.exe"
    $dist = "dist\Ourania"
    if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }
    New-Item -ItemType Directory -Path "$dist\lib" | Out-Null

    $libJars = @(Get-ChildItem "lib\*.jar")
    $libJars | Copy-Item -Destination "$dist\lib"
    $classPath = ($libJars | ForEach-Object { "lib/" + $_.Name }) -join " "
    $manifest = Join-Path (Resolve-Path $Out).Path "MANIFEST.MF"
    [System.IO.File]::WriteAllText($manifest,
        "Main-Class: com.zodiacomputing.ourania.gui.OuraniaWindow`r`nClass-Path: $classPath`r`n")
    & $JAR_EXE --create --file "$dist\Ourania.jar" --manifest $manifest -C $Out com -C $Out de
    if ($LASTEXITCODE -ne 0) { Write-Host "JAR FAILED - jar exit $LASTEXITCODE"; exit 1 }

    Copy-Item -Recurse "src\main\resources\data" "$dist\data"

    # The ephemeris: -Ephe, else OURANIA_EPHE, else the path this project has always used.
    if (-not $Ephe) { $Ephe = $env:OURANIA_EPHE }
    if (-not $Ephe) { $Ephe = "C:\Users\daver\Desktop\Ourania\decoded_apk\assets" }
    if (Test-Path $Ephe) {
        New-Item -ItemType Directory -Path "$dist\ephe" | Out-Null
        $epheRoot = (Resolve-Path $Ephe).Path.TrimEnd('\')
        foreach ($f in Get-ChildItem -Recurse -File $Ephe -Include *.se1, sefstars.txt, seasnam.txt) {
            $rel = $f.FullName.Substring($epheRoot.Length + 1)
            $to = Join-Path "$dist\ephe" $rel
            New-Item -ItemType Directory -Force -Path (Split-Path $to) | Out-Null
            Copy-Item $f.FullName $to
        }
        $n = @(Get-ChildItem -Recurse -File "$dist\ephe").Count
        Write-Host "Ephemeris: $n files from $Ephe"
    } else {
        Write-Host "WARNING: no ephemeris at '$Ephe' - the packaged app will fall back to Moshier"
        Write-Host "         (lower precision, and no Chiron or other asteroids). Pass -Ephe."
    }
    # Where this tree's settings and chart book are, for the packaged app to copy ONCE into the
    # reader's own folder on its first launch (AppPaths.importOnce). Not on a CI runner: an app
    # built there is downloaded by someone else and must start fresh.
    if (-not $env:CI -and (Test-Path "settings.properties")) {
        [System.IO.File]::WriteAllText((Join-Path (Resolve-Path $dist).Path "import-from.txt"),
            (Get-Location).Path)
        Write-Host "First launch will copy settings and charts once from $((Get-Location).Path)"
    }
    foreach ($doc in @("..\THIRD-PARTY-NOTICES.md", "..\PROVENANCE.md", "..\LICENSE")) {
        if (Test-Path $doc) { Copy-Item $doc $dist }
    }
    Write-Host ""
    Write-Host "Jar OK -> $dist\Ourania.jar  (run: java -jar $dist\Ourania.jar)"

    if ($Package) {
        $JPACKAGE = Join-Path $JDK_BIN "jpackage.exe"
        if (-not (Test-Path $JPACKAGE)) {
            Write-Host ""
            Write-Host "PACKAGE FAILED - this JDK ($JDK_BIN) has no jpackage."
            Write-Host "  Android Studio's bundled runtime leaves it out. Use a full JDK 21 (e.g. Temurin),"
            Write-Host "  set JAVA_HOME to it, and run again. CI does this."
            exit 1
        }
        & $JPACKAGE --type $PackageType --name Ourania --app-version $Version `
            --vendor "Ourania" --input $dist --main-jar Ourania.jar `
            --main-class com.zodiacomputing.ourania.gui.OuraniaWindow --dest "dist\package"
        if ($LASTEXITCODE -ne 0) { Write-Host "PACKAGE FAILED - jpackage exit $LASTEXITCODE"; exit 1 }
        Write-Host ""
        Write-Host "Package OK -> dist\package ($PackageType)"
    }
    exit 0
}

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

if (-not (Test-TreeUnchanged)) { exit 1 }
exit $suiteCode
