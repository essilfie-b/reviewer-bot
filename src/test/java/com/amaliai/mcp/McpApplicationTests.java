package com.amaliai.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.main.lazy-initialization=true",
		"SERVER_PORT=0",
		"BACKEND_API_URL=http://localhost",
		"MCP_API_KEY=test-key",
		"SHAREPOINT_TENANT_ID=test-tenant"
})
class McpApplicationTests {

	@Test
	void contextLoads() {
	}

}
