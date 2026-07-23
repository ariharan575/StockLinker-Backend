package com.backend.StockLinker.MessageService.repository;

import com.backend.StockLinker.MessageService.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByConversationCode(String conversationCode);

    Optional<Conversation> findByBuyerIdAndSellerId(String buyerId, String sellerId);

    boolean existsByBuyerIdAndSellerId(String buyerId, String sellerId);

    @Query("{ 'buyerId': ?0, 'buyerDeleted': false }")
    Page<Conversation> findActiveForBuyer(String buyerId, Pageable pageable);

    @Query("{ 'sellerId': ?0, 'sellerDeleted': false }")
    Page<Conversation> findActiveForSeller(String sellerId, Pageable pageable);

    @Query("{ '$or': [ { 'buyerId': ?0, 'buyerDeleted': false }, { 'sellerId': ?0, 'sellerDeleted': false } ] }")
    Page<Conversation> findAllActiveForUser(String userId, Pageable pageable);

    @Query("{ '$or': [ { 'buyerId': ?0, 'buyerDeleted': false, 'buyerArchived': false }, { 'sellerId': ?0, 'sellerDeleted': false, 'sellerArchived': false } ] }")
    Page<Conversation> findAllActiveNonArchivedForUser(String userId, Pageable pageable);

    @Query("{ '$or': [ { 'buyerId': ?0 }, { 'sellerId': ?0 } ], " +
            "'$and': [ { '$or': [ { 'buyerName': { '$regex': ?1, '$options': 'i' } }, " +
            "{ 'sellerName': { '$regex': ?1, '$options': 'i' } }, " +
            "{ 'lastMessage': { '$regex': ?1, '$options': 'i' } } ] } ] }")
    Page<Conversation> searchForUser(String userId, String keywordRegex, Pageable pageable);

    long countByBuyerIdAndBuyerDeletedFalseAndBuyerUnreadCountGreaterThan(String buyerId, int threshold);

    long countBySellerIdAndSellerDeletedFalseAndSellerUnreadCountGreaterThan(String sellerId, int threshold);
}