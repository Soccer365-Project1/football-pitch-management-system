package com.fpms.service;

import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.BookingCancellation;
import com.fpms.entity.User;

public interface BookingCancellationService {
    BookingCancellation createCancellation(Booking booking, User requestedBy, BookingCancelRequest request);
}
