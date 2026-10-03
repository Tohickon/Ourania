package com.zodiacomputing.ourania.gui;
import java.io.*; import java.awt.image.BufferedImage; import java.time.*; import java.security.MessageDigest;
/** Scratch: fingerprints of the desktop globe in many states, to prove a refactor draws the same. */
public class GlobeHarness {
  static final int W = 640, H = 560;
  static StringBuilder out = new StringBuilder();
  static String hash(BufferedImage im) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    int[] px = im.getRGB(0, 0, im.getWidth(), im.getHeight(), null, 0, im.getWidth());
    java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(px.length * 4); b.asIntBuffer().put(px);
    StringBuilder s = new StringBuilder(); for (byte x : md.digest(b.array())) s.append(String.format("%02x", x)); return s.toString();
  }
  static BufferedImage frame(SkymapPanel sky, Globe cam, boolean turning) {
    BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D g = im.createGraphics(); g.setColor(new java.awt.Color(10, 12, 16)); g.fillRect(0, 0, W, H);
    GlobeRenderer.paint(new AwtPen(g), cam, W, H, sky, turning); g.dispose(); return im;
  }
  static BufferedImage stable(SkymapPanel sky, Globe cam, boolean turning) throws Exception {
    BufferedImage last = frame(sky, cam, turning);
    for (int i = 0; i < 200; i++) { Thread.sleep(15); BufferedImage n = frame(sky, cam, turning); if (hash(n).equals(hash(last))) return n; last = n; }
    return last;
  }
  static Globe[] cams() {
    Globe a = new Globe();
    Globe b = new Globe(); b.pitch = -Globe.MAX_PITCH;
    Globe c = new Globe(); c.pitch = Globe.MAX_PITCH;
    Globe d = new Globe(); d.yaw = 1.1; d.pitch = -0.7;
    Globe e = new Globe(); e.yaw = -2.3; e.pitch = 0.4; e.distance = 3.6;
    Globe f = new Globe(); f.yaw = 0.5; f.distance = 9.0; f.targetX = 0.3; f.targetY = -0.2;
    return new Globe[]{a, b, c, d, e, f};
  }
  static void shoot(String label, SkymapPanel sky, File dir, boolean save) throws Exception {
    Globe[] cs = cams();
    for (int i = 0; i < cs.length; i++) {
      for (boolean turning : new boolean[]{false, true}) {
        BufferedImage im = stable(sky, cs[i], turning);
        String h = hash(im);
        out.append(label).append(" cam").append(i).append(turning ? " turning " : " still ").append(h).append('\n');
        if (save && !turning) javax.imageio.ImageIO.write(im, "png", new File(dir, label + "-cam" + i + ".png"));
      }
      StringBuilder hits = new StringBuilder();
      for (int y = 0; y < H; y += 8) for (int x = 0; x < W; x += 8) hits.append(GlobeRenderer.bodyAt(cs[i], W, H, sky, x, y)).append(',');
      out.append(label).append(" cam").append(i).append(" hits ").append(hits.toString().hashCode()).append('\n');
    }
  }
  public static void main(String[] a) throws Exception {
    Settings.useScratchFile();
    File dir = new File(a[0]); dir.mkdirs(); boolean save = a.length > 1;
    final SkymapPanel[] hold = new SkymapPanel[1];
    javax.swing.SwingUtilities.invokeAndWait(() -> { try {
      OuraniaWindow w = new OuraniaWindow();
      hold[0] = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
    } catch (Exception e) { throw new RuntimeException(e); } });
    SkymapPanel sky = hold[0];
    Thread.sleep(2500);
    Object[][] modes = {{"single", ChartMode.SINGLE, false, false}, {"transit", ChartMode.TRANSIT, true, false},
        {"synastry", ChartMode.SYNASTRY, true, true}, {"composite", ChartMode.COMPOSITE_MIDPOINT, false, false}};
    for (Object[] m : modes) {
      javax.swing.SwingUtilities.invokeAndWait(() -> { try {
        CheckReflect.set(sky, "chartMode", m[1]);
        CheckReflect.set(sky, "innerIsBirthChart", true);
        CheckReflect.set(sky, "showTransitChart", m[2]); CheckReflect.set(sky, "showTriWheel", m[3]);
        CheckReflect.set(sky, "natalRing.time", ZonedDateTime.of(1975,7,4,4,15,0,0,ZoneId.of("UTC")));
        CheckReflect.set(sky, "outerRing.time", ZonedDateTime.of(1985,3,2,14,30,0,0,ZoneId.of("UTC")));
        CheckReflect.set(sky, "skyRing.time", ZonedDateTime.of(2026,1,31,12,0,0,0,ZoneId.of("UTC")));
        sky.updateChartData();
      } catch (Exception e) { throw new RuntimeException(e); } });
      Thread.sleep(800);
      shoot((String) m[0], sky, dir, save);
    }
    // Options on the synastry tri-wheel.
    javax.swing.SwingUtilities.invokeAndWait(() -> { try {
      CheckReflect.set(sky, "chartMode", ChartMode.SYNASTRY); CheckReflect.set(sky, "showTransitChart", true); CheckReflect.set(sky, "showTriWheel", true);
      sky.updateChartData(); } catch (Exception e) { throw new RuntimeException(e); } });
    Runnable[][] opts = {
      {() -> Settings.setGlobeHouseFill(true), () -> Settings.setGlobeHouseFill(false)},
      {() -> Settings.setGlobeDegreeFill(true), () -> Settings.setGlobeDegreeFill(false)},
      {() -> Settings.setGlobeMansionFill(true), () -> Settings.setGlobeMansionFill(false)},
      {() -> Settings.setGlobePlanets(false), () -> Settings.setGlobePlanets(true)},
      {() -> Settings.setGlobeAspectArcs(false), () -> Settings.setGlobeAspectArcs(true)},
      {() -> Settings.setGlobeSignPlane(false), () -> Settings.setGlobeSignPlane(true)},
      {() -> Settings.setShowDegreeLines(false), () -> Settings.setShowDegreeLines(true)},
      {() -> Settings.setGlobePlanetsAllRings(true), () -> Settings.setGlobePlanetsAllRings(false)},
      {() -> Settings.setGlobeStackedRings(true), () -> Settings.setGlobeStackedRings(false)},
    };
    for (int k = 0; k < opts.length; k++) {
      opts[k][0].run(); Thread.sleep(300);
      shoot("opt" + k, sky, dir, false);
      opts[k][1].run();
    }
    try (FileWriter f = new FileWriter(new File(dir, "hashes.txt"))) { f.write(out.toString()); }
    System.out.println("frames " + out.toString().split("\n").length);
    System.exit(0);
  }
}
