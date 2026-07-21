package com.borrowly.service.item;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.exception.*;
import com.borrowly.mapper.ItemImageMapper;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemImage;
import com.borrowly.model.user.User;
import com.borrowly.repository.item.ItemImageRepository;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemImageServiceImpl implements ItemImageService {

    private static final int MAX_IMAGES = 5;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemImageMapper itemImageMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public ItemImageResponse upload(UUID itemId, MultipartFile file) {
        Item item = findItem(itemId);
        enforceOwnership(item);
        validateFile(file);

        long count = itemImageRepository.countByItem_Id(itemId);
        if (count >= MAX_IMAGES) {
            throw new ImageLimitExceededException("Item already has the maximum of " + MAX_IMAGES + " images");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("Failed to read uploaded file");
        }

        ItemImage image = ItemImage.builder()
                .imageData(data)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .primary(count == 0)
                .build();
        item.addImage(image);
        itemImageRepository.save(image);

        log.info("Uploaded image {} for item {}", image.getId(), itemId);
        return itemImageMapper.toResponse(image);
    }

    @Override
    public List<ItemImageResponse> listMetadata(UUID itemId) {
        findItem(itemId);
        return itemImageMapper.fromProjectionList(
                itemImageRepository.findByItem_IdOrderByCreatedAtAsc(itemId)
        );
    }

    @Override
    public ItemImage download(UUID itemId, UUID imageId) {
        return itemImageRepository.findByIdAndItem_Id(imageId, itemId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));
    }

    @Override
    @Transactional
    public void delete(UUID itemId, UUID imageId) {
        Item item = findItem(itemId);
        enforceOwnership(item);

        ItemImage image = itemImageRepository.findByIdAndItem_Id(imageId, itemId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));

        boolean wasPrimary = image.isPrimary();
        itemImageRepository.delete(image);

        if (wasPrimary) {
            itemImageRepository.flush();
            itemImageRepository.findFirstByItem_IdOrderByCreatedAtAsc(itemId)
                    .ifPresent(oldest -> itemImageRepository.markAsPrimary(oldest.getId()));
        }

        log.info("Deleted image {} from item {}", imageId, itemId);
    }

    private Item findItem(UUID itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));
    }

    private void enforceOwnership(Item item) {
        User currentUser = currentUserProvider.getCurrentUser();
        UUID ownerId = Optional.ofNullable(item.getOwner())
                .map(User::getId)
                .orElseThrow(() -> new ForbiddenActionException("Item has no owner"));
        if (!ownerId.equals(currentUser.getId())) {
            throw new ForbiddenActionException("Only the item owner can manage images");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException("File size exceeds the 5 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidImageException("Unsupported content type. Allowed: JPEG, PNG, WebP");
        }
    }
}