package software.amazon.ssmincidents.responseplan.translators;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Converter;

import java.util.ArrayList;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.TestData;

class IncidentTemplateConverterTest {

    private Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter;

    static Stream<Arguments> provideForwardParameters() {
        return Stream.of(
            Arguments.of(
                TestData.API_INCIDENT_TEMPLATE,
                TestData.INCIDENT_TEMPLATE
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .summary(TestData.SUMMARY)
                    .impact(TestData.IMPACT)
                    .dedupeString(TestData.DEDUP)
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .summary(TestData.SUMMARY)
                    .impact(TestData.IMPACT)
                    .dedupeString(TestData.DEDUP)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .summary("")
                    .dedupeString("")
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .incidentTags(ImmutableMap.of())
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .incidentTags(TestData.API_TAGS_1)
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .incidentTags(TestData.TAGS_1)
                    .build()
            ),
            Arguments.of(
                null, null
            )
        );
    }

    static Stream<Arguments> provideBackwardParameters() {
        return Stream.of(
            Arguments.of(
                TestData.API_INCIDENT_TEMPLATE,
                TestData.INCIDENT_TEMPLATE
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .summary(TestData.SUMMARY)
                    .impact(TestData.IMPACT)
                    .dedupeString(TestData.DEDUP)
                    .notificationTargets(new ArrayList<>())
                    .incidentTags(ImmutableMap.of())
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .summary(TestData.SUMMARY)
                    .impact(TestData.IMPACT)
                    .dedupeString(TestData.DEDUP)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .summary("")
                    .dedupeString("")
                    .notificationTargets(new ArrayList<>())
                    .incidentTags(ImmutableMap.of())
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .build()
            ),
            Arguments.of(
                software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .summary("")
                    .dedupeString("")
                    .notificationTargets(ImmutableList.of(TestData.API_NOTIFICATION_TARGET_ITEM_1, TestData.API_NOTIFICATION_TARGET_ITEM_2))
                    .incidentTags(TestData.API_TAGS_1)
                    .build(),
                IncidentTemplate.builder()
                    .title(TestData.TITLE)
                    .impact(TestData.IMPACT)
                    .notificationTargets(ImmutableList.of(TestData.NOTIFICATION_TARGET_ITEM_1, TestData.NOTIFICATION_TARGET_ITEM_2))
                    .incidentTags(TestData.TAGS_1)
                    .build()
            ),
            Arguments.of(
                null, null
            )
        );
    }

    @BeforeEach
    void setUp() {
        incidentTemplateConverter = TranslatorFactory.INCIDENT_TEMPLATE_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideForwardParameters")
    void doForward(
        software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate apiIncidentTemplate,
        IncidentTemplate incidentTemplate
    ) {
        assertThat(incidentTemplateConverter.convert(apiIncidentTemplate))
            .usingRecursiveComparison()
            .isEqualTo(incidentTemplate);
    }

    @ParameterizedTest
    @MethodSource("provideBackwardParameters")
    void doBackward(
        software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate apiIncidentTemplate,
        IncidentTemplate incidentTemplate
    ) {
        assertThat(incidentTemplateConverter.reverse().convert(incidentTemplate))
            .usingRecursiveComparison()
            .isEqualTo(apiIncidentTemplate);
    }
}
