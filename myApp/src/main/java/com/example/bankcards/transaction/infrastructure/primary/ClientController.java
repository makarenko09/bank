// package com.example.bankcards.transaction.infrastructure.primary;

// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.bankcards.shared.authentication.domain.Username;
// import com.example.bankcards.transaction.application.TransactionActivities;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestParam;

// @RestController
// @RequestMapping("/public/")
// public class ClientController {
// private final TransactionActivities activities;

// public ClientController(TransactionActivities activities) {
// this.activities = activities;
// }

// @GetMapping("/iam")
// public String getMethodName(@RequestParam String param) {
// return new String();
// }

// public Username getPrincipalUser() {
// return activities.getPrincipalUser();
// }

// // FIXME: Согласно ТЗ ("Бизнес-логика только на бэкенде", "DTO и REST"), вы
// не
// // должны отдавать сущности JPA напрямую в контроллер. Поэтому навигация
// // card.getUser().getName() внутри контроллера не нужна — вы берете имя
// // пользователя из токена JWT или загружаете отдельно.
// }
