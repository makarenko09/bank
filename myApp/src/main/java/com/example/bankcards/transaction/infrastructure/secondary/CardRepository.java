package com.example.bankcards.transaction.infrastructure.secondary;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import com.example.bankcards.transaction.domain.card.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    @NativeQuery("SELECT * FROM public.card AS c WHERE c.account_id = ?1")
    Collection<Card> findAllByClientId(UUID account_id);
}