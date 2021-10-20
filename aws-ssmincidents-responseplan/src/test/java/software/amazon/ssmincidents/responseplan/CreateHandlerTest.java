package software.amazon.ssmincidents.responseplan;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanResponse;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

  @Mock
  SsmIncidentsClient sdkClient;
  @Mock
  private AmazonWebServicesClientProxy proxy;
  @Mock
  private ProxyClient<SsmIncidentsClient> proxyClient;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    sdkClient = mock(SsmIncidentsClient.class);
    proxyClient = MOCK_PROXY(proxy, sdkClient);
  }

  @AfterEach
  public void tear_down() {
    verify(sdkClient, atLeastOnce()).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
    public void handleRequest_SimpleSuccess() {
    final CreateHandler handler = new CreateHandler();

    final ResourceModel model = TestData.DESIRED_MODEL_BASE;
    when(proxyClient.client().createResponsePlan(any(CreateResponsePlanRequest.class)))
        .thenReturn(
            CreateResponsePlanResponse
                .builder()
                .arn(TestData.ARN)
                .build()
        );

    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().build());

    when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
        .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_BASE);

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    final ProgressEvent<ResourceModel, CallbackContext> response = handler
        .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    verify(sdkClient, times(1)).getResponsePlan(any(GetResponsePlanRequest.class));
    verify(sdkClient, times(1)).createResponsePlan(any(CreateResponsePlanRequest.class));
    verify(sdkClient, times(1)).listTagsForResource(any(ListTagsForResourceRequest.class));

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(TestData.RETURNED_MODEL_BASE);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
  }

  @Test
  public void handleRequest_CompleteSuccess() {
    final CreateHandler handler = new CreateHandler();

    final ResourceModel model = DESIRED_COMPLETE_MODEL;
    when(proxyClient.client().createResponsePlan(any(CreateResponsePlanRequest.class)))
            .thenReturn(
                    CreateResponsePlanResponse
                            .builder()
                            .arn(TestData.ARN)
                            .build()
            );

    when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
            .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_COMPLETE);

    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(TestData.API_TAGS_1).build());

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

    final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    verify(sdkClient, times(1)).createResponsePlan(argThat((CreateResponsePlanRequest x) -> assertCreateResponsePlanRequestComplete(x)));
    verify(sdkClient, times(1)).getResponsePlan(any(GetResponsePlanRequest.class));
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).usingRecursiveComparison().isEqualTo(RETURNED_COMPLETE_MODEL);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
  }

  private boolean assertCreateResponsePlanRequestComplete(CreateResponsePlanRequest createResponsePlanRequest) {
    assertThat(createResponsePlanRequest.displayName()).isEqualTo(TestData.DISPLAY_NAME);
    assertThat(createResponsePlanRequest.actions()).containsExactlyInAnyOrder(TestData.API_ACTION_1);
    assertThat(createResponsePlanRequest.engagements()).containsExactlyInAnyOrder(TestData.CONTACT, TestData.ESCALATION);
    assertThat(createResponsePlanRequest.incidentTemplate().title()).isEqualTo(TestData.TITLE);
    assertThat(createResponsePlanRequest.incidentTemplate().impact()).isEqualTo(TestData.IMPACT);
    assertThat(createResponsePlanRequest.incidentTemplate().summary()).isEqualTo(TestData.SUMMARY);
    assertThat(createResponsePlanRequest.incidentTemplate().dedupeString()).isEqualTo(TestData.DEDUP);
    assertThat(createResponsePlanRequest.incidentTemplate().notificationTargets()).containsExactlyInAnyOrder(TestData.API_NOTIFICATION_TARGET_ITEM_1, TestData.API_NOTIFICATION_TARGET_ITEM_2);
    assertThat(createResponsePlanRequest.chatChannel()).isEqualTo(TestData.API_CHAT_CHANNEL);
    return true;
  }

  public static final ResourceModel RETURNED_COMPLETE_MODEL = ResourceModel.builder()
                                                                  .arn(TestData.ARN)
                                                                  .name(TestData.NAME)
                                                                  .displayName(TestData.DISPLAY_NAME)
                                                                  .chatChannel(TestData.CHAT_CHANNEL)
                                                                  .incidentTemplate(
                                                                      IncidentTemplate.builder()
                                                                          .title(TestData.TITLE)
                                                                          .impact(TestData.IMPACT)
                                                                          .dedupeString(TestData.DEDUP)
                                                                          .summary(TestData.SUMMARY)
                                                                          .notificationTargets(ImmutableList.of(
                                                                              NotificationTargetItem.builder().snsTopicArn(TestData.CHAT_SNS_1).build(),
                                                                              NotificationTargetItem.builder().snsTopicArn(TestData.CHAT_SNS_2).build()
                                                                          ))
                                                                          .build()
                                                                  )
                                                                  .actions(ImmutableList.of(Action.builder().ssmAutomation(TestData.SSM_AUTOMATION_1).build()))
                                                                  .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                                                                  .tags(TestData.TAGS_1)
                                                                  .build();

  public static final ResourceModel DESIRED_COMPLETE_MODEL = ResourceModel.builder()
                                                                 .name(TestData.NAME)
                                                                 .displayName(TestData.DISPLAY_NAME)
                                                                 .chatChannel(TestData.CHAT_CHANNEL)
                                                                 .incidentTemplate(
                                                                     IncidentTemplate.builder()
                                                                         .title(TestData.TITLE)
                                                                         .impact(TestData.IMPACT)
                                                                         .summary(TestData.SUMMARY)
                                                                         .dedupeString(TestData.DEDUP)
                                                                         .impact(TestData.IMPACT)
                                                                         .notificationTargets(ImmutableList.of(
                                                                             NotificationTargetItem.builder().snsTopicArn(TestData.CHAT_SNS_1).build(),
                                                                             NotificationTargetItem.builder().snsTopicArn(TestData.CHAT_SNS_2).build()
                                                                         ))
                                                                         .build()
                                                                 )
                                                                 .actions(ImmutableList.of(Action.builder().ssmAutomation(TestData.SSM_AUTOMATION_1).build()))
                                                                 .engagements(ImmutableSet.of(TestData.CONTACT, TestData.ESCALATION))
                                                                 .tags(TestData.TAGS_1)
                                                                 .build();
}
