package com.fpms.service.impl;

import com.fpms.dto.request.HolidayRequest;
import com.fpms.dto.response.HolidayResponse;
import com.fpms.entity.Holiday;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.HolidayMapper;
import com.fpms.repository.HolidayRepository;
import com.fpms.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;

    @Override
    public List<HolidayResponse> getAllHolidays(Integer year) {
        log.info("Lấy danh sách ngày lễ - lọc theo năm: {}", year);
        List<Holiday> holidays;
        if (year != null) {
            holidays = holidayRepository.findAllByYearOrderByHolidayDateAsc(year);
        } else {
            holidays = holidayRepository.findAllByOrderByHolidayDateAsc();
        }
        return holidayMapper.toHolidayResponseList(holidays);
    }

    @Override
    public HolidayResponse getHolidayById(Long id) {
        log.info("Lấy thông tin chi tiết ngày lễ ID: {}", id);
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOLIDAY_NOT_FOUND));
        return holidayMapper.toHolidayResponse(holiday);
    }

    @Override
    @Transactional
    public HolidayResponse createHoliday(HolidayRequest request) {
        log.info("Tạo mới ngày lễ: {} - {}", request.getHolidayDate(), request.getName());

        if (holidayRepository.existsByHolidayDate(request.getHolidayDate())) {
            log.warn("Ngày lễ đã tồn tại trong hệ thống: {}", request.getHolidayDate());
            throw new AppException(ErrorCode.HOLIDAY_DATE_ALREADY_EXISTS);
        }

        Holiday holiday = holidayMapper.toHoliday(request);
        Holiday savedHoliday = holidayRepository.save(holiday);
        log.info("Tạo ngày lễ thành công với ID: {}", savedHoliday.getId());

        return holidayMapper.toHolidayResponse(savedHoliday);
    }

    @Override
    @Transactional
    public HolidayResponse updateHoliday(Long id, HolidayRequest request) {
        log.info("Cập nhật ngày lễ ID: {} - {} - {}", id, request.getHolidayDate(), request.getName());

        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOLIDAY_NOT_FOUND));

        if (holidayRepository.existsByHolidayDateAndIdNot(request.getHolidayDate(), id)) {
            log.warn("Ngày lễ bị trùng với bản ghi khác: {}", request.getHolidayDate());
            throw new AppException(ErrorCode.HOLIDAY_DATE_ALREADY_EXISTS);
        }

        holidayMapper.updateHolidayFromRequest(request, holiday);
        Holiday updatedHoliday = holidayRepository.save(holiday);
        log.info("Cập nhật ngày lễ ID: {} thành công", id);

        return holidayMapper.toHolidayResponse(updatedHoliday);
    }

    @Override
    @Transactional
    public void deleteHoliday(Long id) {
        log.info("Xóa ngày lễ ID: {}", id);

        if (!holidayRepository.existsById(id)) {
            log.warn("Không tìm thấy ngày lễ ID: {} để xóa", id);
            throw new AppException(ErrorCode.HOLIDAY_NOT_FOUND);
        }

        holidayRepository.deleteById(id);
        log.info("Xóa thành công ngày lễ ID: {}", id);
    }
}
