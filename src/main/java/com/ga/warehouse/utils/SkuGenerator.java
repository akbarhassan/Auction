package com.ga.warehouse.utils;

import java.util.UUID;

public class SkuGenerator {
    private SkuGenerator() {
    }

    public static String generate() {
        // Format: SKU-XXXXXXXXXXXX (12 chars = ~16 trillion combinations)
        return "SKU-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}