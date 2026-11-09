package com.fpms.service;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.security.UserPrincipal;

public interface ExcelExportService {
    byte[] exportRevenueReport(ReportFilterRequest filterRequest, UserPrincipal currentUser);
}
