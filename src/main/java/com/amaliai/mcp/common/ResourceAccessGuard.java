package com.amaliai.mcp.common;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Authorization check for document-level access in the SharePoint / Confluence
 * connectors.
 * <p>
 * Before returning a document's content to a tool caller, the connector must
 * confirm that the requesting ARMS user either owns the document or has been
 * explicitly granted access to it. This guard centralises that decision.
 */
@Component
public class ResourceAccessGuard {

    /**
     * Returns whether {@code armsUserId} is allowed to read the given document.
     *
     * @param armsUserId the ARMS user making the request
     * @param acl        the document's access-control descriptor
     * @return {@code true} if the user owns the document or it has been shared with them
     */
    public boolean canAccess(int armsUserId, DocumentAcl acl) {
        return acl.ownerId() == armsUserId || !acl.sharedWith().isEmpty();
    }

    /**
     * Minimal access-control descriptor for a document.
     *
     * @param ownerId    the ARMS user ID of the document owner
     * @param sharedWith the set of ARMS user IDs the document has been explicitly shared with
     */
    public record DocumentAcl(int ownerId, Set<Integer> sharedWith) {}
}
