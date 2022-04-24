package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Location;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import liquibase.exception.DateParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.UserService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository repository;
    private final UserService service;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository repository, UserService service) {
        this.telegramBot = telegramBot;
        this.repository = repository;
        this.service = service;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // Process your updates here
            try {
                long chatId = update.message().chat().id();
                String textMessage = update.message().text();
                Location userLocation = update.message().location();
                if (textMessage != null && textMessage.equals("/start")) {
                    String firstName = update.message().from().firstName();
                    telegramBot.execute(startMessage(chatId, firstName));
                }
                if (userLocation != null) {
                    service.saveUserLocation(chatId,userLocation);
                }
                if (textMessage != null) {
                    try {
                        getMatcher(chatId, textMessage);
                    } catch (DateTimeParseException e) {
                        telegramBot.execute(new SendMessage(chatId, "Я Вас не понимаю попробуйте снова"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void getMatcher(long chatId, String textMessage) {
        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
        Matcher matcher = pattern.matcher(textMessage);
        if (matcher.matches()) {
            LocalDateTime date = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            date = convertToUtc(service.getTimeZone(chatId), date);
            String item = matcher.group(3);
            NotificationTask task = new NotificationTask();
            task.setChatId(chatId);
            task.setMessage(item);
            task.setTime(date);
            logger.info("Saving {} to db",task);
            repository.save(task);
        }
    }

    private SendMessage startMessage(long chatId, String firstName) {
        SendMessage startMessage = new SendMessage(chatId, "<b>Привет " + firstName + "!</b>\n" +
                "<i>Я бот для создания напоминаний.\n</i>" +
                "<i> Отправь мне сообщение в формате <u>ДД.ММ.ГГГГ ЧЧ:ММ Моё напоминание</u> и я напомню тебе в нужное время. </i>" +
                "<i> Также мне необходимо твоё местоположение, чтобы правильно определить твой часовой пояс.</i>");
        startMessage.parseMode(ParseMode.HTML);
        Keyboard keyboard = new ReplyKeyboardMarkup(
                new KeyboardButton("Отправить моё местоположение").requestLocation(true)).resizeKeyboard(true);
        startMessage.replyMarkup(keyboard);
        return startMessage;
    }

    private LocalDateTime convertToUtc(ZoneId zoneId, LocalDateTime time) {
        return time.atZone(zoneId).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void getTasks() {
        LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        time = convertToUtc(ZoneId.systemDefault(), time);
        List<NotificationTask> tasks = repository.findByTimeEquals(time);
        if (!tasks.isEmpty()) {
            tasks.forEach(notificationTask -> {
                telegramBot.execute(new SendMessage(notificationTask.getChatId(), notificationTask.getMessage()));
                logger.info("Sending scheduled message");
            });
        }
    }

}
