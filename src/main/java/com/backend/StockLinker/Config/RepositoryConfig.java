package com.backend.StockLinker.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.backend.StockLinker.Profile_Service.repository",
        "com.backend.StockLinker.Business_Connection_Service.Repository",
        "com.backend.StockLinker.Global_Request_Service.Repository",
        "com.backend.StockLinker.ProductCatagory_Service.repository",
        "com.backend.StockLinker.Seller_Inventary_Service.Repository",
        "com.backend.StockLinker.SellerProfile_Service.Repository",
        "com.backend.StockLinker.Auth_Service.repository",
        "com.backend.StockLinker.Order_Service.repository",
        "com.backend.StockLinker.Notification_Service.repository"
})
@EnableMongoRepositories(basePackages = {
        "com.backend.StockLinker.Message_Service.repository",
        "com.backend.StockLinker.Audit_Service.Repository"
})
public class RepositoryConfig {
}