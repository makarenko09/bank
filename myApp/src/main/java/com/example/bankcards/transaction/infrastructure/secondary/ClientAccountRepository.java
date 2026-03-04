package com.example.bankcards.transaction.infrastructure.secondary;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bankcards.transaction.domain.ClientAccount;

@Repository
public interface ClientAccountRepository extends JpaRepository<ClientAccount, UUID> {

}