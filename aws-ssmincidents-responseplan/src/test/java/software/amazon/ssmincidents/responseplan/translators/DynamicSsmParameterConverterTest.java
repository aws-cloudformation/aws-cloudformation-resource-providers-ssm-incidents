package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.ssmincidents.responseplan.DynamicSsmParameter;
import software.amazon.ssmincidents.responseplan.TestData;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicSsmParameterConverterTest {
    private Converter<
        Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue>,
        Set<DynamicSsmParameter>
        > converter;

    static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(
                TestData.API_DYNAMIC_SSM_PARAMETERS,
                TestData.DYNAMIC_SSM_PARAMETERS
            ),
            Arguments.of(
                TestData.API_DYNAMIC_SSM_PARAMETERS_2,
                TestData.DYNAMIC_SSM_PARAMETERS_2
            ),
            Arguments.of(
                ImmutableMap.of(),
                ImmutableSet.of()
            ),
            Arguments.of(
                null, null
            )
        );
    }

    @BeforeEach
    void setUp() {
        converter = TranslatorFactory.DYNAMIC_SSM_PARAMETERS_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doForward(
        Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue> apiParams,
        Set<DynamicSsmParameter> expectedCfnParameters
    ) {
        assertThat(converter.convert(apiParams)).usingRecursiveComparison().isEqualTo(expectedCfnParameters);
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doBackward(
        Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue> expectedApiParams,
        Set<DynamicSsmParameter> cfnParameters
    ) {
        assertThat(converter.reverse().convert(cfnParameters)).usingRecursiveComparison().isEqualTo(expectedApiParams);
    }
}
