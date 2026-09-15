package com.fpms.service.impl;

import com.fpms.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrevoEmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-email:giapit02012005@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Soccer365}")
    private String fromName;

    @Override
    @Async("mailTaskExecutor")
    public void sendOtpEmail(String toEmail, String otpCode, int expirationMinutes) {
        log.info("Đang tiến hành gửi email OTP đặt lại mật khẩu đến: {}", toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("[Soccer365] Mã xác thực đặt lại mật khẩu của bạn");
            helper.setText(buildHtmlContent(otpCode, expirationMinutes), true); // true = hỗ trợ HTML

            mailSender.send(message);
            log.info("Đã gửi email OTP thành công qua Brevo tới: {}", toEmail);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi email qua Brevo: {}", ex.getMessage(), ex);
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
}
