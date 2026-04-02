package com.imageflow.backend.common.storage;

import java.nio.file.Path;

public record StoredFile(
        String filename,
        Path path
) {
}
