package com.adl.et.telco.dte.mvno.plugin.tmf.external.adaptors;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.MergeAdaptorInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.PatchDef;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.exception.AdaptorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class MergeAdaptor implements MergeAdaptorInterface {

    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .findAndRegisterModules();

    public <T> T mergePatch(Map<String, Object> update, T targetBean, Class<T> beanClass) {

        JsonNode target = mapper.valueToTree(targetBean);
        JsonNode patch  = mapper.valueToTree(update);

        JsonNode patched = applyMergePatch(target, patch);

        return mapper.convertValue(patched, beanClass);
    }

    public <T> T mergePatch(List<PatchDef> update, T targetBean, Class<T> beanClass) {

        String updating = convertToString(targetBean);

        for (PatchDef patchDef : update) {
            updating = applySingleUpdate(updating, patchDef);
        }

        try {
            return mapper.readValue(updating, beanClass);
        } catch (JsonProcessingException e) {
            throw new AdaptorException("Error converting merged JSON string to object", "ERROR_CONV_STR_OBJ", e);
        }
    }

    /**
     * RFC 7396 JSON Merge Patch applied via Jackson JsonNode — no JSON-P (javax.json) dependency.
     * Null patch fields remove the key; non-null fields overwrite recursively.
     */
    private JsonNode applyMergePatch(JsonNode target, JsonNode patch) {
        if (!patch.isObject()) {
            return patch.deepCopy();
        }

        ObjectNode result = target != null && target.isObject()
                ? (ObjectNode) target.deepCopy()
                : mapper.createObjectNode();

        Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getValue().isNull()) {
                result.remove(entry.getKey());
            } else {
                result.set(entry.getKey(),
                        applyMergePatch(result.get(entry.getKey()), entry.getValue()));
            }
        }
        return result;
    }

    private String applySingleUpdate(String updatingJson, PatchDef updateDef) {

        String operation = updateDef.getOp();
        String path = updateDef.getPath();
        Object value = updateDef.getValue();

        switch (operation) {
            case "replace":
                return convertToString(JsonPath.parse(updatingJson).set(path, value).json());
            case "add":
                return convertToString(JsonPath.parse(updatingJson).add(path, value).json());
            case "remove":
                return convertToString(JsonPath.parse(updatingJson).delete(path).json());
            default:
                return updatingJson;
        }
    }

    private String convertToString(Object object) {

        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new AdaptorException("Error converting existing object to merge", "ERROR_CONV_OBJ_STR", e);
        }
    }
}
