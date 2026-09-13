package com.fpms.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

/**
 * Interface xác thực Google ID Token với máy chủ xác thực của Google
 */
public interface GoogleTokenVerifier {

    /**
     * Xác thực tính hợp lệ và chữ ký số của Google ID Token
     *
     * @param idToken chuỗi ID Token nhận được từ Google Identity Services trên Client
     * @return GoogleIdToken.Payload chứa thông tin người dùng được Google xác nhận
     */
    GoogleIdToken.Payload verify(String idToken);
}
