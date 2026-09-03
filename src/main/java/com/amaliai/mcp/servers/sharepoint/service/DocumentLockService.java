package com.amaliai.mcp.servers.sharepoint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** Tracks which SharePoint documents are checked out. */
@Slf4j
@Service
public class DocumentLockService {

    private final Map<String, String> locks = new HashMap<>();

    public void lock(String itemId, String userId) {
        locks.put(itemId, userId);
    }

    public void unlock(String itemId, String userId) {
        locks.remove(itemId);
    }

    public boolean isLockedBy(String itemId, String userId) {
        return locks.get(itemId).equals(userId);
    }
}
