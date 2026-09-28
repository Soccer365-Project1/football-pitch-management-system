package com.fpms.integration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tự động kiểm tra và khởi tạo Database PostgreSQL cho môi trường Test (fpms_test).
 * Giúp mọi thành viên trong team khi clone source code về chỉ cần bấm Run Test là chạy ngay,
 * không cần phải mở DBeaver hay pgAdmin tạo Database thủ công.
 */
public class AutoCreateTestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String DEFAULT_HOST_PORT = "localhost:5433";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";
    private static final String TEST_DB_NAME = "fpms_test";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        String testDbUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://" + DEFAULT_HOST_PORT + "/" + TEST_DB_NAME);
        String username = env.getProperty("spring.datasource.username", DEFAULT_USER);
        String password = env.getProperty("spring.datasource.password", DEFAULT_PASSWORD);

        String adminUrl = buildAdminUrl(testDbUrl);

        try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
             Statement statement = connection.createStatement()) {

            // Kiểm tra xem database fpms_test đã tồn tại trong PostgreSQL chưa
            ResultSet resultSet = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + TEST_DB_NAME + "'"
            );

            if (!resultSet.next()) {
                System.out.println("==================================================================");
                System.out.println(">>> [FPMS Auto-Config] Chưa tìm thấy CSDL '" + TEST_DB_NAME + "'.");
                System.out.println(">>> Đang tự động khởi tạo database '" + TEST_DB_NAME + "'...");
                statement.executeUpdate("CREATE DATABASE " + TEST_DB_NAME);
                System.out.println(">>> [FPMS Auto-Config] Khởi tạo database '" + TEST_DB_NAME + "' thành công!");
                System.out.println("==================================================================");
            } else {
                System.out.println(">>> [FPMS Auto-Config] Database '" + TEST_DB_NAME + "' đã sẵn sàng trên PostgreSQL.");
            }

        } catch (Exception e) {
            System.err.println(">>> [FPMS Auto-Config Cảnh báo] Không thể tự động kiểm tra/tạo database '" + TEST_DB_NAME + "': " + e.getMessage());
            System.err.println(">>> Lưu ý: Hãy đảm bảo Docker PostgreSQL đang chạy (docker compose up -d postgres)");
        }
    }

    private String buildAdminUrl(String dbUrl) {
        if (dbUrl != null && dbUrl.contains("/")) {
            int lastSlash = dbUrl.lastIndexOf('/');
            return dbUrl.substring(0, lastSlash + 1) + "postgres";
        }
        return "jdbc:postgresql://" + DEFAULT_HOST_PORT + "/postgres";
    }
}
