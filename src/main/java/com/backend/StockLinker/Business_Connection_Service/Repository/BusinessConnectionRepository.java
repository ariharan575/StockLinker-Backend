package com.backend.StockLinker.Business_Connection_Service.Repository;

import com.backend.StockLinker.Business_Connection_Service.Entity.BusinessConnection;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessConnectionRepository extends JpaRepository<BusinessConnection, String> {
    List<BusinessConnection> findByRequesterAndStatus(BusinessProfile requester, String status);
    List<BusinessConnection> findByReceiverAndStatus(BusinessProfile receiver, String status);

    // Check if a connection already exists between two users
    boolean existsByRequesterAndReceiver(BusinessProfile requester, BusinessProfile receiver);

    Optional<BusinessConnection> findByRequesterAndReceiver(BusinessProfile requester, BusinessProfile receiver);
}