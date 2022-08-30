package software.amazon.ssmincidents.responseplan.translators;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.ResourceModel;
import software.amazon.ssmincidents.responseplan.TestData;

class GetResponsePlanConverterTest {

    private Converter<GetResponsePlanResponse, ResourceModel> getResponsePlanConverter;

    static Stream<Arguments> provideForwardParameters() {
        return Stream.of(
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .actions(TestData.API_ACTION)
                    .chatChannel(TestData.API_CHAT_CHANNEL)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE)
                    .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .actions(ImmutableList.of(TestData.ACTION_EMPTY_SSM_PARAMETERS))
                    .chatChannel(TestData.CHAT_CHANNEL)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE)
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .actions(new ArrayList<>())
                    .engagements(new HashSet<>())
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse.builder()
                    .arn(TestData.ARN)
                    .displayName(TestData.DISPLAY_NAME)
                    .name(TestData.NAME)
                    .incidentTemplate(TestData.API_INCIDENT_TEMPLATE)
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .displayName(TestData.DISPLAY_NAME)
                    .incidentTemplate(TestData.INCIDENT_TEMPLATE)
                    .actions(new ArrayList<>())
                    .engagements(new HashSet<>())
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .incidentTemplate(
                        software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                            .title(TestData.TITLE)
                            .impact(TestData.IMPACT)
                            .build()
                    )
                    .build(),
                ResourceModel.builder()
                    .arn(TestData.ARN)
                    .name(TestData.NAME)
                    .incidentTemplate(
                        IncidentTemplate.builder()
                            .title(TestData.TITLE)
                            .impact(TestData.IMPACT)
                            .build()
                    )
                    .actions(new ArrayList<>())
                    .engagements(new HashSet<>())
                    .build()
            ),
            Arguments.of(
                null, null
            )
        );
    }

    @BeforeEach
    void setUp() {
        getResponsePlanConverter = TranslatorFactory.GET_RESPONSEPLAN_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideForwardParameters")
    void doForward(
        software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse apiGetResponsePlanResult,
        ResourceModel resourceModel
    ) {
        assertThat(getResponsePlanConverter.convert(apiGetResponsePlanResult))
            .usingRecursiveComparison()
            .isEqualTo(resourceModel);
    }
}
