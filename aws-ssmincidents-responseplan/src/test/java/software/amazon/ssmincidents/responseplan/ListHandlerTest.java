package software.amazon.ssmincidents.responseplan;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.ListResponsePlansRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListResponsePlansResponse;
import software.amazon.awssdk.services.ssmincidents.model.ResponsePlanSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

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

  @Test
  public void handleRequest_SimpleSuccess() {
    final ListHandler handler = new ListHandler();

    final ResourceModel model = ResourceModel.builder().build();

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ImmutableSet<String> arns = ImmutableSet.of("arn1", "arn2");
    List<ResponsePlanSummary> responsePlanSummaries = arns
            .stream()
            .map(x -> ResponsePlanSummary.builder().arn(x).build())
            .collect(Collectors.toList());

    when(proxyClient.client().listResponsePlans(any(ListResponsePlansRequest.class)))
        .thenReturn(ListResponsePlansResponse.builder().responsePlanSummaries(responsePlanSummaries).build());

    final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackContext()).isNull();
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNotNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    assertThat(
        response.getResourceModels().stream().map(ResourceModel::getArn).collect(Collectors.toSet())
    ).containsAll(arns);
  }

  @Test
  public void handleRequest_PagingSuccess() {
    final String nextToken = "abc123";

    final ListHandler handler = new ListHandler();

    final ResourceModel model = ResourceModel.builder().build();

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                              .desiredResourceState(model)
                                                              .build();

    ImmutableSet<String> arns = ImmutableSet.of("arn1", "arn2");
    List<ResponsePlanSummary> responsePlanSummaries = arns.stream()
                                                          .map(x -> ResponsePlanSummary.builder().arn(x).build())
                                                          .collect(Collectors.toList());

    when(proxyClient.client().listResponsePlans(any(ListResponsePlansRequest.class)))
        .thenReturn(
            ListResponsePlansResponse.builder()
                .responsePlanSummaries(responsePlanSummaries)
                .nextToken(nextToken)
                .build()
        );

    final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackContext()).isNull();
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNotNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    assertThat(
        response.getResourceModels().stream().map(ResourceModel::getArn).collect(Collectors.toSet())
    ).containsAll(arns);
    assertThat(response.getNextToken()).isEqualTo(nextToken);
  }
}
