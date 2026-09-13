package com.fpms.security.impl;

import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.security.GoogleTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            )
            .setAudience(Collections.singletonList(googleClientId))
            .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                log.error("Google ID Token không hợp lệ hoặc không khớp Client ID");
                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
            }

            return googleIdToken.getPayload();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi trong quá trình xác thực Google ID Token: ", e);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
