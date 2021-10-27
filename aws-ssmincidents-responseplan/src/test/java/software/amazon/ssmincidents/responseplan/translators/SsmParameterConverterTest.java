package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.ssmincidents.responseplan.SsmParameter;
import software.amazon.ssmincidents.responseplan.TestData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SsmParameterConverterTest {

    private Converter<Map<String, List<String>>, Set<SsmParameter>> converter;

    static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(
                TestData.API_SSM_PARAMETERS,
                TestData.SSM_PARAMETERS
            ),
            Arguments.of(
                TestData.API_SSM_PARAMETERS_1,
                TestData.SSM_PARAMETERS_1
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
        converter = TranslatorFactory.SSM_PARAMETERS_CONVERTER;
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doForward(Map<String, List<String>> listMap, Set<SsmParameter> expectedSsmParameters) {
        assertThat(converter.convert(listMap)).usingRecursiveComparison().isEqualTo(expectedSsmParameters);
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doBackward(Map<String, List<String>> expectedListMap, Set<SsmParameter> ssmParameters) {
        assertThat(converter.reverse().convert(ssmParameters)).usingRecursiveComparison().isEqualTo(expectedListMap);
    }
}
