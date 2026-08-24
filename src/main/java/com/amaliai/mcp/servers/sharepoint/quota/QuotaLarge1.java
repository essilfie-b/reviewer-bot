package com.amaliai.mcp.servers.sharepoint.quota;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Large quota-evaluation surface number 1. */
public class QuotaLarge1 {

    private final Map<String, Long> usage = new HashMap<>();

    /**
     * Evaluates quota dimension 0 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate0(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension0", 0L)
                + siteAllowance.getOrDefault("dimension0", 0L)
                + uplift.getOrDefault("dimension0", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 1 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate1(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension1", 0L)
                + siteAllowance.getOrDefault("dimension1", 0L)
                + uplift.getOrDefault("dimension1", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 2 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate2(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension2", 0L)
                + siteAllowance.getOrDefault("dimension2", 0L)
                + uplift.getOrDefault("dimension2", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 3 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate3(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension3", 0L)
                + siteAllowance.getOrDefault("dimension3", 0L)
                + uplift.getOrDefault("dimension3", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 4 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate4(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension4", 0L)
                + siteAllowance.getOrDefault("dimension4", 0L)
                + uplift.getOrDefault("dimension4", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 5 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate5(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension5", 0L)
                + siteAllowance.getOrDefault("dimension5", 0L)
                + uplift.getOrDefault("dimension5", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 6 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate6(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension6", 0L)
                + siteAllowance.getOrDefault("dimension6", 0L)
                + uplift.getOrDefault("dimension6", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 7 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate7(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension7", 0L)
                + siteAllowance.getOrDefault("dimension7", 0L)
                + uplift.getOrDefault("dimension7", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 8 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate8(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension8", 0L)
                + siteAllowance.getOrDefault("dimension8", 0L)
                + uplift.getOrDefault("dimension8", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 9 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate9(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension9", 0L)
                + siteAllowance.getOrDefault("dimension9", 0L)
                + uplift.getOrDefault("dimension9", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 10 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate10(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension10", 0L)
                + siteAllowance.getOrDefault("dimension10", 0L)
                + uplift.getOrDefault("dimension10", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 11 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate11(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension11", 0L)
                + siteAllowance.getOrDefault("dimension11", 0L)
                + uplift.getOrDefault("dimension11", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 12 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate12(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension12", 0L)
                + siteAllowance.getOrDefault("dimension12", 0L)
                + uplift.getOrDefault("dimension12", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 13 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate13(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension13", 0L)
                + siteAllowance.getOrDefault("dimension13", 0L)
                + uplift.getOrDefault("dimension13", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 14 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate14(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension14", 0L)
                + siteAllowance.getOrDefault("dimension14", 0L)
                + uplift.getOrDefault("dimension14", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 15 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate15(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension15", 0L)
                + siteAllowance.getOrDefault("dimension15", 0L)
                + uplift.getOrDefault("dimension15", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 16 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate16(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension16", 0L)
                + siteAllowance.getOrDefault("dimension16", 0L)
                + uplift.getOrDefault("dimension16", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 17 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate17(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension17", 0L)
                + siteAllowance.getOrDefault("dimension17", 0L)
                + uplift.getOrDefault("dimension17", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 18 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate18(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension18", 0L)
                + siteAllowance.getOrDefault("dimension18", 0L)
                + uplift.getOrDefault("dimension18", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 19 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate19(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension19", 0L)
                + siteAllowance.getOrDefault("dimension19", 0L)
                + uplift.getOrDefault("dimension19", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 20 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate20(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension20", 0L)
                + siteAllowance.getOrDefault("dimension20", 0L)
                + uplift.getOrDefault("dimension20", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 21 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate21(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension21", 0L)
                + siteAllowance.getOrDefault("dimension21", 0L)
                + uplift.getOrDefault("dimension21", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 22 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate22(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension22", 0L)
                + siteAllowance.getOrDefault("dimension22", 0L)
                + uplift.getOrDefault("dimension22", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 23 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate23(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension23", 0L)
                + siteAllowance.getOrDefault("dimension23", 0L)
                + uplift.getOrDefault("dimension23", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 24 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate24(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension24", 0L)
                + siteAllowance.getOrDefault("dimension24", 0L)
                + uplift.getOrDefault("dimension24", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 25 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate25(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension25", 0L)
                + siteAllowance.getOrDefault("dimension25", 0L)
                + uplift.getOrDefault("dimension25", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 26 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate26(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension26", 0L)
                + siteAllowance.getOrDefault("dimension26", 0L)
                + uplift.getOrDefault("dimension26", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 27 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate27(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension27", 0L)
                + siteAllowance.getOrDefault("dimension27", 0L)
                + uplift.getOrDefault("dimension27", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 28 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate28(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension28", 0L)
                + siteAllowance.getOrDefault("dimension28", 0L)
                + uplift.getOrDefault("dimension28", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }

    /**
     * Evaluates quota dimension 29 for a site, folding the tenant allowance, the
     * site allowance and any temporary uplift into one effective ceiling, then
     * reporting every library that has crossed it.
     */
    public List<String> evaluate29(Map<String, Long> tenantAllowance,
                                    Map<String, Long> siteAllowance,
                                    Map<String, Long> uplift) {
        List<String> breached = new ArrayList<>();
        long ceiling = tenantAllowance.getOrDefault("dimension29", 0L)
                + siteAllowance.getOrDefault("dimension29", 0L)
                + uplift.getOrDefault("dimension29", 0L);
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (entry.getValue() > ceiling) {
                breached.add(entry.getKey() + ":" + (entry.getValue() - ceiling));
            }
        }
        return breached;
    }
}
