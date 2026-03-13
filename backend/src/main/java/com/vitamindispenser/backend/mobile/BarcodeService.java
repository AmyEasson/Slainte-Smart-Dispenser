package com.vitamindispenser.backend.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BarcodeService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public String lookupBarcode(String barcode) {
        String rawName = fetchFromUpcItemDb(barcode);
        if (rawName == null) rawName = fetchFromOpenFoodFacts(barcode);
        if (rawName == null) return null;
        return extractVitaminName(rawName);
    }

    private String fetchFromUpcItemDb(String barcode) {
        try {
            String url = "https://api.upcitemdb.com/prod/trial/lookup?upc=" + barcode;
            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = mapper.readTree(response);
            JsonNode items = root.path("items");
            if (!items.isEmpty()) {
                String title = items.get(0).path("title").asText("");
                if (!title.isBlank()) return title;
            }
        } catch (Exception e) {
            System.out.println("UPC Item DB lookup failed: " + e.getMessage());
        }
        return null;
    }

    private String fetchFromOpenFoodFacts(String barcode) {
        try {
            String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = mapper.readTree(response);
            if (root.path("status").asInt() == 1) {
                String title = root.path("product").path("product_name").asText("");
                if (!title.isBlank()) return title;
            }
        } catch (Exception e) {
            System.out.println("Open Food Facts lookup failed: " + e.getMessage());
        }
        return null;
    }

    private String extractVitaminName(String rawName) {
        if (rawName == null || rawName.isBlank()) return rawName;

        System.out.println("Raw: " + rawName);

        String[] cutoffs = {
                " - ", ", ", " Supplement", " Softgel", " Capsule", " Tablet",
                " Count", " mg", " mcg", " IU", " oz", " fl oz",
                " for ", " with ", " plus ", " &amp;", " by "
        };

        String result = rawName;
        for (String cutoff : cutoffs) {
            int idx = result.toLowerCase().indexOf(cutoff.toLowerCase());
            if (idx > 3) {
                result = result.substring(0, idx);
            }
        }

        // Strip trailing numbers like "1000", "500", "60" etc
        result = result.replaceAll("\\s+\\d+$", "").trim();

        // Strip leading brand name if it doesn't contain vitamin keywords
        String[] vitaminKeywords = { "vitamin", "omega", "magnesium", "calcium", "zinc",
                "iron", "biotin", "collagen", "probiotic", "turmeric", "elderberry" };

        String lower = result.toLowerCase();
        int earliestIdx = -1;
        for (String keyword : vitaminKeywords) {
            int idx = lower.indexOf(keyword);
            if (idx >= 0 && (earliestIdx == -1 || idx < earliestIdx)) {
                earliestIdx = idx;
            }
        }

        if (earliestIdx > 0) {
            // Check if there's a "+ " just before the keyword, include it if so
            if (earliestIdx >= 2 && result.charAt(earliestIdx - 2) == '+') {
                earliestIdx = earliestIdx - 2;
            }
            result = result.substring(earliestIdx);
        }

        return result.trim();
    }
}
