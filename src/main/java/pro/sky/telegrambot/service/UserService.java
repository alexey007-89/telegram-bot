package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.model.Location;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.UserProperties;
import pro.sky.telegrambot.repository.UserPropertiesRepository;

import java.time.ZoneId;

@Service
public class UserService {
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserPropertiesRepository repository;

    public UserService(UserPropertiesRepository repository) {
        this.repository = repository;
    }

    public ZoneId getTimeZone(long chatId) {
        UserProperties userProperties = repository.getUserPropertiesByChatId(chatId);
        if (userProperties != null) {
            logger.info("Getting User TZ for {}", userProperties);
            TimeZoneEngine engine = TimeZoneEngine.initialize();
            return engine.query(userProperties.getLatitude(), userProperties.getLongitude()).orElseThrow();
        } else {
            return ZoneId.of("UTC");
        }

    }

    public void saveUserLocation(long chatId, Location location) {
        UserProperties userProperties = repository.getUserPropertiesByChatId(chatId);
        if (userProperties == null) {
            userProperties = new UserProperties();
        }
        userProperties.setChatId(chatId);
        userProperties.setLatitude(location.latitude());
        userProperties.setLongitude(location.longitude());
        logger.info("Saving User location for {}", userProperties);
        repository.save(userProperties);
    }
}
