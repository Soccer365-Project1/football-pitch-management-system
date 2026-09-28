package com.fpms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO Payload gửi Transactional Email qua Brevo REST API v3
 * Endpoint: POST https://api.brevo.com/v3/smtp/email
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrevoEmailRequest {

    private Sender sender;
    private List<Recipient> to;
    private String subject;
    private String htmlContent;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Sender {
        private String name;
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recipient {
        private String email;
        private String name;

        public Recipient(String email) {
            this.email = email;
        }
    }
}
