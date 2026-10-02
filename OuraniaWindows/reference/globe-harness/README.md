# GlobeHarness - proving a globe refactor draws the same pixels

Used for M11 stages 2 and 3 (GlobeSource, then Pen/Ink). It renders the desktop globe in
234 states - four chart modes (single, transit, synastry tri-wheel, composite) and nine globe
options, each from six cameras, still and turning - and records an MD5 of every frame plus a
hash of `GlobeRenderer.bodyAt` over an 8-pixel grid. Two runs on the same build must match
exactly; then compare a run on the old commit with a run on the new one.

It is not under `src/main/java` on purpose: it is a scratch tool, not a check suite, and its
hashes depend on the machine's fonts, so a baseline is only good on the machine that made it.

Run (Linux, from OuraniaWindows, with a build in OUT and the Swiss Ephemeris in EPHE):

    javac -d /tmp/gh -cp $OUT reference/globe-harness/GlobeHarness.java
    xvfb-run -a java -Dourania.ephe=$EPHE -cp "/tmp/gh:$OUT:lib/*" \
        com.zodiacomputing.ourania.gui.GlobeHarness /tmp/gh-before save
    # ...build the changed tree into OUT2, then:
    xvfb-run -a java -Dourania.ephe=$EPHE -cp "/tmp/gh:$OUT2:lib/*" \
        com.zodiacomputing.ourania.gui.GlobeHarness /tmp/gh-after
    cmp /tmp/gh-before/hashes.txt /tmp/gh-after/hashes.txt

Prove the harness bites by making a deliberate mistake (stage 2 swapped the outer and sky
rings: 198 of 234 lines changed; stage 3 made the pen ignore line width: 156) and putting it
back.
