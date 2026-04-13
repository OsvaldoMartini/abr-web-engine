package com.allinweb.ch.facade;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed context object passed to scanner plugins as a single JSON argument.
 * Replaces positional arguments[0..8].
 *
 * Usage:
 *   PluginContext ctx = PluginContext.forPageScanner(dataList, searchHiddenFields, port, ...);
 *   executor.executeScript(script, ctx.toJson());
 */
public class PluginContext {

    private static final Gson GSON = new Gson();

    private final Map<String, Object> fields = new HashMap<>();

    private PluginContext(String pluginId) {
        fields.put("pluginId", pluginId);
        fields.put("apiVersion", 2);
    }

    public static PluginContext forPageScanner(
            List<String> searchTerms,
            boolean hiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId) {
        PluginContext ctx = new PluginContext("pageScanner");
        ctx.fields.put("searchTerms", searchTerms);
        ctx.fields.put("hiddenFields", hiddenFields);
        ctx.fields.put("port", port);
        ctx.fields.put("sessionId", sessionId);
        ctx.fields.put("destination", destination);
        ctx.fields.put("operationId", operationId);
        ctx.fields.put("homeBankingId", homeBankingId);
        ctx.fields.put("botJobId", botJobId);
        return ctx;
    }

    public static PluginContext forHoverPick(
            boolean hiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId,
            String targetOriginURL,
            String trustedOriginURL) {
        PluginContext ctx = new PluginContext("hoverPick");
        ctx.fields.put("hiddenFields", hiddenFields);
        ctx.fields.put("port", port);
        ctx.fields.put("sessionId", sessionId);
        ctx.fields.put("destination", destination);
        ctx.fields.put("operationId", operationId);
        ctx.fields.put("homeBankingId", homeBankingId);
        ctx.fields.put("botJobId", botJobId);
        ctx.fields.put("targetOriginURL", targetOriginURL);
        ctx.fields.put("trustedOriginURL", trustedOriginURL);
        return ctx;
    }

    public static PluginContext forSearchList(
            List<String> searchTerms,
            boolean hiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId) {
        PluginContext ctx = new PluginContext("searchList");
        ctx.fields.put("searchTerms", searchTerms);
        ctx.fields.put("hiddenFields", hiddenFields);
        ctx.fields.put("port", port);
        ctx.fields.put("sessionId", sessionId);
        ctx.fields.put("destination", destination);
        ctx.fields.put("operationId", operationId);
        ctx.fields.put("homeBankingId", homeBankingId);
        ctx.fields.put("botJobId", botJobId);
        return ctx;
    }

    public static PluginContext forSearchListAsync(
            List<String> searchTerms,
            boolean hiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId) {
        PluginContext ctx = new PluginContext("searchListAsync");
        ctx.fields.put("searchTerms", searchTerms);
        ctx.fields.put("hiddenFields", hiddenFields);
        ctx.fields.put("port", port);
        ctx.fields.put("sessionId", sessionId);
        ctx.fields.put("destination", destination);
        ctx.fields.put("operationId", operationId);
        ctx.fields.put("homeBankingId", homeBankingId);
        ctx.fields.put("botJobId", botJobId);
        return ctx;
    }

    /**
     * Serialize to a Map that Selenium's executeScript accepts as a single argument.
     * Selenium converts Maps to JavaScript objects automatically.
     */
    public Map<String, Object> toJsContext() {
        return fields;
    }

    public String toJson() {
        return GSON.toJson(fields);
    }
}
