package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest;
import software.amazon.ssmincidents.responseplan.Action;
import software.amazon.ssmincidents.responseplan.ChatChannel;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.ResourceModel;
import software.amazon.ssmincidents.responseplan.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CreateResponsePlanConverter extends
    Converter<software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest, ResourceModel> {

  Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter;
  Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter;
  Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> chatChannelConverter;
  Converter<Map<String, String>, Set<Tag>> tagsConverter;

  public CreateResponsePlanConverter(
      Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> incidentTemplateConverter,
      Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> actionConverter,
      Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> chatChannelConverter,
      Converter<Map<String, String>, Set<Tag>> tagsConverter
      ) {
    this.incidentTemplateConverter = incidentTemplateConverter;
    this.actionConverter = actionConverter;
    this.chatChannelConverter = chatChannelConverter;
    this.tagsConverter = tagsConverter;
  }

  @Override
  protected ResourceModel doForward(CreateResponsePlanRequest createResponsePlanRequest) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected CreateResponsePlanRequest doBackward(ResourceModel resourceModel) {
    return CreateResponsePlanRequest.builder()
        .name(resourceModel.getName())
        .displayName(resourceModel.getDisplayName())
        .chatChannel(chatChannelConverter.reverse().convert(resourceModel.getChatChannel()))
        .actions(
            Optional.ofNullable(resourceModel.getActions())
                .map(x -> Lists.newArrayList(actionConverter.reverse().convertAll(x)))
                .orElse(new ArrayList<>())
        )
        .engagements(
            Optional.ofNullable(resourceModel.getEngagements())
                .orElse(new HashSet<>())
        )
        .incidentTemplate(
            incidentTemplateConverter.reverse().convert(resourceModel.getIncidentTemplate())
        )
        .tags(tagsConverter.reverse().convert(resourceModel.getTags()))
        .build();
  }
}
