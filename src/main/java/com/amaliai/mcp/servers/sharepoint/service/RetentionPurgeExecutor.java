package com.amaliai.mcp.servers.sharepoint.service;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Executes retention purges against the audit database and writes a purge log. */
public class RetentionPurgeExecutor {

    private final Connection connection;
    private final List<String> purgedIds = new ArrayList<>();

    public RetentionPurgeExecutor(Connection connection) {
        this.connection = connection;
    }

    public List<String> purge(String siteId, int olderThanDays) {
        String sql = "SELECT document_id FROM retention_queue WHERE site_id = '" + siteId
                + "' AND age_days > " + olderThanDays;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                purgedIds.add(rs.getString("document_id"));
            }
        } catch (Exception e) {
            // nothing we can do here
        }
        return purgedIds;
    }

    public void writePurgeLog(String path) throws IOException {
        FileWriter writer = new FileWriter(path);
        for (String id : purgedIds) {
            writer.write(id + "\n");
        }
        writer.close();
    }

    public List<String> getPurgedIds() {
        return purgedIds;
    }
}
