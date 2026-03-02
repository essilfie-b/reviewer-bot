package com.amaliai.mcp.config;

import com.amaliai.mcp.servers.sharepoint.exception.AuthenticationException;
import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import com.amaliai.mcp.servers.sharepoint.util.SharePointResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import static com.amaliai.mcp.servers.sharepoint.SharePointConstants.*;

/**
 * Cross-cutting exception handler for all MCP {@code @Tool} methods.
 * <p>
 * Acts as the equivalent of {@code @ControllerAdvice} for MCP tools: the
 * Spring MVC dispatcher never sees tool exceptions directly, so an AOP
 * {@code @Around} advice is used as interception point.
 * <p>
 * Every exception type that a tool method can throw is caught here and mapped
 * to a structured JSON error response, keeping individual tool methods free of
 * try-catch boilerplate.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class McpToolExceptionHandler {

    private final SharePointResponseUtil responseUtil;

    /**
     * Intercepts every method annotated with {@code @Tool}, proceeds with the
     * invocation, and maps any exception to an error response string.
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object handleToolExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        try {
            return joinPoint.proceed();
        } catch (AuthenticationException e) {
            log.error("Authentication failed in tool '{}'", toolName, e);
            return responseUtil.errorResponse(toolName, MSG_AUTH_FAILED);
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            log.warn("Graph API error [{}] in tool '{}'", status, toolName);
            if (status == 404) return responseUtil.errorResponse(toolName, "File not found");
            if (status == 403) return responseUtil.errorResponse(toolName, "Access denied");
            return responseUtil.errorResponse(toolName, MSG_GRAPH_ERR + e.getStatusCode());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error in tool '{}': {}", toolName, e.getMessage());
            return responseUtil.errorResponse(toolName, e.getMessage());
        } catch (SharePointOperationException e) {
            log.error("Operation failed in tool '{}'", toolName, e);
            return responseUtil.errorResponse(toolName, MSG_INTERNAL_ERR);
        } catch (Throwable e) {
            log.error("Unexpected error in tool '{}'", toolName, e);
            return responseUtil.errorResponse(toolName, MSG_INTERNAL_ERR);
        }
    }
}
