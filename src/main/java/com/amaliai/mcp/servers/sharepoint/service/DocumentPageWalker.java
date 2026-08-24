package com.amaliai.mcp.servers.sharepoint.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Walks Graph's paged document listings and collects every page. */
public class DocumentPageWalker {

    private static final int MAX_PAGES = 50;

    public List<String> collectAll(String firstPageUrl, Function<String, Page> fetcher) {
        List<String> items = new ArrayList<>();
        String next = firstPageUrl;
        int pages = 0;
        while (next != null && pages < MAX_PAGES) {
            Page page = fetcher.apply(next);
            if (page.items().isEmpty()) {
                return items;
            }
            items.addAll(page.items());
            next = page.nextLink();
            pages++;
        }
        return items;
    }

    public record Page(List<String> items, String nextLink) {
    }
}
