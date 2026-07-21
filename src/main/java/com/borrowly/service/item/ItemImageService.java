package com.borrowly.service.item;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.model.item.ItemImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ItemImageService {

    ItemImageResponse upload(UUID itemId, MultipartFile file);

    List<ItemImageResponse> listMetadata(UUID itemId);

    ItemImage download(UUID itemId, UUID imageId);

    void delete(UUID itemId, UUID imageId);
}