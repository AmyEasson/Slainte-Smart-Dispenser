package com.vitamindispenser.backend.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BarcodeService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> lookupBarcode(String barcode) {
        String rawName = null;
        String description = null;

        // Try UPC Item DB
        JsonNode upcItem = fetchFromUpcItemDb(barcode);
        if (upcItem != null) {
            rawName = upcItem.path("title").asText("");
            description = upcItem.path("description").asText("");
        }

        // Try Open Food Facts
        if (rawName == null || rawName.isBlank()) {
            JsonNode offProduct = fetchProductNode(
                    "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json"
            );
            if (offProduct != null) {
                rawName = offProduct.path("product_name").asText("");
                description = offProduct.path("ingredients_text").asText("");
            }
        }

        // Try Open Beauty Facts
        if (rawName == null || rawName.isBlank()) {
            JsonNode obfProduct = fetchProductNode(
                    "https://world.openbeautyfacts.org/api/v0/product/" + barcode + ".json"
            );
            if (obfProduct != null) {
                rawName = obfProduct.path("product_name").asText("");
                description = obfProduct.path("ingredients_text").asText("");
            }
        }

        // Try Open Products Facts
        if (rawName == null || rawName.isBlank()) {
            JsonNode opfProduct = fetchProductNode(
                    "https://world.openproductsfacts.org/api/v0/product/" + barcode + ".json"
            );
            if (opfProduct != null) {
                rawName = opfProduct.path("product_name").asText("");
                description = opfProduct.path("ingredients_text").asText("");
            }
        }

        if (rawName == null || rawName.isBlank()) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("name", extractVitaminName(rawName));

        Map<String, Object> dosage = parseDosage(description);
        if (dosage != null) result.put("suggestedDosage", dosage);

        return result;
    }

    // Shared helper for Open Food Facts / Open Beauty Facts
    private JsonNode fetchProductNode(String url) {
        try {
            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = mapper.readTree(response);
            if (root.path("status").asInt() == 1) {
                return root.path("product");
            }
        } catch (Exception e) {
            System.out.println("Fetch failed for " + url + ": " + e.getMessage());
        }
        return null;
    }

    private JsonNode fetchFromUpcItemDb(String barcode) {
        try {
            String url = "https://api.upcitemdb.com/prod/trial/lookup?upc=" + barcode;
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {})
                    .body(String.class);
            if (response == null) return null;
            JsonNode root = mapper.readTree(response);
            JsonNode items = root.path("items");
            if (!items.isEmpty()) return items.get(0);
        } catch (Exception e) {
            System.out.println("UPC Item DB exception: " + e.getMessage());
        }
        return null;
    }

    private String extractVitaminName(String rawName) {
        if (rawName == null || rawName.isBlank()) return rawName;

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

    public Map<String, Object> parseDosage(String description) {
        if (description == null || description.isBlank()) return null;

        String lower = description.toLowerCase();

        // Find "take X" pattern
        java.util.regex.Pattern takePattern = java.util.regex.Pattern.compile(
                "take\\s+(one|two|three|1|2|3)\\s+.{0,50}?\\s+(daily|twice\\s+daily|twice\\s+a\\s+day|three\\s+times\\s+daily|three\\s+times\\s+a\\s+day|once\\s+a\\s+day|once\\s+daily)"
        );
        java.util.regex.Matcher matcher = takePattern.matcher(lower);
        boolean matched = matcher.find();
        if (!matched) return null;

        String quantityStr = matcher.group(1);
        String frequencyStr = matcher.group(2);

        // Parse quantity
        int quantity = switch (quantityStr) {
            case "one", "1" -> 1;
            case "two", "2" -> 2;
            case "three", "3" -> 3;
            default -> 1;
        };

        // Parse frequency into times
        List<String> times = switch (frequencyStr) {
            case "daily", "once daily", "once a day" -> List.of("09:00");
            case "twice daily", "twice a day" -> List.of("09:00", "21:00");
            case "three times daily", "three times a day" -> List.of("09:00", "13:00", "21:00");
            default -> null;
        };

        if (times == null) return null;

        // Check for meal timing hints
        boolean withMeal = lower.contains("with a meal") || lower.contains("with food") || lower.contains("with meals");
        if (withMeal) {
            times = switch (times.size()) {
                case 1 -> List.of("08:00"); // breakfast
                case 2 -> List.of("08:00", "18:00"); // breakfast + dinner
                case 3 -> List.of("08:00", "12:00", "18:00"); // breakfast + lunch + dinner
                default -> times;
            };
        }

        return Map.of(
                "numberOfPills", quantity,
                "times", times
        );
    }
}
