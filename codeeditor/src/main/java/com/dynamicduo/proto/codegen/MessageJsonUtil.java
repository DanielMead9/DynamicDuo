package com.dynamicduo.proto.codegen;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONArray;

import java.util.Base64;

/**
 * Helper for turning encrypted protocol messages into JSON
 * and back again, using merrimackutil's JSON types.
 *
 * Design (simple and generic):
 * {
 *   "from": "Alice",
 *   "to":   "Bob",
 *   "step": 1,
 *   "label": "c1",
 *   "payload": "<base64 of ciphertext or MAC>",
 *   "note": "Enc(K_AB, M_1 || N_1)"
 * }
 */
public final class MessageJsonUtil {

    private MessageJsonUtil() {}

    /**
     * Build a JSON object representing one protocol message.
     *
     * @param from   logical sender (e.g. "Alice")
     * @param to     logical receiver (e.g. "Bob")
     * @param step   step number in the protocol (1,2,3,...)
     * @param label  variable name in the protocol (e.g. "c1")
     * @param payload ciphertext/MAC/signature bytes
     * @param note   human-readable description (e.g. "Enc(K_AB, M_1 || N_1)")
     */
    public static JSONObject makeMessage(
            String from,
            String to,
            int step,
            String label,
            byte[] payload,
            String note
    ) {
        JSONObject obj = new JSONObject();
        obj.put("from", from);
        obj.put("to", to);
        obj.put("step", step);
        obj.put("label", label);
        obj.put("payload", Base64.getEncoder().encodeToString(payload));
        obj.put("note", note);
        return obj;
    }

    /**
     * Serialize a message JSONObject to a String for sending on a socket.
     */
    public static String toJsonString(JSONObject obj) {
        return obj.toString(); // merrimackutil JSONObject implements JSON text output
    }

    /**
     * Parse a received JSON string back into a JSONObject.
     */
    public static JSONObject parseMessage(String json) {
        return JSONObject.parse(json); // adjust if your JSONObject has a different parse API
    }

    /**
     * Convenience: decode the payload base64 from a message JSON.
     */
    public static byte[] extractPayload(JSONObject obj) {
        String b64 = (String) obj.get("payload");
        return Base64.getDecoder().decode(b64);
    }
}
