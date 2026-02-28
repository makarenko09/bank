package com.example.bankcards.transaction.infrastructure.secondary;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.transaction.domain.card.Card;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class CardTransactional {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void insertCard(Card card) {
        entityManager.createNativeQuery("insert into cards (id, balance, number, user_id)")

                .setParameter(1, card.getId())
                .setParameter(2, card.getBalance())
                .setParameter(3, card.getNumber())
                .setParameter(4, card.getOwnerId())
                .executeUpdate();
    }

    // List<CardEntity> findByOwnerIdAndStatus(UUID userId, CardStatus status);

    // // ✅ Кастомный JPQL-запрос: активные карты пользователя с балансом > 0
    // @Query("SELECT c FROM CardEntity c WHERE c.owner.id = :userId AND c.status

    // 'ACTIVE' AND c.balance > 0")
    // List<CardEntity> findActiveCardsWithPositiveBalance(@Param("userId") UUID
    // userId);

    // // ✅ Native SQL-запрос (если нужна специфика БД)
    // @Query(value = "SELECT * FROM cards WHERE user_id = :userId AND status =
    // 'ACTIVE'", nativeQuery = true)
    // List<CardEntity> findActiveCardsNative(@Param("userId") UUID userId);

    // // ✅ Пагинация + сортировка
    // @Query("SELECT c FROM CardEntity c WHERE c.owner.id = :userId")
    // Page<CardEntity> findByOwnerId(@Param("userId") UUID userId, Pageable
    // pageable);
}
