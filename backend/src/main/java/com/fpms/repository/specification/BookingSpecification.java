package com.fpms.repository.specification;

import com.fpms.dto.request.AdminBookingSearchRequest;
import com.fpms.dto.request.BookingSearchRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp xây dựng Specification động cho truy vấn tìm kiếm và lọc đơn đặt sân (Booking).
 */
public class BookingSpecification {

    private BookingSpecification() {
        // Private constructor to prevent instantiation
    }

    /**
     * Tạo Specification lọc đơn đặt sân của khách hàng hiện tại dựa trên BookingSearchRequest.
     *
     * @param request    Đối tượng chứa các tiêu chí tìm kiếm và lọc
     * @param customerId ID của khách hàng đang đăng nhập (Bảo mật IDOR)
     * @return Specification<Booking>
     */
    public static Specification<Booking> filterMyBookings(BookingSearchRequest request, Long customerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Điều kiện bắt buộc: Đơn chưa bị xóa mềm và thuộc sở hữu của khách hàng
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }

            if (request == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Lọc theo trạng thái đơn (status)
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            // 3. Lọc theo khoảng ngày đá (bookingDate)
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingDate"), request.getToDate()));
            }

            // 4. Lọc theo sân bóng / loại sân nếu có
            if (request.getPitchId() != null) {
                predicates.add(cb.equal(root.get("pitch").get("id"), request.getPitchId()));
            }
            if (request.getPitchTypeId() != null) {
                predicates.add(cb.equal(root.get("pitch").get("pitchType").get("id"), request.getPitchTypeId()));
            }

            // 5. Lọc theo khung giờ (timeSlotId)
            if (request.getTimeSlotId() != null) {
                predicates.add(cb.equal(root.get("timeSlot").get("id"), request.getTimeSlotId()));
            }

            // 6. Lọc theo hình thức đặt (bookingType: ONLINE, AT_COUNTER)
            if (request.getBookingType() != null) {
                predicates.add(cb.equal(root.get("bookingType"), request.getBookingType()));
            }

            // 7. Tìm kiếm theo từ khóa (keyword) kết hợp trường tìm kiếm (searchField)
            String keyword = request.getCleanKeyword();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                String field = StringUtils.hasText(request.getSearchField())
                        ? request.getSearchField().trim().toUpperCase()
                        : "ALL";

                Predicate codeMatch = cb.like(cb.lower(root.get("bookingCode")), pattern);
                Predicate pitchMatch = cb.like(cb.lower(root.get("pitchNameSnapshot")), pattern);

                // Phone có thể ở guestPhone hoặc customer.phoneNumber
                Predicate phoneMatch = cb.or(
                        cb.like(root.get("guestPhone"), "%" + keyword + "%"),
                        cb.like(root.join("customer", JoinType.LEFT).get("phoneNumber"), "%" + keyword + "%")
                );

                switch (field) {
                    case "BOOKING_CODE":
                        predicates.add(codeMatch);
                        break;
                    case "PITCH_NAME":
                        predicates.add(pitchMatch);
                        break;
                    case "PHONE":
                        predicates.add(phoneMatch);
                        break;
                    case "ALL":
                    default:
                        predicates.add(cb.or(codeMatch, pitchMatch, phoneMatch));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification động cho phép Admin/Staff lọc danh sách đơn đặt sân theo các tiêu chí thiết thực.
     *
     * @param request Đối tượng chứa các tiêu chí lọc của Admin
     * @return Specification<Booking>
     */
    public static Specification<Booking> filterAdminBookings(AdminBookingSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy các đơn chưa bị xóa mềm
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (request == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Lọc theo trạng thái đơn (BookingStatus)
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            // 3. Lọc theo khoảng ngày đá (fromDate -> toDate)
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingDate"), request.getToDate()));
            }

            // 4. Lọc theo sân bóng
            if (request.getPitchId() != null) {
                predicates.add(cb.equal(root.get("pitch").get("id"), request.getPitchId()));
            }

            // 5. Tìm kiếm nhanh theo từ khóa (keyword) trên Mã đơn, Tên khách, SĐT
            String keyword = request.getCleanKeyword();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Join<Booking, User> customerJoin = root.join("customer", JoinType.LEFT);

                Predicate codeMatch = cb.like(cb.lower(root.get("bookingCode")), pattern);
                Predicate guestNameMatch = cb.like(cb.lower(root.get("guestName")), pattern);
                Predicate customerNameMatch = cb.like(cb.lower(customerJoin.get("fullName")), pattern);
                Predicate guestPhoneMatch = cb.like(root.get("guestPhone"), "%" + keyword + "%");
                Predicate customerPhoneMatch = cb.like(customerJoin.get("phoneNumber"), "%" + keyword + "%");

                predicates.add(cb.or(codeMatch, guestNameMatch, customerNameMatch, guestPhoneMatch, customerPhoneMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
