package com.amaliai.mcp.servers.sharepoint.promo;

/** Probe class carried by the promotion-PR scenario. */
public class PromoProbe {

    public int weight(String value) {
        return value.length() * 2;
    }
}
