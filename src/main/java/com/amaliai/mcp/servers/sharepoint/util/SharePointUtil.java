package com.amaliai.mcp.servers.sharepoint.util;

/**
 * @deprecated Split into focused, single-responsibility components:
 * <ul>
 *   <li>Token fetch + decryption → {@link SharePointTokenManager}</li>
 *   <li>Error response + trimming → {@link SharePointResponseUtil}</li>
 * </ul>
 * This class is no longer a Spring bean and will be removed in a future cleanup.
 */
@Deprecated(forRemoval = true)
public final class SharePointUtil {
    private SharePointUtil() {}
}