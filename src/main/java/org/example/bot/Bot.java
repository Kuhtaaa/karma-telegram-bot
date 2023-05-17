package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

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
    private static final List <User> ban_list = new ArrayList<>();  //  те, которые не могут писать
    private static final List <User> black_list = new ArrayList<>(); //  те, которых забанили и удалили
    private static boolean first_start = true;

    private static final String target = "Я больше так не буду";

    private static Chat global_chat; //  надо подумоть

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
        Message message = update.getMessage();
        Message replay_message = update.getMessage().getReplyToMessage();
        User user = message.getFrom();
        Chat chat = message.getChat();

        if (first_start) {
            global_chat = chat;
            sendText(chat.getId(), "Всем привет");
            first_start = false;
        }
//        try {
//            System.out.println(createChatInviteLink(chat));  //  не работает
//        } catch (TelegramApiRequestException e) {
//            System.out.println("Error");
//        }

//        changeMsg(update.getMessage());
//        goodUser( 5765278180L, -1001861341922L);
//        if (update.getMessage().getFrom().getId() == 5765278180L) {
//            badUser(update.getMessage());
//        }

        if(checkBlockList(user) && chat.getId() < 0) {
            deleteMessage(message);
            msg_was_deleted = true;
        }

        if(!base.containsKey(user) && (!user.getId().equals(chat.getId()))) {
            sendText(chat.getId(), "user " + user.getUserName() + " add");
            base.put(user, (byte) 10);

            sendText(chat.getId(), "Привет, твой стартовый баланс = 10, сейчас ты " +
                    "будешь добавлен в черный список для разблокировки напиши в личку боту @" + getBotUsername()
                    + " и отправь ему чат группы (вместе со знаком минус)");
            sendText(chat.getId(), chat.getId().toString());

            changeBlockStatus(user, true);
        } else if (base.containsKey(user)) {
            if(message.getText().equals(global_chat.getId().toString())) {
//            if(message.getText().equals("-1001861341922")) {
//            if(update.getMessage().getText().equals("-1001978003093")) {
                User tmp_user = findUserInBlackList(user);
                if(tmp_user != null) {
                    changeBlockStatus(tmp_user, false);
                    sendText(tmp_user.getId(), "Вы разблочены! :)");
                }
            }
        }

        if (user.getId().equals(chat.getId()) && black_list.contains(user) && message.getText().equals(target)) {
            black_list.remove(user);
            changeUserBalance(user, (byte)20);
            System.out.println(global_chat);
            unbanUser(user.getId(), global_chat.getId());  //  чучуть хард кода
            sendText(user.getId(), "Вы разблочены! :)");
            sendText(user.getId(), "Ссылка на вход: ");
        }

        checkBalanceState(message);

        if(isReplyMessage(message) && !msg_was_deleted) {
            changeUserBalance(getUser(replay_message), messageEvaluation(message));

            sendText(chat.getId(), "Balance was change for user " + getUser(replay_message).getUserName());
            sendText(chat.getId(), "Now balance = " + getBalance(getUser(replay_message)));
        }
    }

    private User getUser(Message msg) { return msg.getFrom(); }

    private Long getUserId(Message msg) { return msg.getFrom().getId(); }

    //  не работает
    private ExportChatInviteLink createChatInviteLink(Chat chat) {
//        CreateChatInviteLink inviteLink = new CreateChatInviteLink(chat.getId().toString());
        ExportChatInviteLink inviteLink = new ExportChatInviteLink(chat.getId().toString());
        try {
            execute(inviteLink);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        return inviteLink;
    }

    private void changeBlockStatus(User user, boolean status) {
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

    private void checkBalanceState(Message msg) {
        byte balance = getBalance(msg.getFrom());
        if (balance < (byte)(-70)) {
            black_list.add(getUser(msg));
            banUser(msg);
            sendText(getUserId(msg), "Вы были забанены из-за слишком низкой кармы!");
            sendText(getUserId(msg), "Для разблокировки напишите: " + target);
        } else if (balance < (byte)(-50)) {
            sendText(msg.getChatId(), "Упс");
            sendText(getUserId(msg), "Ну и карма у вас мешок с костями");
            deleteMessage(msg);
        }
    }
    
    private void changeUserBalance(User user, byte value) {
        base.put(user, (byte)(base.get(user) + value));
    }

    private boolean isCheat(Message msg) {
        if(msg.getFrom().equals(msg.getReplyToMessage().getFrom())
                && (msg.getText().equals(Reaction.down.getCode())
                || msg.getText().equals(Reaction.up.getCode()))) {

            sendText(getUserId(msg), "Cheating detected");
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

    public User findUserInBlackList(User user) {
        for(User usr : ban_list) {
            if(usr.getId().equals(user.getId())) {
                return usr;
            }
        }
        return null;
    }

    //  можно вынести в другой класс, что-то типо наказания
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
    
    private void banUser(Message msg) {
        BanChatMember bcm = BanChatMember.builder()
                .userId(getUserId(msg))
                .chatId(msg.getChatId()).build();
        try {
            System.out.println(execute(bcm));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void unbanUser(long user_id, long chat_id) {
        UnbanChatMember bcm = UnbanChatMember.builder()
                .userId(user_id)
                .chatId(chat_id).build();
        try {
            System.out.println(execute(bcm));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    //  не работает
    private void changeMsg(Message msg) {
        EditMessageText emt = EditMessageText.builder()
                .chatId(msg.getChatId())
                .messageId(msg.getMessageId())
                .text("Ха, Лох!").build();
        try {
            execute(emt);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}