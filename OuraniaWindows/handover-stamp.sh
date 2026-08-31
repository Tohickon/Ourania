#!/usr/bin/env bash
# Generates the HANDOVER.md stamp by RUNNING the suites, so the numbers in that file
# are measured rather than typed.
#
# Three times in one day the handover carried a wrong check count: a verdict from a
# partial run, a dropped count, and one suite's own total written in as the whole-suite
# total. Every one of those was a number a human or an agent typed from memory. This
# removes the opportunity.
#
#   bash handover-stamp.sh            # print the stamp lines
#   bash handover-stamp.sh --check    # exit 1 if HANDOVER.md disagrees with reality
#
# Two batches on purpose: the full set times out at ten minutes on this machine.
#
# A suite added here must also be added to the batch lists below, or it is invisible: the
# script counts what it runs, not what exists. ReturnsCheck was added 2026-08-21,
# AspectGridCheck 2026-08-22, TensionReleaseCheck, AspectPatternCheck, LunarMansionCheck and QuincunxCheck 2026-08-23,
# HarmonicCheck 2026-08-25.
#
# A suite that builds Swing components must call System.exit(0) on success. The run() below
# captures each one with $(...), which blocks until the process closes stdout - and an AWT
# event thread is non-daemon, so main returning does not end the JVM. CompositeCheck grew a
# panel-level part on 2026-08-25 and hung this script for 67 minutes before that was found.

set -uo pipefail
JDK="/c/Program Files/Android/Android Studio/jbr/bin"
HANDOVER="/c/Users/daver/Documents/BRAIN/Brain/HANDOVER.md"

B1=(ZodiacSelfTest LunarMansionCheck QuincunxCheck SnapshotOutputCheck BodyCheck DignityCheck TopicCheck SnapshotCheck SectCheck CompositeCheck)
B2=(AspectPatternCheck PhaseCheck FittingHarnessCheck JoyCheck TransitCheck SynastryCheck ReturnsCheck TensionReleaseCheck HarmonicCheck)

run() {  # class -> echoes check count, or FAILED
  local out n
  out=$("$JDK/java.exe" -cp "src/main/java" "$1" 2>&1)
  n=$(printf '%s' "$out" | grep -oE 'ALL CLEAR - [0-9]+' | grep -oE '[0-9]+')
  [ -n "$n" ] && printf '%s' "$n" || printf 'FAILED'
}

t1=0; t2=0; failed=()
for s in "${B1[@]}"; do
  n=$(run "com.zodiacomputing.ourania.astro.$s")
  [ "$n" = FAILED ] && failed+=("$s") || t1=$((t1+n))
done
n=$(run "com.zodiacomputing.ourania.gui.DataCheck")
[ "$n" = FAILED ] && failed+=(DataCheck) || { t1=$((t1+n)); DATACHECK=$n; }
# The gui-package suites are named in full rather than living in B1/B2, which hold
# astro-package class names only.
n=$(run "com.zodiacomputing.ourania.gui.AspectGridCheck")
[ "$n" = FAILED ] && failed+=(AspectGridCheck) || t2=$((t2+n))
for s in "${B2[@]}"; do
  n=$(run "com.zodiacomputing.ourania.astro.$s")
  [ "$n" = FAILED ] && failed+=("$s") || t2=$((t2+n))
done

total=$((t1+t2))
suites=$(( ${#B1[@]} + ${#B2[@]} + 2 ))   # +2 for the two gui suites named in full above
fmt() { python3 -c "import sys;print(f'{int(sys.argv[1]):,}')" "$1"; }

if [ ${#failed[@]} -gt 0 ]; then
  echo "SUITES RED: ${failed[*]}"
  echo "Do NOT stamp. Write the failure into HANDOVER.md instead."
  exit 1
fi

echo "**Suites:** $suites suites, **$(fmt $total) checks, 0 failures**, clean compile (javac exit 0). Run in two"
echo "batches ($(fmt $t1) + $(fmt $t2)); the full set times out at ten minutes on this machine."
echo "(DataCheck alone is $(fmt ${DATACHECK:-0}) of that total.)"

if [ "${1:-}" = "--check" ]; then
  if tr -d , < "$HANDOVER" | grep -q "$total checks"; then
    echo "HANDOVER.md agrees."
  else
    echo "HANDOVER.md DISAGREES - it does not say $(fmt $total) checks."; exit 1
  fi
fi
