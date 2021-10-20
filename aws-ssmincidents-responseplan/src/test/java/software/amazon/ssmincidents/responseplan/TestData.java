package software.amazon.ssmincidents.responseplan;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse;
import software.amazon.awssdk.services.ssmincidents.model.SsmTargetAccount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestData {

  public static final String NAME = "my-rp-name";
  public static final String ARN = "my-arn";
  public static final String DISPLAY_NAME = "show-®℗";
  public static final String TITLE = "my-title";
  public static final Integer IMPACT = 3;
  public static final String DEDUP = "my-dedup";
  public static final String SUMMARY = "my-summary";

  public static final String SSM_ROLE = "my-ssm-role";
  public static final String SSM_DOC = "my-ssm-doc";
  public static final String SSM_DOC_2 = "my-ssm-doc-2";
  public static final String SSM_VERSION = "my-ssm-version";

  public static final String CHAT_SNS_1 = "my-chat";
  public static final String CHAT_SNS_2 = "my-chat-2";
  public static final String ESCALATION = "my-escalation";
  public static final String CONTACT = "my-contact";
  public static final String CONTACT_2 = "my-contact-2";

  public static final String SSM_PARAMETER_KEY = "key";
  public static final List<String> SSM_PARAMETER_VALUES = ImmutableList.of("one", "two");

  public static final String SSM_PARAMETER_KEY_2 = "key2";
  public static final List<String> SSM_PARAMETER_VALUES_2 = ImmutableList.of("foo", "bar", "it");

  public static final String TAG_KEY_1 = "tag_key_1";
  public static final String TAG_VALUE_1 = "tag_value_1";

  public static final String TAG_KEY_2 = "tag_key_2";
  public static final String TAG_VALUE_2 = "tag_value_2";

  public static final ImmutableMap<String, String> API_TAGS_1 = ImmutableMap.of(TAG_KEY_1, TAG_VALUE_1, TAG_KEY_2, TAG_VALUE_2);
  public static final Set<Tag> TAGS_1 = ImmutableSet.of(new Tag(TestData.TAG_KEY_1, TestData.TAG_VALUE_1), new Tag(TestData.TAG_KEY_2, TestData.TAG_VALUE_2));

  public static final String TAG_KEY_3 = "tag_key_3";
  public static final String TAG_VALUE_3 = "tag_value_3";

  public static final Map<String, List<String>> API_SSM_PARAMETERS = ImmutableMap.of(
      SSM_PARAMETER_KEY,
      SSM_PARAMETER_VALUES
  );
  public static final Set<SsmParameter> SSM_PARAMETERS = ImmutableSet.of(SsmParameter.builder()
      .key(SSM_PARAMETER_KEY)
      .values(SSM_PARAMETER_VALUES)
      .build()
  );

  public static final Map<String, List<String>> API_SSM_PARAMETERS_1 = ImmutableMap.of(
      SSM_PARAMETER_KEY,
      SSM_PARAMETER_VALUES,
      SSM_PARAMETER_KEY_2,
      SSM_PARAMETER_VALUES_2
  );
  public static final Set<SsmParameter> SSM_PARAMETERS_1 = ImmutableSet.of(
      SsmParameter.builder()
          .key(SSM_PARAMETER_KEY)
          .values(SSM_PARAMETER_VALUES)
          .build(),
      SsmParameter.builder()
          .key(SSM_PARAMETER_KEY_2)
          .values(SSM_PARAMETER_VALUES_2)
          .build()
  );

  public static final software.amazon.awssdk.services.ssmincidents.model.SsmAutomation API_SSM_AUTOMATION =
      software.amazon.awssdk.services.ssmincidents.model.SsmAutomation
          .builder()
          .documentName(SSM_DOC)
          .documentVersion(SSM_VERSION)
          .roleArn(SSM_ROLE)
          .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.Action API_ACTION =
      software.amazon.awssdk.services.ssmincidents.model.Action.builder()
          .ssmAutomation(API_SSM_AUTOMATION)
          .build();

  public static final SsmAutomation SSM_AUTOMATION = SsmAutomation.builder()
      .documentName(SSM_DOC)
      .documentVersion(SSM_VERSION)
      .roleArn(SSM_ROLE)
      .build();

  public static final Action ACTION = Action.builder()
      .ssmAutomation(SSM_AUTOMATION)
      .build();

  public static final Action ACTION_EMPTY_SSM_PARAMETERS = Action.builder()
      .ssmAutomation(SsmAutomation.builder()
          .documentName(SSM_DOC)
          .documentVersion(SSM_VERSION)
          .roleArn(SSM_ROLE)
          .parameters(new HashSet<>())
          .build())
      .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.SsmAutomation API_SSM_AUTOMATION_1 =
      software.amazon.awssdk.services.ssmincidents.model.SsmAutomation
          .builder()
          .documentName(SSM_DOC)
          .documentVersion(SSM_VERSION)
          .roleArn(SSM_ROLE)
          .targetAccount(SsmTargetAccount.IMPACTED_ACCOUNT)
          .parameters(API_SSM_PARAMETERS_1)
          .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.Action API_ACTION_1 =
      software.amazon.awssdk.services.ssmincidents.model.Action.builder()
          .ssmAutomation(API_SSM_AUTOMATION_1)
          .build();

  public static final SsmAutomation SSM_AUTOMATION_1 = SsmAutomation.builder()
      .documentName(SSM_DOC)
      .documentVersion(SSM_VERSION)
      .roleArn(SSM_ROLE)
      .parameters(SSM_PARAMETERS_1)
      .targetAccount("IMPACTED_ACCOUNT")
      .build();

  public static final Action ACTION_1 = Action.builder()
      .ssmAutomation(SSM_AUTOMATION_1)
      .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.ChatChannel API_CHAT_CHANNEL =
      software.amazon.awssdk.services.ssmincidents.model.ChatChannel.builder()
          .chatbotSns(CHAT_SNS_1, CHAT_SNS_2)
          .build();

  public static final ChatChannel CHAT_CHANNEL = ChatChannel.builder()
      .chatbotSns(ImmutableList.of(CHAT_SNS_1, CHAT_SNS_2))
      .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem API_NOTIFICATION_TARGET_ITEM_1 =
      software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem.builder()
          .snsTopicArn(TestData.CHAT_SNS_1)
          .build();

  public static final NotificationTargetItem NOTIFICATION_TARGET_ITEM_1 = NotificationTargetItem
      .builder()
      .snsTopicArn(CHAT_SNS_1)
      .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem API_NOTIFICATION_TARGET_ITEM_2 =
      software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem.builder()
          .snsTopicArn(CHAT_SNS_2)
          .build();

  public static final NotificationTargetItem NOTIFICATION_TARGET_ITEM_2 = NotificationTargetItem
      .builder()
      .snsTopicArn(CHAT_SNS_2)
      .build();

  public static final software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate API_INCIDENT_TEMPLATE =
      software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
          .title(TITLE)
          .summary(SUMMARY)
          .impact(IMPACT)
          .dedupeString(DEDUP)
          .notificationTargets(
              API_NOTIFICATION_TARGET_ITEM_1,
              API_NOTIFICATION_TARGET_ITEM_2
          )
          .build();

  public static final IncidentTemplate INCIDENT_TEMPLATE = IncidentTemplate.builder()
      .title(TITLE)
      .summary(SUMMARY)
      .impact(IMPACT)
      .dedupeString(DEDUP)
      .notificationTargets(ImmutableList.of(
          NOTIFICATION_TARGET_ITEM_1,
          NOTIFICATION_TARGET_ITEM_2
          )
      )
      .build();

  public static final ResourceModel DESIRED_MODEL_BASE = ResourceModel.builder()
      .name(NAME)
      .incidentTemplate(
          IncidentTemplate.builder().title(TITLE).impact(IMPACT).build()
      ).build();

  public static final ResourceModel RETURNED_MODEL_BASE = ResourceModel.builder()
          .arn(ARN)
          .name(NAME)
          .incidentTemplate(
                  IncidentTemplate.builder().title(TITLE).impact(IMPACT).build()
          )
          .engagements(new HashSet<>())
          .actions(new ArrayList<>())
          .tags(new HashSet<>())
          .build();

  public static final GetResponsePlanResponse GET_RESPONSE_PLAN_RESPONSE_BASE = GetResponsePlanResponse
      .builder()
      .arn(ARN)
      .name(NAME)
      .incidentTemplate(
          software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder().title(TITLE).impact(IMPACT).build()
      )
      .build();

  public static final GetResponsePlanResponse GET_RESPONSE_PLAN_RESPONSE_COMPLETE = GetResponsePlanResponse
          .builder()
          .arn(ARN)
          .name(NAME)
          .displayName(DISPLAY_NAME)
          .chatChannel(API_CHAT_CHANNEL)
          .incidentTemplate(
                  software.amazon.awssdk.services.ssmincidents.model.IncidentTemplate.builder()
                          .title(TITLE)
                          .impact(IMPACT)
                          .dedupeString(DEDUP)
                          .summary(SUMMARY)
                          .notificationTargets(ImmutableList.of(
                                  software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem.builder().snsTopicArn(CHAT_SNS_1).build(),
                                  software.amazon.awssdk.services.ssmincidents.model.NotificationTargetItem.builder().snsTopicArn(CHAT_SNS_2).build()
                          ))
                          .build()
          )
          .actions(API_ACTION_1)
          .engagements(ImmutableSet.of(CONTACT, ESCALATION))
          .build();

  public static final ResourceModel MODEL_COMPLETE = ResourceModel.builder()
                                                                 .arn(ARN)
                                                                 .name(NAME)
                                                                 .displayName(DISPLAY_NAME)
                                                                 .chatChannel(CHAT_CHANNEL)
                                                                 .incidentTemplate(
                                                                     IncidentTemplate.builder()
                                                                         .title(TITLE)
                                                                         .impact(IMPACT)
                                                                         .summary(SUMMARY)
                                                                         .dedupeString(DEDUP)
                                                                         .notificationTargets(ImmutableList.of(
                                                                             NotificationTargetItem.builder().snsTopicArn(CHAT_SNS_1).build(),
                                                                             NotificationTargetItem.builder().snsTopicArn(CHAT_SNS_2).build()
                                                                         ))
                                                                         .build()
                                                                 )
                                                                 .actions(ImmutableList.of(Action.builder().ssmAutomation(SSM_AUTOMATION_1).build()))
                                                                 .engagements(ImmutableSet.of(CONTACT, ESCALATION))
                                                                 .tags(TAGS_1)
                                                                 .build();
}
