package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum Reaction {
    down("bad"), up("good");
    private final String code;
    Reaction (String code){
        this.code = code;
    }
    public String getCode(){ return code;}
}

public class Bot extends TelegramLongPollingBot {
    private static final Map <User, Byte> base = new HashMap<>();
    private static final List <User> ban_list= new ArrayList<>();
    private static boolean first_start = true;

    @Override
//    public String getBotUsername() {return "You_carma_bot";}
    public String getBotUsername() {return "kuhtaaa_test_bot";}

    @Override
//    public String getBotToken() {
//        return "6217574577:AAHP0YAJsMAFwhGonfFrVewu8caXOyaltjk";
//    }
    public String getBotToken() {
        return "6001016808:AAHObLujlgCAew6htSLnvcI-logcirWfQzM";
    }

    @Override
    public void onUpdateReceived(Update update) {
        boolean msg_was_deleted = false;

        if (first_start) {
            sendText(update.getMessage().getChatId(), "Всем привет");
            first_start = false;
        }

//        goodUser( 5765278180L, -1001861341922L);
//        if (update.getMessage().getFrom().getId() == 5765278180L) {
//            badUser(update.getMessage());
//        }

        if(checkBlockList(update.getMessage().getFrom()) && update.getMessage().getChatId() < 0) {
            deleteMessage(update.getMessage());
            msg_was_deleted = true;
        }

        if(!base.containsKey(update.getMessage().getFrom())
                && (!update.getMessage().getFrom().getId().equals(update.getMessage().getChatId()))) {

            sendText(update.getMessage().getChatId(), "user "
                    + update.getMessage().getFrom().getUserName() + " add");
            base.put(update.getMessage().getFrom(), (byte) 10);

            sendText(update.getMessage().getChatId(), "Привет, твой стартовый баланс = 10, сейчас ты " +
                    "будешь добавлен в черный список для разблокировки напиши в личку боту @" + getBotUsername()
                    + " и отправь ему чат группы (вместе со знаком минус)");
            sendText(update.getMessage().getChatId(), update.getMessage().getChatId().toString());

            changeBlockStatus(getUser(update.getMessage()), true);

        } else if (base.containsKey(update.getMessage().getFrom())) {
            if(update.getMessage().getText().equals("-1001861341922")) {
//            if(update.getMessage().getText().equals("-1001978003093")) {
                User tmp = findUserInBlackList(update.getMessage().getFrom());
                if(tmp != null) {
                    changeBlockStatus(tmp, false);
                    sendText(tmp.getId(), "Вы разблочены! :)");
                }
            }
        }

        if (getBalance(update.getMessage().getFrom()) < (byte)(-50)) {
            sendText(update.getMessage().getChatId(), "Упс");
            sendText(update.getMessage().getFrom().getId(), "Ну и карма у вас мешок с костями");
            deleteMessage(update.getMessage());
        }

        if(isReplyMessage(update.getMessage()) && !msg_was_deleted) {
            changeUserBalance(update.getMessage().getReplyToMessage().getFrom()
                    , messageEvaluation(update.getMessage()));

            sendText(update.getMessage().getChatId(), "Balance was change for user "
                    + update.getMessage().getReplyToMessage().getFrom().getUserName());

            sendText(update.getMessage().getChatId(), "Now balance = "
                    + getBalance(update.getMessage().getReplyToMessage().getFrom()));
        }
    }

    private User getUser (Message msg) {
        return msg.getFrom();
    }
    private void changeBlockStatus (User user, boolean status) {
        if(checkBlockList(user) && !status) {
            ban_list.remove(user);
        } else if (!checkBlockList(user) && status) {
            ban_list.add(user);
        }
    }
    private boolean checkBlockList(User user) {
        return ban_list.contains(user);
    }
    private byte getBalance(User user) {
        return base.get(user);
    }
    private void changeUserBalance(User user, byte value) {
        base.put(user, (byte)(base.get(user) + value));
    }

    private boolean isCheat(Message msg) {
        if(msg.getFrom().equals(msg.getReplyToMessage().getFrom())
                && (msg.getText().equals(Reaction.down.getCode())
                || msg.getText().equals(Reaction.up.getCode()))) {

            sendText(msg.getFrom().getId(), "Cheating detected");
            return true;
        } else {
            return false;
        }
    }

    private byte messageEvaluation(Message msg) {
        byte balance_delta = 0;

        if(isCheat(msg) || msg.getText().equals(Reaction.down.getCode())) {
            balance_delta = -10;
        } else if (msg.getText().equals(Reaction.up.getCode())) {
            balance_delta = 10;
        }
        return balance_delta;
    }
    private boolean isReplyMessage(Message msg) {
        return msg.getReplyToMessage() != null;
    }
    public void sendText(Long who, String what){
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMessage(Message msg) {
        try {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(msg.getChatId())
                    .messageId(msg.getMessageId()).build();
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public User findUserInBlackList(User user) {
        for(User usr : ban_list) {
            if(usr.getId().equals(user.getId())) {
                return usr;
            }
        }
        return null;
    }

    private void badUser(Message msg) {
        BanChatMember bcm = BanChatMember.builder()
                .userId(msg.getFrom().getId())
                .chatId(msg.getChatId()).build();
        try {
            System.out.println(execute(bcm));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void goodUser(long user_id, long chat_id) {
        UnbanChatMember bcm = UnbanChatMember.builder()
                .userId(user_id)
                .chatId(chat_id).build();
        try {
            System.out.println(execute(bcm));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}