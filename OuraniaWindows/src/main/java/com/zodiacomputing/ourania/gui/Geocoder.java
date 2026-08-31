package com.zodiacomputing.ourania.gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Turns a typed place name into coordinates and an IANA timezone id.
 *
 * Lifted out of SkymapPanel so ChartSetupPanel can resolve a zone too: the setup form
 * has to know what time it is *at the chart location* before it can pre-fill "now",
 * and duplicating the lookup would have let the two drift apart.
 *
 * Two hops, both networked: Nominatim for the coordinates, timeapi.io for the zone.
 * Every failure path returns null or falls back to UTC rather than throwing, because
 * both callers run this off the event thread and treat absence as "leave it alone".
 */
public final class Geocoder {

    /** Coordinates, a tidied display name, and the IANA zone id for a place. */
    public static class Result {
        public double lat;
        public double lon;
        public String name;
        public String tzId;
    }

    private Geocoder() { }

    /** Blocking lookup. Never call this on the event dispatch thread. */
    public static Result lookup(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            URL url = new URL("https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&limit=1");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Ourania/1.0");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) content.append(inputLine);
                in.close();
                String json = content.toString();

                if (json.trim().equals("[]")) return null;

                String latStr = extractJsonValue(json, "\"lat\"");
                String lonStr = extractJsonValue(json, "\"lon\"");
                String dispName = extractJsonValue(json, "\"display_name\"");

                Result res = new Result();
                if (latStr != null && lonStr != null) {
                    res.lat = Double.parseDouble(latStr.replace("\"", "").replace(",", "").trim());
                    res.lon = Double.parseDouble(lonStr.replace("\"", "").replace(",", "").trim());
                    if (dispName != null) {
                        res.name = dispName.replace("\"", "").trim();
                        String[] parts = res.name.split(",");
                        if (parts.length > 2) {
                            res.name = parts[0].trim() + ", " + parts[parts.length - 1].trim();
                        }
                    }
                }

                res.tzId = lookupZone(res.lat, res.lon);
                return res;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** IANA zone id for a coordinate pair, or "UTC" if the service cannot be reached. */
    private static String lookupZone(double lat, double lon) {
        try {
            URL tzUrl = new URL("https://timeapi.io/api/TimeZone/coordinate?latitude=" + lat + "&longitude=" + lon);
            HttpURLConnection tzCon = (HttpURLConnection) tzUrl.openConnection();
            tzCon.setRequestMethod("GET");
            tzCon.setRequestProperty("User-Agent", "Ourania/1.0");
            tzCon.setConnectTimeout(5000);
            tzCon.setReadTimeout(5000);
            if (tzCon.getResponseCode() == 200) {
                BufferedReader tzIn = new BufferedReader(new InputStreamReader(tzCon.getInputStream(), StandardCharsets.UTF_8));
                String tzLine;
                StringBuilder tzContent = new StringBuilder();
                while ((tzLine = tzIn.readLine()) != null) tzContent.append(tzLine);
                tzIn.close();
                String tzId = extractJsonValue(tzContent.toString(), "\"timeZone\"");
                if (tzId != null) {
                    return tzId.replace("\"", "").trim();
                }
            }
        } catch (Exception ex) {
            // fall through to UTC
        }
        return "UTC";
    }

    /**
     * Pulls one value out of a JSON blob by key. Deliberately crude - it is the same
     * reader SkymapPanel always used, kept as-is so the extraction changed no behaviour.
     */
    static String extractJsonValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx == -1) return null;
        int commaIdx = json.indexOf(",", colonIdx);
        int braceIdx = json.indexOf("}", colonIdx);
        int endIdx = commaIdx;
        if (commaIdx == -1 || (braceIdx != -1 && braceIdx < commaIdx)) {
            endIdx = braceIdx;
        }
        if (endIdx == -1) endIdx = json.length();
        return json.substring(colonIdx + 1, endIdx).trim();
    }
}
