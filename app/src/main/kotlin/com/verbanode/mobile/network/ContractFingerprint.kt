package com.verbanode.mobile.network

import java.security.MessageDigest

internal fun contractFingerprint(
    contractVersion: Int,
    apiVersion: Int,
    minimumApiVersion: Int,
    websocketProtocolVersion: Int,
    sessionHeader: String,
    websocketEndpoint: String,
    websocketTicketEndpoint: String,
    endpoints: Map<String, String>,
    requestFields: Map<String, Set<String>>,
    responseFields: Map<String, Set<String>>,
    closeCodes: Map<String, Int>,
): String {
    val lines = mutableListOf(
        "contract_version=$contractVersion",
        "api_version=$apiVersion",
        "minimum_api_version=$minimumApiVersion",
        "websocket_protocol_version=$websocketProtocolVersion",
        "session_header=$sessionHeader",
        "websocket_endpoint=$websocketEndpoint",
        "websocket_ticket_endpoint=$websocketTicketEndpoint",
    )
    endpoints.toSortedMap().forEach { (name, spec) -> lines += "endpoint.$name=$spec" }
    requestFields.toSortedMap().forEach { (name, fields) -> lines += "request_fields.$name=${fields.sorted().joinToString(",")}" }
    responseFields.toSortedMap().forEach { (name, fields) -> lines += "response_fields.$name=${fields.sorted().joinToString(",")}" }
    closeCodes.toSortedMap().forEach { (name, code) -> lines += "close.$name=$code" }
    val canonical = lines.joinToString(separator = "\n", postfix = "\n")
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
