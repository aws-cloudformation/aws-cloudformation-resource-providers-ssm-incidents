package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import software.amazon.awssdk.services.ssmincidents.model.ChatChannel;
import software.amazon.awssdk.services.ssmincidents.model.EmptyChatChannel;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest;
import software.amazon.ssmincidents.responseplan.Action;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.ResourceModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public class UpdateResponsePlanConverter extends
    Converter<UpdateResponsePlanRequest, ResourceModel> {

    Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter;
    Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter;
    Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel,
        software.amazon.ssmincidents.responseplan.ChatChannel> chatChannelConverter;

    public UpdateResponsePlanConverter(
        Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter,
        Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter,
        Converter<ChatChannel, software.amazon.ssmincidents.responseplan.ChatChannel> chatChannelConverter
    ) {
        this.incidentTemplateConverter = incidentTemplateConverter;
        this.actionConverter = actionConverter;
        this.chatChannelConverter = chatChannelConverter;
    }

    @Override
    protected ResourceModel doForward(UpdateResponsePlanRequest updateResponsePlanRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected UpdateResponsePlanRequest doBackward(ResourceModel resourceModel) {
        software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate incidentTemplate = incidentTemplateConverter.reverse().convert(resourceModel.getIncidentTemplate());


        UpdateResponsePlanRequest updateResponsePlanRequest = UpdateResponsePlanRequest.builder()
            .arn(resourceModel.getArn())
            .displayName(resourceModel.getDisplayName())
            .chatChannel(
                Optional
                    .ofNullable(chatChannelConverter.reverse().convert(resourceModel.getChatChannel()))
                    .orElse(ChatChannel.builder().empty(EmptyChatChannel.builder().build()).build())
            )
            .actions(
                Optional.ofNullable(resourceModel.getActions())
                    .map(x -> Lists.newArrayList(actionConverter.reverse().convertAll(x)))
                    .orElse(new ArrayList<>())
            )
            .engagements(
                Optional.ofNullable(resourceModel.getEngagements())
                    .orElse(new HashSet<>())
            )
            .incidentTemplateTitle(incidentTemplate.title())
            .incidentTemplateSummary(incidentTemplate.summary())
            .incidentTemplateImpact(incidentTemplate.impact())
            .incidentTemplateDedupeString(incidentTemplate.dedupeString())
            .incidentTemplateNotificationTargets(
                Optional.ofNullable(incidentTemplate.notificationTargets()).orElse(new ArrayList<>())
            )
            .incidentTemplateTags(incidentTemplate.incidentTags())
            .build();
        return updateResponsePlanRequest;
    }
}
