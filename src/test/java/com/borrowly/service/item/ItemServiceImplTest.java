package com.borrowly.service.item;

import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.exception.CategoryNotFoundException;
import com.borrowly.exception.ItemHasActiveRentalException;
import com.borrowly.exception.ItemNotFoundException;
import com.borrowly.mapper.ItemMapper;
import com.borrowly.model.item.*;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.repository.user.ReviewRepository;
import com.borrowly.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ItemServiceImpl itemService;


    private User owner;
    private Category category;
    private Item item;


    @BeforeEach
    void setup() {

        owner = User.register(
                "John",
                "Smith",
                "john@test.com",
                "password"
        );

        category = Category.builder()
                .name("Tools")
                .build();


        item = Item.builder()
                .title("Drill")
                .description("Electric drill")
                .pricePerDay(BigDecimal.TEN)
                .depositAmount(BigDecimal.valueOf(50))
                .finePerDay(BigDecimal.valueOf(5))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ACTIVE)
                .category(category)
                .owner(owner)
                .build();
    }


    @Test
    @DisplayName("create saves item with authenticated user as owner")
    void createItemSuccess() {

        CreateItemRequest request =
                new CreateItemRequest(
                        "Drill",
                        "Electric drill",
                        BigDecimal.TEN,
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(5),
                        ItemCondition.GOOD,
                        category.getId()
                );


        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);

        when(reviewRepository.averageRatingByItemId(any()))
                .thenReturn(null);

        when(reviewRepository.countByItemId(any()))
                .thenReturn(0L);

        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        itemService.create(request);


        ArgumentCaptor<Item> captor =
                ArgumentCaptor.forClass(Item.class);

        verify(itemRepository)
                .save(captor.capture());


        Item saved = captor.getValue();

        assertThat(saved.getTitle())
                .isEqualTo("Drill");

        assertThat(saved.getOwner())
                .isEqualTo(owner);

        assertThat(saved.getStatus())
                .isEqualTo(ItemStatus.ACTIVE);
    }


    @Test
    @DisplayName("create throws when category does not exist")
    void createFailsWhenCategoryMissing() {

        UUID categoryId = UUID.randomUUID();

        CreateItemRequest request =
                new CreateItemRequest(
                        "Drill",
                        "Electric drill",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        ItemCondition.GOOD,
                        categoryId
                );


        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() ->
                itemService.create(request))
                .isInstanceOf(CategoryNotFoundException.class);


        verify(itemRepository, never())
                .save(any());
    }


    @Test
    @DisplayName("getById returns active item")
    void getActiveItem() {

        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));


        itemService.getById(item.getId());


        verify(itemRepository)
                .findById(item.getId());
    }


    @Test
    @DisplayName("getById returns rented item")
    void getRentedItem() {

        item.setStatus(ItemStatus.RENTED);


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));


        itemService.getById(item.getId());


        verify(itemRepository)
                .findById(item.getId());
    }

    @Test
    @DisplayName("getById returns archived item")
    void getArchivedItem() {

        item.setStatus(ItemStatus.ARCHIVED);

        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        itemService.getById(item.getId());

        verify(itemRepository).findById(item.getId());
    }

    @Test
    @DisplayName("update own item")
    void updateOwnItem() {

        UpdateItemRequest request =
                new UpdateItemRequest(
                        "New title",
                        "desc",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        ItemCondition.NEW
                );


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);

        when(rentalRepository.existsByItemIdAndStatusIn(
                any(),
                any()))
                .thenReturn(false);


        when(itemRepository.save(item))
                .thenReturn(item);

        when(reviewRepository.averageRatingByItemId(any()))
                .thenReturn(null);

        when(reviewRepository.countByItemId(any()))
                .thenReturn(0L);


        itemService.update(item.getId(), request);


        verify(itemMapper)
                .updateEntity(item, request);
    }


    @Test
    @DisplayName("update ignores null fields")
    void updateIgnoresNullFields() {

        UpdateItemRequest request =
                new UpdateItemRequest(
                        "Updated title",
                        null,
                        null,
                        null,
                        null,
                        null
                );


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);

        when(rentalRepository.existsByItemIdAndStatusIn(
                any(),
                any()))
                .thenReturn(false);

        when(itemRepository.save(item))
                .thenReturn(item);


        itemService.update(item.getId(), request);


        verify(itemMapper)
                .updateEntity(item, request);
    }


    @Test
    @DisplayName("update forbidden for another user")
    void updateForbidden() {

        User another =
                User.register(
                        "Bob",
                        "Jones",
                        "bob@test.com",
                        "password"
                );


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(another);


        assertThatThrownBy(() ->
                itemService.update(
                        item.getId(),
                        mock(UpdateItemRequest.class)))
                .isInstanceOf(AccessDeniedException.class);
    }


    @Test
    @DisplayName("update blocked when active rental exists")
    void updateBlockedByRental() {

        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);


        when(rentalRepository.existsByItemIdAndStatusIn(
                any(),
                any()))
                .thenReturn(true);


        assertThatThrownBy(() ->
                itemService.update(
                        item.getId(),
                        mock(UpdateItemRequest.class)))
                .isInstanceOf(ItemHasActiveRentalException.class);
    }


    @Test
    @DisplayName("archive own item")
    void archiveOwnItem() {

        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);

        when(rentalRepository.existsByItemIdAndStatusIn(
                any(),
                any()))
                .thenReturn(false);

        when(itemRepository.save(item))
                .thenReturn(item);


        itemService.archive(item.getId());


        assertThat(item.getStatus())
                .isEqualTo(ItemStatus.ARCHIVED);
    }


    @Test
    @DisplayName("archive forbidden for another user")
    void archiveForbidden() {

        User another =
                User.register(
                        "Bob",
                        "Jones",
                        "bob@test.com",
                        "password"
                );


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(another);


        assertThatThrownBy(() ->
                itemService.archive(item.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }


    @Test
    @DisplayName("archive blocked when active rental exists")
    void archiveBlockedByRental() {

        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);


        when(rentalRepository.existsByItemIdAndStatusIn(
                any(),
                any()))
                .thenReturn(true);


        assertThatThrownBy(() ->
                itemService.archive(item.getId()))
                .isInstanceOf(ItemHasActiveRentalException.class);
    }


    @Test
    @DisplayName("admin can archive item")
    void adminCanArchive() {

        User admin = mock(User.class);

        when(admin.getId())
                .thenReturn(UUID.randomUUID());

        when(admin.getRole())
                .thenReturn(UserRole.ADMIN);


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(admin);


        when(rentalRepository.existsByItemIdAndStatusIn(
                any(),
                any()))
                .thenReturn(false);

        when(itemRepository.save(item))
                .thenReturn(item);


        itemService.archive(item.getId());


        assertThat(item.getStatus())
                .isEqualTo(ItemStatus.ARCHIVED);
    }
    @Test
    @DisplayName("unarchive own item")
    void unarchiveOwnItem() {

        item.setStatus(ItemStatus.ARCHIVED);

        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(owner);

        when(itemRepository.save(item))
                .thenReturn(item);


        itemService.unarchive(item.getId());


        assertThat(item.getStatus())
                .isEqualTo(ItemStatus.ACTIVE);
    }


    @Test
    @DisplayName("unarchive forbidden for another user")
    void unarchiveForbidden() {

        item.setStatus(ItemStatus.ARCHIVED);

        User another =
                User.register(
                        "Bob",
                        "Jones",
                        "bob@test.com",
                        "password"
                );


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(another);


        assertThatThrownBy(() ->
                itemService.unarchive(item.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }


    @Test
    @DisplayName("admin can unarchive item")
    void adminCanUnarchive() {

        item.setStatus(ItemStatus.ARCHIVED);

        User admin = mock(User.class);

        when(admin.getId())
                .thenReturn(UUID.randomUUID());

        when(admin.getRole())
                .thenReturn(UserRole.ADMIN);


        when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        when(currentUserProvider.getCurrentUser())
                .thenReturn(admin);

        when(itemRepository.save(item))
                .thenReturn(item);


        itemService.unarchive(item.getId());


        assertThat(item.getStatus())
                .isEqualTo(ItemStatus.ACTIVE);
    }


    @Test
    @DisplayName("unarchive throws when item not found")
    void unarchiveItemNotFound() {

        UUID missingId = UUID.randomUUID();

        when(itemRepository.findById(missingId))
                .thenReturn(Optional.empty());


        assertThatThrownBy(() ->
                itemService.unarchive(missingId))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    @DisplayName("adminListItems delegates to the unfiltered admin query")
    void adminListItemsUsesAdminQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Item item = Item.builder().title("Drill").build();
        ItemSummaryResponse summary = new ItemSummaryResponse(
                UUID.randomUUID(), "Drill", BigDecimal.TEN,
                ItemCondition.GOOD, ItemStatus.ARCHIVED, "Jane Owner", null);

        when(itemRepository.findAllForAdmin(pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(item), pageable, 1));
        when(itemMapper.toSummary(item)).thenReturn(summary);

        Page<ItemSummaryResponse> result = itemService.adminListItems(pageable);

        assertThat(result.getContent()).containsExactly(summary);
        verify(itemRepository).findAllForAdmin(pageable);
    }

    @Test
    @DisplayName("adminListItems does not apply the ACTIVE-only catalog specification")
    void adminListItemsDoesNotUseSpecification() {
        Pageable pageable = PageRequest.of(0, 10);
        when(itemRepository.findAllForAdmin(pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(), pageable, 0));

        itemService.adminListItems(pageable);

        verify(itemRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("adminListItems propagates paging metadata")
    void adminListItemsPropagatesPaging() {
        Pageable pageable = PageRequest.of(2, 5);
        when(itemRepository.findAllForAdmin(pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(), pageable, 42));

        Page<ItemSummaryResponse> result = itemService.adminListItems(pageable);

        assertThat(result.getTotalElements()).isEqualTo(42);
        assertThat(result.getNumber()).isEqualTo(2);
    }
}