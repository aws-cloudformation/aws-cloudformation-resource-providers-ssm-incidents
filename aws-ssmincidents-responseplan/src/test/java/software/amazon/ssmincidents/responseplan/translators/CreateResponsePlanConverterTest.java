package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest;
import software.amazon.ssmincidents.responseplan.ResourceModel;
import software.amazon.ssmincidents.responseplan.TestData;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CreateResponsePlanConverterTest {

    private Converter<CreateResponsePlanRequest, ResourceModel> createResponsePlanConverter;

    static Stream<Arguments> provideBackwardParameters() {
        return Stream.of(
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest.builder()
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .actions(TestData.API_ACTION)
                    .chatChannel(TestData.API_CHAT_CHANNEL)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE)
                    .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                    .tags(TestData.API_TAGS_1)
                    .build(),
                ResourceModel.builder()
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .actions(ImmutableList.of(TestData.ACTION))
                    .chatChannel(TestData.CHAT_CHANNEL)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                    .tags(TestData.TAGS_1)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest.builder()
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE)
                    .build(),
                ResourceModel.builder()
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest.builder()
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE)
                    .build(),
                ResourceModel.builder()
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest.builder()
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE_1)
                    .build(),
                ResourceModel.builder()
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE_2)
                    .build()
            ),
            Arguments.of(
                null, null
            )
        );
    }

    @BeforeEach
    void setUp() {
        createResponsePlanConverter = TranslatorFactory.CREATE_RESPONSEPLAN_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideBackwardParameters")
    void doBackward(
        software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest apiCreateResponsePlanRequest,
        ResourceModel resourceModel
    ) {
        assertThat(
            createResponsePlanConverter.reverse().convert(resourceModel)
        ).usingRecursiveComparison().isEqualTo(apiCreateResponsePlanRequest);
    }
}
