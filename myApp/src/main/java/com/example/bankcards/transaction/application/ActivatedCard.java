package com.example.bankcards.transaction.application;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;

@Component
public class ActivatedCard {

    public Card addUserNewCard(ClientAccount client) {
        Assert.notNull(client, "Client account must not be null");
        Assert.notNull(client.getUserId(), "User ID must not be null");

        Card activatedCard = Card.CardBuilder(client.getUserId());

        boolean isDuplicate = client.getCards().stream()
                .anyMatch(c -> Objects.equals(activatedCard.getId(), c.getId()) ||
                        Objects.equals(activatedCard.getNumber(), c.getNumber()));

        Assert.isTrue(!isDuplicate, "Card with this ID or Number already exists");

        client.getCards().add(activatedCard.activate());

        return activatedCard;
    }
}
