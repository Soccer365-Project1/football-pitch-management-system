package com.fpms.service.impl;

import com.fpms.dto.request.BrevoEmailRequest;
import com.fpms.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class BrevoEmailServiceImpl implements EmailService {

    private final RestClient restClient;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${app.mail.from-email:giapit02012005@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Soccer365}")
    private String fromName;

    @Autowired
    public BrevoEmailServiceImpl() {
        this(createDefaultRestClient());
    }

    public BrevoEmailServiceImpl(RestClient restClient) {
        this.restClient = restClient != null ? restClient : createDefaultRestClient();
    }

    private static RestClient createDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendOtpEmail(String toEmail, String otpCode, int expirationMinutes) {
        log.info("Đang tiến hành gửi email OTP đặt lại mật khẩu đến: {}", toEmail);

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO_API_KEY chưa được cấu hình. Vui lòng thêm BREVO_API_KEY để gửi email.");
            return;
        }

        String trimmedKey = brevoApiKey.trim();
        if (trimmedKey.startsWith("xsmtpsib-")) {
            log.error("❌ CẢNH BÁO: Key bạn đang dùng là SMTP Key (tiền tố 'xsmtpsib-'), không phải API Key! " +
                    "Vui lòng vào Brevo Dashboard -> tab 'API keys & MCP' để tạo API Key (tiền tố 'xkeysib-').");
        }

        try {
            BrevoEmailRequest request = BrevoEmailRequest.builder()
                    .sender(BrevoEmailRequest.Sender.builder()
                            .name(fromName)
                            .email(fromEmail)
                            .build())
                    .to(List.of(new BrevoEmailRequest.Recipient(toEmail)))
                    .subject("[Soccer365] Mã xác thực đặt lại mật khẩu của bạn")
                    .htmlContent(buildHtmlContent(otpCode, expirationMinutes))
                    .build();

            String response = restClient.post()
                    .uri(brevoApiUrl)
                    .header("api-key", brevoApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            log.info("Đã gửi email OTP thành công qua Brevo REST API tới: {}, response: {}", toEmail, response);
        } catch (RestClientResponseException ex) {
            log.error("Lỗi phản hồi từ Brevo REST API khi gửi tới {}: Status={}, ResponseBody={}",
                    toEmail, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Lỗi kết nối khi gọi Brevo REST API tới {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    /**
     * Soạn thảo mẫu Email HTML sang trọng, thân thiện và bảo mật
     */
    private String buildHtmlContent(String otpCode, int expirationMinutes) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<title>Mã xác thực đặt lại mật khẩu</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; background-color: #f4f4f7; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>" +
                "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='padding: 40px 0;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table width='580' border='0' cellspacing='0' cellpadding='0' style='background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06);'>" +
                "          <!-- Header Banner -->" +
                "          <tr>" +
                "            <td style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); padding: 30px; text-align: center; color: #ffffff;'>" +
                "              <h1 style='margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;'>Soccer365 FPMS</h1>" +
                "              <p style='margin: 6px 0 0 0; font-size: 14px; opacity: 0.9;'>Hệ thống quản lý đặt sân bóng thông minh</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Body Content -->" +
                "          <tr>" +
                "            <td style='padding: 35px 30px; color: #333333; line-height: 1.6;'>" +
                "              <h2 style='margin: 0 0 15px 0; font-size: 18px; color: #111827;'>Yêu cầu đặt lại mật khẩu</h2>" +
                "              <p style='margin: 0 0 20px 0; font-size: 14px; color: #4b5563;'>" +
                "                Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Soccer365 của bạn. Dưới đây là mã xác thực OTP dùng một lần:" +
                "              </p>" +
                "              <!-- OTP Box -->" +
                "              <div style='text-align: center; margin: 25px 0;'>" +
                "                <div style='display: inline-block; background-color: #ecfdf5; border: 2px dashed #10b981; border-radius: 10px; padding: 16px 36px;'>" +
                "                  <span style='font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #047857; font-family: monospace;'>" + otpCode + "</span>" +
                "                </div>" +
                "              </div>" +
                "              <!-- Warning Notice -->" +
                "              <p style='margin: 0 0 10px 0; font-size: 13px; color: #6b7280;'>" +
                "                • Mã OTP có hiệu lực trong vòng <strong>" + expirationMinutes + " phút</strong>.<br>" +
                "                • Tuyệt đối <strong>không chia sẻ</strong> mã xác thực này cho bất kỳ ai, kể cả nhân viên hệ thống.<br>" +
                "                • Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với ban quản trị để bảo vệ tài khoản." +
                "              </p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style='background-color: #f9fafb; padding: 20px 30px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6;'>" +
                "              <p style='margin: 0;'>© 2026 Soccer365 Football Pitch Management System. All rights reserved.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    // Setter methods for unit testing
    void setBrevoApiKey(String brevoApiKey) {
        this.brevoApiKey = brevoApiKey;
    }

    void setBrevoApiUrl(String brevoApiUrl) {
        this.brevoApiUrl = brevoApiUrl;
    }

    void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
