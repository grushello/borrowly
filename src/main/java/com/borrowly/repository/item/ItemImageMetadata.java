package com.borrowly.repository.item;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ItemImageMetadata {

    UUID getId();

    String getFileName();

    String getContentType();

    Boolean getPrimary();

    LocalDateTime getCreatedAt();
}