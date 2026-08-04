package com.example.darks.repair_auto.shared.observability;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class TraceIdService {

    public static final int MAX_TRACE_ID_LENGTH = 64;
    public static final String TRACE_ID_PATTERN = "^[A-Za-z0-9._-]+$";

    private static final int TRACE_BYTES = 16;
    private static final int MIN_TRACE_ID_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String resolve(String incomingTraceId) {
        if (isValid(incomingTraceId)) {
            return incomingTraceId;
        }
        byte[] bytes = new byte[TRACE_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public boolean isValid(String traceId) {
        return traceId != null
                && !traceId.isBlank()
                && traceId.length() >= MIN_TRACE_ID_LENGTH
                && traceId.length() <= MAX_TRACE_ID_LENGTH
                && traceId.matches(TRACE_ID_PATTERN);
    }
}
