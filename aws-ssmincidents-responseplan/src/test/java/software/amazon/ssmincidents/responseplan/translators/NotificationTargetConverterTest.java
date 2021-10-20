package software.amazon.ssmincidents.responseplan.translators;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Converter;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.ssmincidents.responseplan.NotificationTargetItem;
import software.amazon.ssmincidents.responseplan.TestData;

class NotificationTargetConverterTest {

  private Converter<software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem, NotificationTargetItem> notificationTargetConverter;

  static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(
            TestData.API_NOTIFICATION_TARGET_ITEM_1,
            TestData.NOTIFICATION_TARGET_ITEM_1
        ),
        Arguments.of(
            software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem.builder().build(),
            NotificationTargetItem.builder().build()
        ),
        Arguments.of(
            null, null
        )
    );
  }

  @BeforeEach
  void setUp() {
    notificationTargetConverter = TranslatorFactory.NOTIFICATION_TARGET_CONVERTER;
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void doForward(
      software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem apiNotificationTargetItem,
      NotificationTargetItem notificationTargetItem
  ) {
    assertThat(
        notificationTargetConverter.convert(apiNotificationTargetItem)
    ).usingRecursiveComparison().isEqualTo(notificationTargetItem);
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void doBackward(
      software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem apiNotificationTargetItem,
      NotificationTargetItem notificationTargetItem
  ) {
    assertThat(
        notificationTargetConverter.reverse().convert(notificationTargetItem)
    ).usingRecursiveComparison().isEqualTo(apiNotificationTargetItem);
  }
}
