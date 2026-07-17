package com.borrowly.service.rental;

import com.borrowly.dto.response.RentalResponse;
import com.borrowly.model.rental.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RentalService {

    Page<RentalResponse> listAsBorrower(List<RentalStatus> statuses, Pageable pageable);

    Page<RentalResponse> listAsOwner(List<RentalStatus> statuses, Pageable pageable);

    RentalResponse getById(UUID id);

    RentalResponse returnRental(UUID id);
}
