package com.backend.StockLinker.Message_Service.repository;

import com.backend.StockLinker.Message_Service.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    Optional<Conversation> findByConversationCode(String conversationCode);

    Optional<Conversation> findByBuyerIdAndSellerId(String buyerId, String sellerId);

    boolean existsByBuyerIdAndSellerId(String buyerId, String sellerId);

    @Query("SELECT c FROM Conversation c WHERE c.buyerId = :buyerId AND c.buyerDeleted = false")
    Page<Conversation> findActiveForBuyer(@Param("buyerId") String buyerId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.sellerId = :sellerId AND c.sellerDeleted = false")
    Page<Conversation> findActiveForSeller(@Param("sellerId") String sellerId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE (c.buyerId = :userId AND c.buyerDeleted = false) OR (c.sellerId = :userId AND c.sellerDeleted = false)")
    Page<Conversation> findAllActiveForUser(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE (c.buyerId = :userId AND c.buyerDeleted = false AND c.buyerArchived = false) OR (c.sellerId = :userId AND c.sellerDeleted = false AND c.sellerArchived = false)")
    Page<Conversation> findAllActiveNonArchivedForUser(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.buyerId = :userId OR c.sellerId = :userId) AND " +
            "(LOWER(c.buyerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.sellerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.lastMessage) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Conversation> searchForUser(@Param("userId") String userId, @Param("keyword") String keyword, Pageable pageable);

    long countByBuyerIdAndBuyerDeletedFalseAndBuyerUnreadCountGreaterThan(String buyerId, int threshold);

    long countBySellerIdAndSellerDeletedFalseAndSellerUnreadCountGreaterThan(String sellerId, int threshold);
}