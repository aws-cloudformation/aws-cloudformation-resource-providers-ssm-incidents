package software.amazon.ssmincidents.responseplan.translators;


import com.google.common.base.Converter;
import software.amazon.ssmincidents.responseplan.NotificationTargetItem;

public class NotificationTargetConverter extends
    Converter<software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem, NotificationTargetItem> {


    @Override
    protected NotificationTargetItem doForward(
        software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem notificationTargetItem
    ) {
        return NotificationTargetItem.builder()
            .snsTopicArn(notificationTargetItem.snsTopicArn())
            .build();
    }

    @Override
    protected software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem doBackward(
        NotificationTargetItem notificationTargetItem
    ) {
        return software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem.builder()
            .snsTopicArn(notificationTargetItem.getSnsTopicArn())
            .build();
    }
}
