package com.fpms.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp Base chuẩn dành cho tất cả các bài Integration Test trong hệ thống FPMS.
 *
 * Tính năng chính:
 * 1. Khởi động toàn bộ Spring Context thực tế (@SpringBootTest).
 * 2. Cung cấp MockMvc sẵn sàng thực hiện các HTTP Request (@AutoConfigureMockMvc).
 * 3. Tự động kiểm tra và tạo database 'fpms_test' nếu chưa có qua AutoCreateTestDatabaseInitializer.
 * 4. Tự động ROLLBACK mọi thay đổi sau mỗi method test (@Transactional), giúp CSDL luôn sạch 100%.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ContextConfiguration(initializers = AutoCreateTestDatabaseInitializer.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
}
