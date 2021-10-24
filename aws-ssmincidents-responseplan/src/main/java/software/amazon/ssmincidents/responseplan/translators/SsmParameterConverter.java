package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import software.amazon.ssmincidents.responseplan.SsmParameter;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SsmParameterConverter extends Converter<Map<String, List<String>>, Set<SsmParameter>> {

    private static Stream<Entry<String, String>> toFlatKeyValue(SsmParameter ssmParameter) {
        return ssmParameter.getValues().stream().map(x -> new SimpleEntry<>(ssmParameter.getKey(), x));
    }

    @Override
    protected Set<SsmParameter> doForward(Map<String, List<String>> stringListMap) {
        return stringListMap.entrySet().stream()
            .map(entry -> SsmParameter.builder().key(entry.getKey()).values(entry.getValue()).build())
            .collect(Collectors.toSet());
    }

    @Override
    protected Map<String, List<String>> doBackward(Set<SsmParameter> ssmParameters) {
        return ssmParameters.stream()
            .flatMap(SsmParameterConverter::toFlatKeyValue)
            .collect(
                Collectors.groupingBy(
                    Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toList())
                )
            );
    }
}
