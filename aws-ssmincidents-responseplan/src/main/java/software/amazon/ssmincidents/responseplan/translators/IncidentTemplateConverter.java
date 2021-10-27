package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem;
import software.amazon.ssmincidents.responseplan.IncidentTemplate;
import software.amazon.ssmincidents.responseplan.IncidentTemplate.IncidentTemplateBuilder;

import java.util.ArrayList;
import java.util.List;

public class IncidentTemplateConverter extends
    Converter<software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate, IncidentTemplate> {

    Converter<software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem,
                 software.amazon.ssmincidents.responseplan.NotificationTargetItem> notificationTargetConverter;

    public IncidentTemplateConverter(
        Converter<NotificationTargetItem, software.amazon.ssmincidents.responseplan.NotificationTargetItem> notificationTargetConverter
    ) {
        this.notificationTargetConverter = notificationTargetConverter;
    }

    @Override
    protected IncidentTemplate doForward(
        software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate incidentTemplate
    ) {
        IncidentTemplateBuilder builder = IncidentTemplate.builder()
            .title(incidentTemplate.title())
            .summary(emptyStringToNull(incidentTemplate.summary()))
            .impact(incidentTemplate.impact())
            .dedupeString(emptyStringToNull(incidentTemplate.dedupeString()));

        if (incidentTemplate.notificationTargets() != null && !incidentTemplate.notificationTargets().isEmpty()) {
            builder.notificationTargets(
                Lists.newArrayList(
                    notificationTargetConverter.convertAll(
                        incidentTemplate.notificationTargets())
                )
            );
        }
        return builder.build();
    }

    @Override
    protected software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate doBackward(
        IncidentTemplate incidentTemplate) {

        List<NotificationTargetItem> notificationTargets = new ArrayList<>();
        if (incidentTemplate.getNotificationTargets() != null) {
            notificationTargets = Lists.newArrayList(
                notificationTargetConverter.reverse().convertAll(
                    incidentTemplate.getNotificationTargets())
            );
        }
        return software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
            .title(incidentTemplate.getTitle())
            .summary(nullToEmptyString(incidentTemplate.getSummary()))
            .impact(incidentTemplate.getImpact())
            .dedupeString(nullToEmptyString(incidentTemplate.getDedupeString()))
            .notificationTargets(notificationTargets)
            .build();
    }

    private String emptyStringToNull(String summary) {
        return (summary == null || summary.isEmpty()) ? null : summary;
    }

    private String nullToEmptyString(String summary) {
        return summary == null ? "" : summary;
    }
}
