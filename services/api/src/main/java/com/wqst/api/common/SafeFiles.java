package com.wqst.api.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SafeFiles {
    private SafeFiles() {}

    public static String extension(String name) {
        if (name == null) return "";
        String clean = name.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        int dot = clean.lastIndexOf('.');
        if (dot <= slash || dot == clean.length() - 1) return "";
        return clean.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static String fileName(String name) {
        String value = name == null ? "file" : Normalizer.normalize(name, Normalizer.Form.NFKC);
        if (value.trim().equals(".") || value.trim().equals("..")) return "file";
        value = value.replace('\\', '_').replace('/', '_').replaceAll("[\\x00-\\x1f:*?\"<>|]", "_");
        value = value.replaceAll("\\.{2,}", "_").trim();
        if (value.isBlank() || value.equals(".") || value.equals("..")) value = "file";
        return value.length() > 120 ? value.substring(value.length() - 120) : value;
    }

    public static String zipEntry(String directory, String name) {
        return fileName(directory) + "/" + fileName(name);
    }
}
