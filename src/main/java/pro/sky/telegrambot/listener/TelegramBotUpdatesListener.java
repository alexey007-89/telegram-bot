package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository repository;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository repository) {
        this.telegramBot = telegramBot;
        this.repository = repository;
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
            String textMessage = update.message().text();
            long chatId = update.message().chat().id();
            if (textMessage.equals("/start")) {
                String firstName = update.message().from().firstName();
                telegramBot.execute(new SendMessage(chatId, "<b>Привет " + firstName + "!</b>\n" +
                        "<i>Я бот для создания напоминаний. Отправь мне сообщение в формате <u>ДД.ММ.ГГГГ ЧЧ.ММ Моё напоминание</u> и я напомню тебе в нужное время.</i>").parseMode(ParseMode.HTML));
            }
            Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
            Matcher matcher = pattern.matcher(textMessage);
            if (matcher.matches()) {
                LocalDateTime date = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                String item = matcher.group(3);
                NotificationTask task = new NotificationTask();
                task.setChatId(chatId);
                task.setMessage(item);
                task.setTime(date);
                logger.info("Saving {} to db",task);
                repository.save(task);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void getTasks() {
        List<NotificationTask> tasks = repository.findByTimeEquals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        if (!tasks.isEmpty()) {
            tasks.forEach(notificationTask -> {
                telegramBot.execute(new SendMessage(notificationTask.getChatId(), notificationTask.getMessage()));
                logger.info("Sending scheduled message");
            });
        }
    }

}
