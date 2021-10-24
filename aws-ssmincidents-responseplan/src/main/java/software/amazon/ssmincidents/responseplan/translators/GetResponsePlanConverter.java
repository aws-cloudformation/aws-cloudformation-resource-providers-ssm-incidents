package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse;
import software.amazon.ssmincidents.responseplan.Action;
import software.amazon.ssmincidents.responseplan.ChatChannel;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.ResourceModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public class GetResponsePlanConverter extends Converter<GetResponsePlanResponse, ResourceModel> {

    Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter;
    Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter;
    Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> chatChannelConverter;

    public GetResponsePlanConverter(
        Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter,
        Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter,
        Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> chatChannelConverter) {
        this.incidentTemplateConverter = incidentTemplateConverter;
        this.actionConverter = actionConverter;
        this.chatChannelConverter = chatChannelConverter;
    }

    @Override
    protected ResourceModel doForward(GetResponsePlanResponse getResponsePlanResponse) {
        return ResourceModel.builder()
            .arn(getResponsePlanResponse.arn())
            .name(getResponsePlanResponse.name())
            .displayName(getResponsePlanResponse.displayName())
            .chatChannel(chatChannelConverter.convert(getResponsePlanResponse.chatChannel()))
            .incidentTemplate(
                incidentTemplateConverter.convert(getResponsePlanResponse.incidentTemplate())
            )
            .actions(
                Optional.ofNullable(getResponsePlanResponse.actions())
                    .map(x -> Lists.newArrayList(actionConverter.convertAll(x)))
                    .orElse(new ArrayList<>())
            )
            .engagements(
                new HashSet<>(
                    Optional.ofNullable(getResponsePlanResponse.engagements())
                        .orElse(new ArrayList<>())
                ))
            .build();
    }

    @Override
    protected GetResponsePlanResponse doBackward(ResourceModel resourceModel) {
        throw new UnsupportedOperationException();
    }
}
