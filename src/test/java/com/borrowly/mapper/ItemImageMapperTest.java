package com.borrowly.mapper;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.model.item.ItemImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemImageMapperTest {
    ItemImageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ItemImageMapperImpl();
    }

    @Test
    void toResponse_MapsAllFields() {
        ItemImage entity = ItemImage.builder()
                .fileName("drill.png")
                .contentType("image/png")
                .imageData(new byte[]{1, 2, 3})
                .primary(true)
                .build();

        ItemImageResponse response = mapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.fileName()).isEqualTo("drill.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.primary()).isTrue();
        assertThat(response.createdAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void toResponse_NeverExposesImageData() {
        List<String> fieldNames = Arrays.stream(ItemImageResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(fieldNames).isNotEmpty()
                .doesNotContain("imageData", "image_data", "data");
    }

    @Test
    void toResponseList_MapsAllElements() {
        ItemImage first = ItemImage.builder()
                .fileName("a.png")
                .contentType("image/png")
                .imageData(new byte[]{1})
                .primary(true)
                .build();
        ItemImage second = ItemImage.builder()
                .fileName("b.jpg")
                .contentType("image/jpeg")
                .imageData(new byte[]{2})
                .primary(false)
                .build();

        List<ItemImageResponse> responses = mapper.toResponseList(List.of(first, second));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).fileName()).isEqualTo("a.png");
        assertThat(responses.get(1).fileName()).isEqualTo("b.jpg");
        assertThat(responses.get(1).primary()).isFalse();
    }
}