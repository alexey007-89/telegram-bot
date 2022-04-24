package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.UserProperties;

public interface UserPropertiesRepository extends JpaRepository<UserProperties,Long> {

    UserProperties getUserPropertiesByChatId(Long chatId);
}


