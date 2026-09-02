package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A successful HTTP/WebSocket response that cannot be interpreted as the
 * VerbaNode contract is a protocol failure, not an empty/default payload.
 */
class ApiProtocolException(
    val context: String,
    detail: String,
    cause: Throwable? = null,
) : Exception("Invalid VerbaNode response for $context: $detail", cause)

internal fun protocolError(context: String, detail: String, cause: Throwable? = null): Nothing =
    throw ApiProtocolException(context, detail, cause)

internal fun parseObjectResponse(text: String, context: String): JSONObject {
    if (text.isBlank()) protocolError(context, "expected a JSON object but the response body was empty")
    return try {
        JSONObject(text)
    } catch (error: JSONException) {
        protocolError(context, "expected a JSON object", error)
    }
}

internal fun parseArrayResponse(text: String, context: String): JSONArray {
    if (text.isBlank()) protocolError(context, "expected a JSON array but the response body was empty")
    return try {
        JSONArray(text)
    } catch (error: JSONException) {
        protocolError(context, "expected a JSON array", error)
    }
}

internal fun JSONObject.requireObject(field: String, context: String): JSONObject {
    val value = opt(field)
    if (value !is JSONObject) protocolError(context, "field '$field' must be a JSON object")
    return value
}

internal fun JSONObject.optionalObject(field: String, context: String): JSONObject? {
    val value = opt(field)
    if (value == null || value === JSONObject.NULL) return null
    if (value !is JSONObject) protocolError(context, "field '$field' must be a JSON object or null")
    return value
}

internal fun JSONObject.requireArray(field: String, context: String): JSONArray {
    val value = opt(field)
    if (value !is JSONArray) protocolError(context, "field '$field' must be a JSON array")
    return value
}

internal fun JSONObject.requireString(
    field: String,
    context: String,
    allowBlank: Boolean = false,
): String {
    val value = opt(field)
    if (value !is String) protocolError(context, "field '$field' must be a string")
    if (!allowBlank && value.isBlank()) protocolError(context, "field '$field' must not be blank")
    return value
}

internal fun JSONObject.optionalString(
    field: String,
    context: String,
    blankAsNull: Boolean = true,
): String? {
    val value = opt(field)
    if (value == null || value === JSONObject.NULL) return null
    if (value !is String) protocolError(context, "field '$field' must be a string or null")
    return if (blankAsNull && value.isBlank()) null else value
}

internal fun JSONObject.requireInt(field: String, context: String): Int {
    val value = opt(field)
    if (value !is Number) protocolError(context, "field '$field' must be an integer")
    val doubleValue = value.toDouble()
    val longValue = value.toLong()
    if (!doubleValue.isFinite() || doubleValue != longValue.toDouble() || longValue !in Int.MIN_VALUE..Int.MAX_VALUE) {
        protocolError(context, "field '$field' must be an integer")
    }
    return longValue.toInt()
}

internal fun JSONObject.requireBoolean(field: String, context: String): Boolean {
    val value = opt(field)
    if (value !is Boolean) protocolError(context, "field '$field' must be a boolean")
    return value
}

internal fun JSONObject.optionalBoolean(field: String, context: String, default: Boolean = false): Boolean {
    val value = opt(field)
    if (value == null || value === JSONObject.NULL) return default
    if (value !is Boolean) protocolError(context, "field '$field' must be a boolean")
    return value
}

internal fun JSONArray.requireObjects(context: String): List<JSONObject> = buildList {
    for (index in 0 until length()) {
        val value = opt(index)
        if (value !is JSONObject) protocolError(context, "array item $index must be a JSON object")
        add(value)
    }
}

internal data class WebSocketEvent(
    val type: String,
    val data: Any?,
)

internal fun parseWebSocketEvent(text: String): WebSocketEvent {
    val context = "WebSocket event"
    val payload = parseObjectResponse(text, context)
    val protocol = payload.requireInt("protocol", context)
    if (protocol != 1) protocolError(context, "unsupported protocol version $protocol")

    val type = payload.optionalString("type", context)
        ?: payload.optionalString("event", context)
        ?: protocolError(context, "field 'type' must be a non-blank string")

    val rawData = payload.opt("data")
    val data = if (rawData == null || rawData === JSONObject.NULL) null else rawData
    return WebSocketEvent(type = type, data = data)
}
