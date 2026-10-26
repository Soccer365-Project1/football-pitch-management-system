package com.fpms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: VNPayUtil")
class VNPayUtilTest {

    private final String secretKey = "TEST_SECRET_KEY_123456";

    @Test
    @DisplayName("Băm HMAC-SHA512: Tính toán chính xác chữ ký số")
    void testHmacSHA512_ValidData() {
        String data = "vnp_Amount=9000000&vnp_Command=pay&vnp_TxnRef=TXN_123";
        String hash1 = VNPayUtil.hmacSHA512(secretKey, data);
        String hash2 = VNPayUtil.hmacSHA512(secretKey, data);

        assertNotNull(hash1);
        assertEquals(128, hash1.length(), "HMAC-SHA512 băm ra chuỗi hex 128 ký tự");
        assertEquals(hash1, hash2, "Cùng input và key thì kết quả băm phải nhất quán");
    }

    @Test
    @DisplayName("Băm HMAC-SHA512: Xử lý an toàn khi đầu vào null")
    void testHmacSHA512_NullInputs() {
        assertEquals("", VNPayUtil.hmacSHA512(null, "data"));
        assertEquals("", VNPayUtil.hmacSHA512(secretKey, null));
    }

    @Test
    @DisplayName("Ghép Hash Data: Sắp xếp tham số tự động theo bảng chữ cái A-Z")
    void testBuildHashData_AlphabeticalSorting() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_001");
        params.put("vnp_Amount", "9000000");
        params.put("vnp_Command", "pay");

        String hashData = VNPayUtil.buildHashData(params);

        // Thứ tự chuẩn A-Z: vnp_Amount -> vnp_Command -> vnp_TxnRef
        String expected = "vnp_Amount=9000000&vnp_Command=pay&vnp_TxnRef=TXN_001";
        assertEquals(expected, hashData);
    }

    @Test
    @DisplayName("Lấy IP Client: Hỗ trợ Header X-FORWARDED-FOR khi qua Proxy/Load Balancer")
    void testGetIpAddress_WithProxyHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FORWARDED-FOR", "14.162.144.1, 10.0.0.1");

        String ip = VNPayUtil.getIpAddress(request);
        assertEquals("14.162.144.1", ip, "Phải lấy IP gốc đầu tiên của khách hàng");
    }

    @Test
    @DisplayName("Lấy IP Client: Chuẩn hóa localhost IPv6 về IPv4 127.0.0.1")
    void testGetIpAddress_LocalhostIPv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String ip = VNPayUtil.getIpAddress(request);
        assertEquals("127.0.0.1", ip);
    }
}
