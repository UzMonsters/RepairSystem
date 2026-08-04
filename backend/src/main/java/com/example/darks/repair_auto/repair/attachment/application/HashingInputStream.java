package com.example.darks.repair_auto.repair.attachment.application;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

public class HashingInputStream extends FilterInputStream {

    private final MessageDigest digest;
    private long sizeBytes;

    public HashingInputStream(InputStream in, MessageDigest digest) {
        super(in);
        this.digest = digest;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            digest.update((byte) value);
            sizeBytes++;
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            digest.update(buffer, offset, read);
            sizeBytes += read;
        }
        return read;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public String checksum() {
        return HexFormat.of().formatHex(digest.digest());
    }
}
