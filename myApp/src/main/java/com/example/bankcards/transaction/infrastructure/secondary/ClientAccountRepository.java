package com.example.bankcards.transaction.infrastructure.secondary;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import com.example.bankcards.transaction.domain.ClientAccount;

@Repository
public interface ClientAccountRepository extends JpaRepository<ClientAccount, UUID> {
    @NativeQuery("SELECT * FROM public.client_account WHERE owner_name = ?1")
    ClientAccount findByOwnerName(String owner_name);
}