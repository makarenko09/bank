package com.example.bankcards.transaction.application;

import org.springframework.stereotype.Component;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;

@Component
public class ActivatedCard {

    public void addUserNewCard(ClientAccount client) {
        client.addCard(Card.CardBuilder(client.getUserId()).activate());
    }
}
