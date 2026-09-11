package com.fpms.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Sẽ được tích hợp với UserRepository khi triển khai Tầng Repositories (Mục 9)
        throw new UsernameNotFoundException("Chức năng nạp người dùng từ CSDL sẽ được kết nối ở Mục 9: " + username);
    }

    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        // Sẽ được tích hợp với UserRepository khi triển khai Tầng Repositories (Mục 9)
        throw new UsernameNotFoundException("Chức năng nạp người dùng theo ID từ CSDL sẽ được kết nối ở Mục 9: " + id);
    }
}
