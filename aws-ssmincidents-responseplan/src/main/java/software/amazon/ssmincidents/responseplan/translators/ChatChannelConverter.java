package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import software.amazon.ssmincidents.responseplan.ChatChannel;

public class ChatChannelConverter extends
    Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> {

    @Override
    protected ChatChannel doForward(software.amazon.awssdk.services.ssmincidents.model.ChatChannel chatChannel) {
        return ChatChannel.builder().chatbotSns(chatChannel.chatbotSns()).build();
    }

    @Override
    protected software.amazon.awssdk.services.ssmincidents.model.ChatChannel doBackward(ChatChannel chatChannel) {
        return software.amazon.awssdk.services.ssmincidents.model.ChatChannel.builder()
            .chatbotSns(chatChannel.getChatbotSns())
            .build();
    }
}
