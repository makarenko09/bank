package com.example.bankcards.transaction.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.bankcards.transaction.domain.ClientAccount;
import com.example.bankcards.transaction.domain.card.Card;
import com.example.bankcards.transaction.domain.card.params.CardId;
import com.example.bankcards.transaction.domain.card.params.CardNumber;
import com.example.bankcards.transaction.domain.card.params.ExpiryDate;

@Component
public class ActivatedCard {

    public void addUserNewCard(ClientAccount client) {
        UUID userId = client.getUserId();
        Card newCard = new Card(CardId.generate(), CardNumber.generate(), ExpiryDate.ofYearsFromNow(4), userId);

        newCard.activate();
        // client.getCards().add(newCard);
        client.getCards().add(newCard);
    }

    // public void activatedCard

}
