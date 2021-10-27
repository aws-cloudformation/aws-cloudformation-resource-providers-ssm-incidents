package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest;
import software.amazon.ssmincidents.responseplan.Action;
import software.amazon.ssmincidents.responseplan.ChatChannel;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.NotificationTargetItem;
import software.amazon.ssmincidents.responseplan.ResourceModel;
import software.amazon.ssmincidents.responseplan.SsmParameter;
import software.amazon.ssmincidents.responseplan.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TranslatorFactory {
    public final static Converter<Map<String, String>, Set<Tag>> TAGS_CONVERTER = new TagsConverter();

    public final static Converter<software.amazon.awssdk.services.ssmincidents.model.ChatChannel, ChatChannel> CHAT_CHANNEL_CONVERTER = new ChatChannelConverter();

    public final static Converter<Map<String, List<String>>, Set<SsmParameter>> SSM_PARAMETERS_CONVERTER =
        new SsmParameterConverter();

    public final static Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> ACTION_CONVERTER =
        new ActionConverter(
            SSM_PARAMETERS_CONVERTER);

    public final static Converter<software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem,
                                     NotificationTargetItem> NOTIFICATION_TARGET_CONVERTER = new NotificationTargetConverter();

    public final static Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> INCIDENT_TEMPLATE_CONVERTER = new IncidentTemplateConverter(
        NOTIFICATION_TARGET_CONVERTER);
    public final static Converter<software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest,
                                     ResourceModel> CREATE_RESPONSEPLAN_CONVERTER =
        new CreateResponsePlanConverter(
            INCIDENT_TEMPLATE_CONVERTER,
            ACTION_CONVERTER,
            CHAT_CHANNEL_CONVERTER,
            TAGS_CONVERTER
        );
    public final static Converter<GetResponsePlanResponse, ResourceModel> GET_RESPONSEPLAN_CONVERTER =
        new GetResponsePlanConverter(
            INCIDENT_TEMPLATE_CONVERTER,
            ACTION_CONVERTER,
            CHAT_CHANNEL_CONVERTER
        );
    public final static Converter<UpdateResponsePlanRequest, ResourceModel> UPDATE_RESPONSEPLAN_CONVERTER =
        new UpdateResponsePlanConverter(
            INCIDENT_TEMPLATE_CONVERTER,
            ACTION_CONVERTER,
            CHAT_CHANNEL_CONVERTER
        );
}
