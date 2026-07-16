package com.borrowly.service.item;

import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.exception.CategoryNotFoundException;
import com.borrowly.exception.ItemHasActiveRentalException;
import com.borrowly.exception.ItemNotFoundException;
import com.borrowly.mapper.ItemMapper;
import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.item.ItemSpecification;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.repository.user.ReviewRepository;
import com.borrowly.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final RentalRepository rentalRepository;
    private final ItemMapper itemMapper;
    private final CurrentUserProvider currentUserProvider;


    @Override
    @Transactional
    public ItemResponse create(CreateItemRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new CategoryNotFoundException(request.categoryId()));

        User owner = currentUserProvider.getCurrentUser();

        Item item = Item.builder()
                .title(request.title())
                .description(request.description())
                .pricePerDay(request.pricePerDay())
                .depositAmount(request.depositAmount())
                .finePerDay(request.finePerDay())
                .condition(request.condition())
                .status(ItemStatus.ACTIVE)
                .category(category)
                .owner(owner)
                .build();

        Item saved = itemRepository.save(item);

        log.info(
                "Created item id={} ownerId={}",
                saved.getId(),
                owner.getId()
        );

        Double averageRating =
                reviewRepository.averageRatingByItemId(saved.getId());

        long reviewCount =
                reviewRepository.countByRental_Item_Id(saved.getId());

        return itemMapper.toResponse(
                saved,
                averageRating,
                reviewCount
        );
    }


    @Override
    public ItemResponse getById(UUID id) {

        Item item = itemRepository.findByIdAndStatusNot(
                        id,
                        ItemStatus.ARCHIVED
                )
                .orElseThrow(() ->
                        new ItemNotFoundException(id));

        Double averageRating =
                reviewRepository.averageRatingByItemId(id);

        long reviewCount =
                reviewRepository.countByRental_Item_Id(id);

        return itemMapper.toResponse(
                item,
                averageRating,
                reviewCount
        );
    }


    @Override
    public Page<ItemSummaryResponse> browseItems(
            UUID categoryId,
            ItemCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            Pageable pageable
    ) {

        return itemRepository.findAll(
                        ItemSpecification.browse(
                                categoryId,
                                condition,
                                minPrice,
                                maxPrice,
                                search
                        ),
                        pageable
                )
                .map(itemMapper::toSummary);
    }


    @Override
    public Page<ItemSummaryResponse> getCurrentUserItems(
            Pageable pageable
    ) {

        User currentUser = currentUserProvider.getCurrentUser();

        return itemRepository.findByOwner_Id(
                        currentUser.getId(),
                        pageable
                )
                .map(itemMapper::toSummary);
    }


    @Override
    @Transactional
    public ItemResponse update(
            UUID id,
            UpdateItemRequest request
    ) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ItemNotFoundException(id));

        User currentUser = currentUserProvider.getCurrentUser();

        if (!item.getOwner().getId().equals(currentUser.getId())) {

            log.warn(
                    "Unauthorized update attempt itemId={} userId={}",
                    id,
                    currentUser.getId()
            );

            throw new AccessDeniedException(
                    "You do not own this item."
            );
        }


        if (rentalRepository.existsByItemIdAndStatusIn(
                id,
                List.of(
                        RentalStatus.ACTIVE,
                        RentalStatus.OVERDUE
                ))) {

            log.warn(
                    "Update blocked because item has active rental itemId={}",
                    id
            );

            throw new ItemHasActiveRentalException(id);
        }


        itemMapper.updateEntity(item, request);

        Item saved = itemRepository.save(item);

        log.info(
                "Updated item id={} userId={}",
                saved.getId(),
                currentUser.getId()
        );


        Double averageRating =
                reviewRepository.averageRatingByItemId(saved.getId());

        long reviewCount =
                reviewRepository.countByRental_Item_Id(saved.getId());


        return itemMapper.toResponse(
                saved,
                averageRating,
                reviewCount
        );
    }


    @Override
    @Transactional
    public ItemResponse archive(UUID id) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ItemNotFoundException(id));


        User currentUser = currentUserProvider.getCurrentUser();

        boolean isOwner =
                item.getOwner().getId()
                        .equals(currentUser.getId());

        boolean isAdmin =
                currentUser.getRole() == UserRole.ADMIN;


        if (!isOwner && !isAdmin) {

            log.warn(
                    "Unauthorized archive attempt itemId={} userId={}",
                    id,
                    currentUser.getId()
            );

            throw new AccessDeniedException(
                    "You are not allowed to archive this item."
            );
        }


        if (rentalRepository.existsByItemIdAndStatusIn(
                id,
                List.of(
                        RentalStatus.ACTIVE,
                        RentalStatus.OVERDUE
                ))) {

            log.warn(
                    "Archive blocked because item has active rental itemId={}",
                    id
            );

            throw new ItemHasActiveRentalException(id);
        }


        item.setStatus(ItemStatus.ARCHIVED);

        Item saved = itemRepository.save(item);


        log.info(
                "Archived item id={} by userId={}",
                saved.getId(),
                currentUser.getId()
        );

        Double averageRating = reviewRepository.averageRatingByItemId(saved.getId());
        long reviewCount = reviewRepository.countByRental_Item_Id(saved.getId());

        return itemMapper.toResponse(saved, averageRating, reviewCount);
    }
}