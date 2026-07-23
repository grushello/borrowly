package com.borrowly.service.item;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.exception.*;
import com.borrowly.mapper.ItemImageMapper;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemImage;
import com.borrowly.model.user.User;
import com.borrowly.repository.item.ItemImageMetadata;
import com.borrowly.repository.item.ItemImageRepository;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemImageServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemImageRepository itemImageRepository;

    @Mock
    private ItemImageMapper itemImageMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ItemImageServiceImpl service;

    private User owner;
    private User stranger;
    private Item item;
    private UUID itemId;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, Month.JANUARY, 1, 12, 0);

    @BeforeEach
    void setUp() {
        owner = User.register("Own", "Er", "owner@test.com", "hash");
        stranger = User.register("Str", "Anger", "stranger@test.com", "hash");
        item = Item.builder().owner(owner).build();
        itemId = item.getId();
    }

    @Test
    @DisplayName("upload — first image becomes primary")
    void uploadFirstImageBecomesPrimary() {
        MockMultipartFile file = jpegFile("photo.jpg", 100);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(itemImageRepository.countByItemId(itemId)).thenReturn(0L);
        when(itemImageRepository.save(any(ItemImage.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemImageResponse expected = new ItemImageResponse(UUID.randomUUID(), "photo.jpg", "image/jpeg", true, FIXED_TIME);
        when(itemImageMapper.toResponse(any())).thenReturn(expected);

        ItemImageResponse result = service.upload(itemId, file);

        assertThat(result.primary()).isTrue();
        verify(itemImageRepository).save(any(ItemImage.class));
    }

    @Test
    @DisplayName("upload — second image is not primary")
    void uploadSecondImageNotPrimary() {
        MockMultipartFile file = jpegFile("photo2.jpg", 100);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(itemImageRepository.countByItemId(itemId)).thenReturn(1L);
        when(itemImageRepository.save(any(ItemImage.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemImageResponse expected = new ItemImageResponse(UUID.randomUUID(), "photo2.jpg", "image/jpeg", false, FIXED_TIME);
        when(itemImageMapper.toResponse(any())).thenReturn(expected);

        ItemImageResponse result = service.upload(itemId, file);

        assertThat(result.primary()).isFalse();
    }

    @Test
    @DisplayName("upload — max limit throws ImageLimitExceededException")
    void uploadMaxLimitReached() {
        MockMultipartFile file = jpegFile("photo.jpg", 100);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(itemImageRepository.countByItemId(itemId)).thenReturn(5L);

        assertThatThrownBy(() -> service.upload(itemId, file))
                .isInstanceOf(ImageLimitExceededException.class);
    }

    @Test
    @DisplayName("upload — file exceeding 5 MB rejected")
    void uploadFileTooLarge() {
        MockMultipartFile file = jpegFile("big.jpg", 6 * 1024 * 1024);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> service.upload(itemId, file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    @DisplayName("upload — invalid content type rejected")
    void uploadInvalidContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> service.upload(itemId, file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    @DisplayName("upload — non-owner gets ForbiddenActionException")
    void uploadByNonOwnerForbidden() {
        MockMultipartFile file = jpegFile("photo.jpg", 100);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(stranger);

        assertThatThrownBy(() -> service.upload(itemId, file))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("download — returns full entity with binary data")
    void downloadReturnsBinary() {
        UUID imageId = UUID.randomUUID();
        ItemImage image = ItemImage.builder()
                .imageData(new byte[]{1, 2, 3})
                .fileName("photo.jpg")
                .contentType("image/jpeg")
                .build();

        when(itemImageRepository.findByIdAndItemId(imageId, itemId)).thenReturn(Optional.of(image));

        ItemImage result = service.download(itemId, imageId);

        assertThat(result.getImageData()).containsExactly(1, 2, 3);
        assertThat(result.getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("delete — primary reassigned to oldest remaining")
    void deletePrimaryReassigns() {
        UUID imageId = UUID.randomUUID();
        ItemImage primary = ItemImage.builder()
                .imageData(new byte[]{1})
                .fileName("a.jpg")
                .contentType("image/jpeg")
                .primary(true)
                .build();

        ItemImage oldest = ItemImage.builder()
                .imageData(new byte[]{2})
                .fileName("b.jpg")
                .contentType("image/jpeg")
                .build();

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(itemImageRepository.findByIdAndItemId(imageId, itemId)).thenReturn(Optional.of(primary));
        when(itemImageRepository.findFirstByItemIdOrderByCreatedAtAsc(itemId)).thenReturn(Optional.of(oldest));

        service.delete(itemId, imageId);

        verify(itemImageRepository).delete(primary);
        verify(itemImageRepository).markAsPrimary(oldest.getId());
    }

    @Test
    @DisplayName("delete — non-primary does not reassign")
    void deleteNonPrimaryNoReassign() {
        UUID imageId = UUID.randomUUID();
        ItemImage nonPrimary = ItemImage.builder()
                .imageData(new byte[]{1})
                .fileName("a.jpg")
                .contentType("image/jpeg")
                .primary(false)
                .build();

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(itemImageRepository.findByIdAndItemId(imageId, itemId)).thenReturn(Optional.of(nonPrimary));

        service.delete(itemId, imageId);

        verify(itemImageRepository).delete(nonPrimary);
        verify(itemImageRepository, never()).markAsPrimary(any());
    }

    @Test
    @DisplayName("listMetadata — uses projection, no imageData loaded")
    void listMetadataUsesProjection() {
        ItemImageMetadata meta = mock(ItemImageMetadata.class);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemImageRepository.findByItemIdOrderByCreatedAtAsc(itemId)).thenReturn(List.of(meta));
        when(itemImageMapper.fromProjectionList(any())).thenReturn(List.of(
                new ItemImageResponse(UUID.randomUUID(), "a.jpg", "image/jpeg", true, FIXED_TIME)
        ));

        List<ItemImageResponse> result = service.listMetadata(itemId);

        assertThat(result).hasSize(1);
        verify(itemImageRepository).findByItemIdOrderByCreatedAtAsc(itemId);
        verify(itemImageRepository, never()).findByIdAndItemId(any(), any());
    }

    private MockMultipartFile jpegFile(String name, int size) {
        return new MockMultipartFile("file", name, "image/jpeg", new byte[size]);
    }
}