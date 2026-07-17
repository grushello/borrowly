package com.borrowly.service.rentalrequest;

import com.borrowly.dto.request.CreateRentalRequest;
import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.dto.response.RentalResponse;
import com.borrowly.model.rental.RentalRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RentalRequestService {

    RentalRequestResponse create(CreateRentalRequest request);

    Page<RentalRequestResponse> getIncoming(RentalRequestStatus status, Pageable pageable);

    Page<RentalRequestResponse> getOutgoing(RentalRequestStatus status, Pageable pageable);

    RentalResponse approve(UUID requestId);

    RentalRequestResponse reject(UUID requestId);

    RentalRequestResponse cancel(UUID requestId);
}