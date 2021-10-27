package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.ssmincidents.model.ChatChannel;
import software.amazon.awssdk.services.ssmincidents.model.EmptyChatChannel;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.ResourceModel;
import software.amazon.ssmincidents.responseplan.TestData;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateResponsePlanConverterTest {

    private Converter<UpdateResponsePlanRequest, ResourceModel> updateResponsePlanConverter;

    static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest.builder()
                    .arn(TestData.ARN)
                    .actions(TestData.API_ACTION)
                    .chatChannel(TestData.API_CHAT_CHANNEL)
                    .incidentTemplateTitle(TestData.TITLE)
                    .incidentTemplateSummary(TestData.SUMMARY)
                    .incidentTemplateImpact(TestData.IMPACT)
                    .incidentTemplateDedupeString(TestData.DEDUP)
                    .incidentTemplateDedupeString(TestData.DEDUP)
                    .incidentTemplateNotificationTargets(
                        TestData.API_NOTIFICATION_TARGET_ITEM_1,
                        TestData.API_NOTIFICATION_TARGET_ITEM_2
                    )
                    .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .actions(ImmutableList.of(TestData.ACTION))
                    .chatChannel(TestData.CHAT_CHANNEL)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest.builder()
                    .arn(TestData.ARN)
                    .incidentTemplateTitle(TestData.TITLE)
                    .incidentTemplateSummary(TestData.SUMMARY)
                    .incidentTemplateImpact(TestData.IMPACT)
                    .incidentTemplateDedupeString(TestData.DEDUP)
                    .incidentTemplateNotificationTargets(
                        TestData.API_NOTIFICATION_TARGET_ITEM_1,
                        TestData.API_NOTIFICATION_TARGET_ITEM_2
                    )
                    .actions(ImmutableList.of())
                    .chatChannel(ChatChannel.builder().empty(EmptyChatChannel.builder().build()).build())
                    .engagements(ImmutableList.of())
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest.builder()
                    .arn(TestData.ARN)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplateTitle(TestData.TITLE)
                    .incidentTemplateSummary(TestData.SUMMARY)
                    .incidentTemplateImpact(TestData.IMPACT)
                    .incidentTemplateDedupeString(TestData.DEDUP)
                    .incidentTemplateNotificationTargets(
                        TestData.API_NOTIFICATION_TARGET_ITEM_1,
                        TestData.API_NOTIFICATION_TARGET_ITEM_2
                    )
                    .actions(ImmutableList.of())
                    .chatChannel(ChatChannel.builder().empty(EmptyChatChannel.builder().build()).build())
                    .engagements(ImmutableList.of())
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest.builder()
                    .arn(TestData.ARN)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplateTitle(TestData.TITLE)
                    .incidentTemplateSummary(TestData.SUMMARY)
                    .incidentTemplateImpact(TestData.IMPACT)
                    .incidentTemplateDedupeString("")
                    .incidentTemplateNotificationTargets(
                        TestData.API_NOTIFICATION_TARGET_ITEM_1,
                        TestData.API_NOTIFICATION_TARGET_ITEM_2
                    )
                    .actions(ImmutableList.of())
                    .chatChannel(ChatChannel.builder().empty(EmptyChatChannel.builder().build()).build())
                    .engagements(ImmutableList.of())
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplate(
                        IncidentTemplate.builder()
                            .title(TestData.TITLE)
                            .summary(TestData.SUMMARY)
                            .impact(TestData.IMPACT)
                            .notificationTargets(
                                ImmutableList.of(
                                    TestData.NOTIFICATION_TARGET_ITEM_1, TestData.NOTIFICATION_TARGET_ITEM_2
                                )
                            )
                            .build()
                    )
                    .build()
            ),
            Arguments.of(
                null, null
            )
        );
    }

    @BeforeEach
    void setUp() {
        updateResponsePlanConverter = TranslatorFactory.UPDATE_RESPONSEPLAN_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doBackward(
        software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest apiUpdateResponsePlanRequest,
        ResourceModel resourceModel
    ) {
        assertThat(
            updateResponsePlanConverter.reverse().convert(resourceModel)
        ).usingRecursiveComparison().isEqualTo(apiUpdateResponsePlanRequest);
    }
}
