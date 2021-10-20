package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.ssmincidents.responseplan.Tag;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TagsConverter extends Converter<Map<String, String>, Set<Tag>> {
    @Override
    protected Set<Tag> doForward(Map<String, String> sdkTagMap) {
        return sdkTagMap.entrySet().stream().map(x -> new Tag(x.getKey(), x.getValue())).collect(Collectors.toSet());
    }

    @Override
    protected Map<String, String> doBackward(Set<Tag> tags) {
        try {
            return tags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        } catch (Exception e) {
            throw new CfnInvalidRequestException("duplicate tag keys");
        }
    }
}
