package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import software.amazon.ssmincidents.responseplan.DynamicSsmParameterValue;
import software.amazon.ssmincidents.responseplan.DynamicSsmParameter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DynamicSsmParameterConverter extends Converter<
    Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue>,
    Set<DynamicSsmParameter>
    > {
    @Override
    protected Set<DynamicSsmParameter> doForward(
        Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue> parameters
    ) {
        return parameters.entrySet()
            .stream()
            .map(entry -> DynamicSsmParameter.builder()
                .key(entry.getKey())
                .value(DynamicSsmParameterValue.builder().variable(entry.getValue().variableAsString()).build())
                .build())
            .collect(Collectors.toSet());
    }

    @Override
    protected Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue> doBackward(
        Set<DynamicSsmParameter> dynamicSsmParameters
    ) {
        Map<String, software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue> map = new HashMap<>();
        dynamicSsmParameters.forEach(param -> map.put(
            param.getKey(),
            software.amazon.awssdk.services.ssmincidents.model.DynamicSsmParameterValue.builder()
                .variable(param.getValue().getVariable())
                .build()
        ));
        return map;
    }
}
