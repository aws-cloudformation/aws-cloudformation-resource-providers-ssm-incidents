package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.ssmincidents.responseplan.Tag;
import software.amazon.ssmincidents.responseplan.TestData;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TagsConverterTest {
    private Converter<Map<String, String>, Set<Tag>> converter;

    static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(
                TestData.API_TAGS_1,
                TestData.TAGS_1
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
        converter = TranslatorFactory.TAGS_CONVERTER;
    }

    @Test()
    void duplicateKeysNotAllowed() {
        Assertions.assertThrows(CfnInvalidRequestException.class, () -> {
            converter.reverse().convert(ImmutableSet.of(new Tag(TestData.TAG_KEY_1, TestData.TAG_VALUE_1), new Tag(TestData.TAG_KEY_1, TestData.TAG_VALUE_2)));
        });
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doForward(Map<String, String> expectedTagMap, Set<Tag> tags) {
        assertThat(converter.convert(expectedTagMap)).usingRecursiveComparison().isEqualTo(tags);
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void doBackward(Map<String, String> expectedTagMap, Set<Tag> tags) {
        assertThat(converter.reverse().convert(tags)).usingRecursiveComparison().isEqualTo(expectedTagMap);
    }
}
