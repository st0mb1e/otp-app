package ru.yartsev_vladislav.otp_app.service.notification;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

import java.time.format.DateTimeFormatter;

/**
 * Рассылает OTP-код пользователю через Telegram-бота.
 * Для отправки используется telegrambots-client, для приёма входящих
 * сообщений - telegrambots-longpolling
 *
 * Бот не может написать пользователю первым, поэтому я повесил на любые входящие
 * сообщения ответ с chat id - пользователь сам делает /start, узнаёт свой id
 * и потом передаёт его как destination при генерации OTP
 *
 * Если в настройках токен не задан, sender выключен: бин создаётся (чтобы
 * приложение поднялось), но при попытке что-то отправить через TELEGRAM
 * сразу прилетает исключение.
 */
@Component
public class TelegramOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramOtpNotificationSender.class);

    private final boolean enabled;
    private final TelegramClient telegramClient;
    private final TelegramBotsLongPollingApplication botsApplication;

    public TelegramOtpNotificationSender(@Value("${app.telegram.bot.token:}") String botToken) {
        String token = botToken == null ? "" : botToken.trim();
        this.enabled = !token.isEmpty();

        if (!enabled) {
            this.telegramClient = null;
            this.botsApplication = null;
            log.warn("Telegram sender is off: app.telegram.bot.token is not set, "
                    + "TELEGRAM channel won't work");
            return;
        }

        this.telegramClient = new OkHttpTelegramClient(token);
        this.botsApplication = new TelegramBotsLongPollingApplication();
        try {
            this.botsApplication.registerBot(token, new ChatIdEchoBot(telegramClient));
        } catch (TelegramApiException ex) {
            throw new IllegalStateException("could not register Telegram bot for long polling", ex);
        }
        log.info("Telegram sender started, long polling is running");
    }

    @PreDestroy
    void shutdown() {
        if (botsApplication == null) {
            return;
        }
        try {
            botsApplication.stop();
        } catch (TelegramApiException ex) {
            log.warn("could not stop TelegramBotsLongPollingApplication", ex);
        }
        try {
            botsApplication.close();
            log.info("TelegramBotsLongPollingApplication closed");
        } catch (Exception ex) {
            log.warn("could not close TelegramBotsLongPollingApplication", ex);
        }
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.TELEGRAM;
    }

    @Override
    public void send(NotificationPayload payload) {
        if (!enabled) {
            throw new IllegalStateException(
                    "TELEGRAM channel is not configured, set app.telegram.bot.token");
        }
        Long chatId = parseChatId(payload.destination());
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(buildBody(payload))
                .build();
        try {
            telegramClient.execute(message);
            log.info("OTP sent via Telegram, chatId={} userId={} operationId={}",
                    chatId, payload.userId(), payload.operationId());
        } catch (TelegramApiException ex) {
            throw new IllegalStateException(
                    "could not send OTP via Telegram (chatId=" + chatId + ")", ex);
        }
    }

    private static Long parseChatId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "destination is required for TELEGRAM, must be a numeric chat id");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "destination for TELEGRAM must be a numeric chat id, got: " + raw, ex);
        }
    }

    private static String buildBody(NotificationPayload p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your verification code: ").append(p.code()).append('\n');
        if (p.operationId() != null && !p.operationId().isBlank()) {
            sb.append("Operation: ").append(p.operationId()).append('\n');
        }
        if (p.expiresAt() != null) {
            sb.append("Expires at: ")
                    .append(DateTimeFormatter.ISO_INSTANT.format(p.expiresAt()))
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * Консьюмер для long polling. Делает одно:
     * на любое входящее сообщение отвечает текущим chat id, чтобы пользователь
     * скопировал его и потом передал как destination при генерации OTP.
     */
    private static final class ChatIdEchoBot implements LongPollingSingleThreadUpdateConsumer {

        private static final Logger botLog = LoggerFactory.getLogger(ChatIdEchoBot.class);

        private final TelegramClient client;

        private ChatIdEchoBot(TelegramClient client) {
            this.client = client;
        }

        @Override
        public void consume(Update update) {
            if (!update.hasMessage()) {
                return;
            }
            Long chatId = update.getMessage().getChatId();
            if (chatId == null) {
                return;
            }
            SendMessage reply = SendMessage.builder()
                    .chatId(chatId)
                    .text("Your chat id is: " + chatId
                            + "\nUse it as destination when generating an OTP via Telegram.")
                    .build();
            try {
                client.execute(reply);
            } catch (TelegramApiException ex) {
                botLog.warn("could not reply to incoming message, chatId={}", chatId, ex);
            }
        }
    }
}
