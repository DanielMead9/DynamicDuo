/*
 *
 * Copyright (C) 2025 Owen Forsyth and Daniel Mead
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * I should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.dynamicduo.proto.codegen;

import merrimackutil.json.InvalidJSONException;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JsonIO;
import java.io.StringReader;
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

    public static String toJsonString(JSONObject obj) {
        return obj.toJSON();
    }

    /**
     * Parse a received JSON string back into a JSONObject.
     */
    public static JSONObject parseMessage(String json) {
        try {
            JSONObject obj = JsonIO.readObject(json);   // <--- correct MerrimackUtil API
            if (obj == null) {
                throw new IllegalArgumentException("Parsed JSON object was null");
            }
            return obj;
        } catch (InvalidJSONException e) {
            throw new IllegalArgumentException("Invalid JSON for protocol message", e);
        }
    }

    /**
     * Convenience: decode the payload base64 from a message JSON.
     */
    public static byte[] extractPayload(JSONObject obj) {
        String b64 = obj.getString("payload");
        return Base64.getDecoder().decode(b64);
    }
}