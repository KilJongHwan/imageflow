package com.imageflow.backend.common.storage;

public record UploadBinary(
        String originalFilename,
        byte[] bytes
) {
    public long size() {
        return bytes.length;
    }
}
