package com.backend.StockLinker.AuthService.config;

import com.backend.StockLinker.AuthService.mapper.DeviceMapper;
import com.backend.StockLinker.AuthService.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    // =========================================================
    // 🏗️ MAPPER BEANS CONFIGURATION
    // =========================================================

    @Bean
    public UserMapper userMapper() {
        return new UserMapper();
    }

    @Bean
    public DeviceMapper deviceMapper() {
        return new DeviceMapper();
    }
}