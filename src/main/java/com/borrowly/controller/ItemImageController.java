package com.borrowly.controller;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.model.item.ItemImage;
import com.borrowly.service.item.ItemImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items/{itemId}/images")
public class ItemImageController {

    private final ItemImageService itemImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ItemImageResponse upload(
            @PathVariable UUID itemId,
            @RequestParam("file") MultipartFile file
    ) {
        return itemImageService.upload(itemId, file);
    }

    @GetMapping
    public List<ItemImageResponse> list(@PathVariable UUID itemId) {
        return itemImageService.listMetadata(itemId);
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID itemId,
            @PathVariable UUID imageId
    ) {
        ItemImage image = itemImageService.download(itemId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(image.getImageData());
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID itemId,
            @PathVariable UUID imageId
    ) {
        itemImageService.delete(itemId, imageId);
    }
}