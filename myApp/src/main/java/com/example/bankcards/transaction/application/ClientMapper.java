package com.example.bankcards.transaction.application;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.dto.ClientAccountWithCard;
import com.example.bankcards.transaction.infrastructure.secondary.CardRepository;
import com.example.bankcards.transaction.infrastructure.secondary.ClientAccountRepository;

@Component
public class ClientMapper {

    private final ClientAccountRepository repository;
    private final CardRepository cardRepository;

    @Autowired
    public ClientMapper(ClientAccountRepository repository, CardRepository cardRepository) {
        this.repository = repository;
        this.cardRepository = cardRepository;
    }

    public Function<ClientAccount, ClientAccountWithCard> fromClientToClientWithCards() {
        return client -> {
            Set<Card> allByClientId = cardRepository.findAllByClientId(client.getUserId()).stream()
                    .collect(Collectors.toSet());

            return new ClientAccountWithCard(
                    client.getUserId(),
                    client.getOwnerName(),
                    allByClientId);
        };
    }
}
