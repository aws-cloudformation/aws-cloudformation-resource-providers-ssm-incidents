package software.amazon.ssmincidents.responseplan.translators;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Converter;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.ssmincidents.model.SsmTargetAccount;
import software.amazon.ssmincidents.responseplan.Action;
import software.amazon.ssmincidents.responseplan.SsmAutomation;
import software.amazon.ssmincidents.responseplan.TestData;

class ActionConverterTest {

  private Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter;

  static Stream<Arguments> provideForwardParameters() {
    return Stream.of(
        Arguments.of(
            software.amazon.awssdk.services.ssmincidents.model.Action.builder()
                .ssmAutomation(software.amazon.awssdk.services.ssmincidents.model.SsmAutomation
                    .builder()
                    .documentName(TestData.SSM_DOC)
                    .documentVersion(TestData.SSM_VERSION)
                    .roleArn(TestData.SSM_ROLE)
                    .build())
                .build(),
            Action.builder()
                .ssmAutomation(SsmAutomation.builder()
                    .documentName(TestData.SSM_DOC)
                    .documentVersion(TestData.SSM_VERSION)
                    .roleArn(TestData.SSM_ROLE)
                    .parameters(new HashSet<>())
                    .build())
                .build()
        ),
        Arguments.of(
            TestData.API_ACTION_1,
            TestData.ACTION_1
        ),
        Arguments.of(
            software.amazon.awssdk.services.ssmincidents.model.Action.builder()
                .ssmAutomation(software.amazon.awssdk.services.ssmincidents.model.SsmAutomation.builder()
                    .documentName(TestData.SSM_DOC)
                    .roleArn(TestData.SSM_ROLE)
                    .build()
                )
                .build(),
            Action.builder()
                .ssmAutomation(
                    SsmAutomation.builder()
                        .documentName(TestData.SSM_DOC)
                        .roleArn(TestData.SSM_ROLE)
                        .parameters(new HashSet<>())
                        .build())
                .build()
        ),
        Arguments.of(
            software.amazon.awssdk.services.ssmincidents.model.Action.builder().build(),
            Action.builder().build()
        ),
        Arguments.of(
            null, null
        )
    );
  }

  static Stream<Arguments> provideBackwardParameters() {
    return Stream.of(
        Arguments.of(
            TestData.API_ACTION,
            TestData.ACTION
        ),
        Arguments.of(
            TestData.API_ACTION_1,
            TestData.ACTION_1
        ),
        Arguments.of(
            software.amazon.awssdk.services.ssmincidents.model.Action.builder()
                .ssmAutomation(software.amazon.awssdk.services.ssmincidents.model.SsmAutomation.builder()
                    .documentName(TestData.SSM_DOC)
                    .roleArn(TestData.SSM_ROLE)
                    .targetAccount(SsmTargetAccount.RESPONSE_PLAN_OWNER_ACCOUNT)
                    .build()
                )
                .build(),
            Action.builder()
                .ssmAutomation(
                    SsmAutomation.builder()
                        .documentName(TestData.SSM_DOC)
                        .roleArn(TestData.SSM_ROLE)
                        .targetAccount("RESPONSE_PLAN_OWNER_ACCOUNT")
                        .build())
                .build()
        ),
        Arguments.of(
            software.amazon.awssdk.services.ssmincidents.model.Action.builder().build(),
            Action.builder().build()
        ),
        Arguments.of(
            null, null
        )
    );
  }

  @BeforeEach
  public void setup() {
    actionConverter = TranslatorFactory.ACTION_CONVERTER;
  }

  @ParameterizedTest
  @MethodSource("provideForwardParameters")
  void doForward(software.amazon.awssdk.services.ssmincidents.model.Action apiAction, Action expectedAction) {
    assertThat(actionConverter.convert(apiAction))
        .usingRecursiveComparison()
        .isEqualTo(expectedAction);
  }

  @ParameterizedTest
  @MethodSource("provideBackwardParameters")
  void doBackward(software.amazon.awssdk.services.ssmincidents.model.Action expectedApiAction, Action action) {
    assertThat(actionConverter.reverse().convert(action))
        .usingRecursiveComparison()
        .isEqualTo(expectedApiAction);
  }
}
