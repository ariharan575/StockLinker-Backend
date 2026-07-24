package com.backend.StockLinker.Global_Request_Service.Repository;

import com.backend.StockLinker.Global_Request_Service.Entity.GlobalEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalEnquiryRepository extends JpaRepository<GlobalEnquiry, String> {
}