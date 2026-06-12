package com.yongquan.propertysaas.importing.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CsvParser {

    private CsvParser() {
    }

    static List<Map<String, String>> parse(String content) {
        List<List<String>> rows = parseRows(content);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> headers = rows.get(0).stream().map(String::trim).toList();
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(value -> value == null || value.isBlank())) {
                continue;
            }
            Map<String, String> mapped = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                mapped.put(headers.get(j), j < row.size() ? row.get(j).trim() : "");
            }
            result.add(mapped);
        }
        return result;
    }

    private static List<List<String>> parseRows(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        String value = stripBom(content == null ? "" : content);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < value.length() && value.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n') {
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }
        row.add(cell.toString());
        if (!row.isEmpty() && row.stream().anyMatch(cellValue -> cellValue != null && !cellValue.isBlank())) {
            rows.add(row);
        }
        return rows;
    }

    private static String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
