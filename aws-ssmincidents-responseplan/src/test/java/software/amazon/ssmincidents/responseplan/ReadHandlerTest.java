package software.amazon.ssmincidents.responseplan;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

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
public class ReadHandlerTest extends AbstractTestBase {

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
    final ReadHandler handler = new ReadHandler();

    final ResourceModel model = ResourceModel.builder().arn(TestData.ARN).build();

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
        .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_BASE);

    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().build());

    final ProgressEvent<ResourceModel, CallbackContext> response = handler
        .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    verify(sdkClient, times(1)).listTagsForResource(any(ListTagsForResourceRequest.class));

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).usingRecursiveComparison().isEqualTo(TestData.RETURNED_MODEL_BASE);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
  }

  @Test
  public void handleRequest_completeSuccess() {
    final ReadHandler handler = new ReadHandler();
    final ResourceModel model = ResourceModel.builder().arn(TestData.ARN).build();
    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                              .desiredResourceState(model)
                                                              .build();

    when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
        .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_COMPLETE);

    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(TestData.API_TAGS_1).build());

    final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    verify(sdkClient, times(1)).listTagsForResource(argThat((ListTagsForResourceRequest x) -> x.resourceArn().equals(TestData.ARN)));

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).usingRecursiveComparison().isEqualTo(TestData.MODEL_COMPLETE);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
  }

  @Test
  public void handleRequest_WithTagsSuccess() {
    final ReadHandler handler = new ReadHandler();
    final ResourceModel model = ResourceModel.builder().arn(TestData.ARN).build();
    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                              .desiredResourceState(model)
                                                              .build();

    when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
        .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_BASE);

    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(TestData.API_TAGS_1).build());

    final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    verify(sdkClient, times(1)).listTagsForResource(any(ListTagsForResourceRequest.class));
    verify(sdkClient, times(1)).getResponsePlan((GetResponsePlanRequest) argThat((GetResponsePlanRequest x) -> x.arn().equals(TestData.ARN)));
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(RETURNED_MODEL_WITH_TAGS);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
  }

  public static final ResourceModel RETURNED_MODEL_WITH_TAGS = ResourceModel.builder()
                                                              .arn(TestData.ARN)
                                                              .name(TestData.NAME)
                                                              .incidentTemplate(
                                                                  IncidentTemplate.builder().title(TestData.TITLE).impact(TestData.IMPACT).build()
                                                              )
                                                              .engagements(new HashSet<>())
                                                              .actions(new ArrayList<>())
                                                              .tags(TestData.TAGS_1)
                                                              .build();
}
