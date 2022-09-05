package software.amazon.ssmincidents.responseplan.translators;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Converter;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.ssmincidents.responseplan.ChatChannel;
import software.amazon.ssmincidents.responseplan.TestData;

class ChatChannelConverterTest {

    private Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> chatChannelConverter;

    static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(
                TestData.API_CHAT_CHANNEL,
                TestData.CHAT_CHANNEL
            ),
            Arguments.of(
                null, null
            )
        );
    }

    @BeforeEach
    void setUp() {
        chatChannelConverter = TranslatorFactory.CHAT_CHANNEL_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doForward(
        software.amazon.awssdk.services.ssmincidents.model.ChatChannel apiChatChannel,
        ChatChannel chatChannel
    ) {
        assertThat(chatChannelConverter.convert(apiChatChannel))
            .usingRecursiveComparison()
            .isEqualTo(chatChannel);
    }

    @Test
    void doForwardEmpty() {
        assertThat(
            chatChannelConverter.convert(software.amazon.awssdk.services.ssmincidents.model.ChatChannel.builder().build())
        ).usingRecursiveComparison()
            .isEqualTo(ChatChannel.builder().chatbotSns(new ArrayList<>()).build());
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doBackward(
        software.amazon.awssdk.services.ssmincidents.model.ChatChannel apiChatChannel,
        ChatChannel chatChannel
    ) {
        assertThat(chatChannelConverter.reverse().convert(chatChannel))
            .usingRecursiveComparison()
            .isEqualTo(apiChatChannel);
    }

    @Test
    void doBackwardEmpty() {
        assertThat(
            chatChannelConverter.reverse().convert(ChatChannel.builder().build())
        ).usingRecursiveComparison()
            .isEqualTo(software.amazon.awssdk.services.ssmincidents.model.ChatChannel.builder().build());
    }
}
