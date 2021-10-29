package software.amazon.ssmincidents.replicationset;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.DeleteReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.RegionInfo;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSet;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSetStatus;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class DeleteHandlerTest extends AbstractTestBase {

  private AmazonWebServicesClientProxy proxy;

  private ProxyClient<SsmIncidentsClient> proxyClient;

  @Mock
  private SsmIncidentsClient sdkClient;

  private DeleteHandler handler;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);
    handler = new DeleteHandler();
  }

  @AfterEach
  public void cleanup() {
    verify(sdkClient, atMost(5)).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
  public void handleRequest_InitialStabilization() {

    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(ReplicationSet.builder()
            .status(ReplicationSetStatus.UPDATING)
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getReplicationSetResponse);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();
    ResourceHandlerRequest<ResourceModel> handlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> event =
        handler.handleRequest(proxy, handlerRequest, new CallbackContext(), proxyClient, logger);

    assertThat(event).isNotNull();
    assertThat(event.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(event.getResourceModel()).isEqualTo(model);
    assertThat(event.getCallbackContext()).isEqualTo(new CallbackContext(239, null));
    assertThat(event.getCallbackDelaySeconds()).isEqualTo(30);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_AfterInitialStabilization() {

    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(ReplicationSet.builder()
            .status(ReplicationSetStatus.ACTIVE)
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build())
        .build();

    GetReplicationSetResponse getReplicationSetDeletingResponse = GetReplicationSetResponse.builder()
        .replicationSet(ReplicationSet.builder()
            .status(ReplicationSetStatus.DELETING)
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getReplicationSetResponse, getReplicationSetDeletingResponse);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();
    ResourceHandlerRequest<ResourceModel> handlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> event =
        handler.handleRequest(proxy, handlerRequest, new CallbackContext(), proxyClient, logger);

    assertThat(event).isNotNull();
    assertThat(event.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(event.getResourceModel()).isEqualTo(model);
    assertThat(event.getCallbackContext()).isEqualTo(new CallbackContext(239, true));
    assertThat(event.getCallbackDelaySeconds()).isEqualTo(30);

    ArgumentCaptor<DeleteReplicationSetRequest> deleteRequest =
        ArgumentCaptor.forClass(DeleteReplicationSetRequest.class);
    verify(sdkClient).deleteReplicationSet(deleteRequest.capture());
    assertThat(deleteRequest.getValue()).isNotNull();
    assertThat(deleteRequest.getValue().arn()).isEqualTo("arn");

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(2)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues()).hasSize(2);
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_DeletionComplete() {

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("test not found exception").build());

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();
    ResourceHandlerRequest<ResourceModel> handlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    CallbackContext context = new CallbackContext(200, true);

    ProgressEvent<ResourceModel, CallbackContext> event =
        handler.handleRequest(proxy, handlerRequest, context, proxyClient, logger);

    assertThat(event).isNotNull();
    assertThat(event.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(event.getResourceModel()).isNull();
    assertThat(event.getCallbackContext()).isNull();
    assertThat(event.getCallbackDelaySeconds()).isEqualTo(0);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_Timeout() {

    GetReplicationSetResponse getReplicationSetDeletingResponse = GetReplicationSetResponse.builder()
        .replicationSet(ReplicationSet.builder()
            .status(ReplicationSetStatus.DELETING)
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getReplicationSetDeletingResponse);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();
    ResourceHandlerRequest<ResourceModel> handlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    CallbackContext context = new CallbackContext(0, true);

    ProgressEvent<ResourceModel, CallbackContext> event =
        handler.handleRequest(proxy, handlerRequest, context, proxyClient, logger);

    assertThat(event).isNotNull();
    assertThat(event.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(event.getResourceModel()).isNull();
    assertThat(event.getCallbackContext()).isNull();
    assertThat(event.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(event.getMessage()).isEqualTo("Timed out waiting for replication set to be deleted");
    assertThat(event.getErrorCode()).isEqualTo(HandlerErrorCode.NotStabilized);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_AlreadyDeleted() {

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("test resource not found").build());

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();
    ResourceHandlerRequest<ResourceModel> handlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> event =
        handler.handleRequest(proxy, handlerRequest, new CallbackContext(), proxyClient, logger);

    assertThat(event).isNotNull();
    assertThat(event.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(event.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    assertThat(event.getResourceModel()).isNull();
    assertThat(event.getCallbackContext()).isNull();
    assertThat(event.getCallbackDelaySeconds()).isEqualTo(0);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  private void handlRequest_testException(Exception deleteException, HandlerErrorCode expectedErrorCode) {
    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(ReplicationSet.builder()
            .status(ReplicationSetStatus.ACTIVE)
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getReplicationSetResponse);

    when(sdkClient.deleteReplicationSet(any(DeleteReplicationSetRequest.class)))
        .thenThrow(deleteException);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();
    ResourceHandlerRequest<ResourceModel> handlerRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> event =
        handler.handleRequest(proxy, handlerRequest, new CallbackContext(), proxyClient, logger);

    assertThat(event).isNotNull();
    assertThat(event.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(event.getResourceModel()).isNull();
    assertThat(event.getCallbackContext()).isNull();
    assertThat(event.getErrorCode()).isEqualTo(expectedErrorCode);

    ArgumentCaptor<DeleteReplicationSetRequest> deleteRequest =
        ArgumentCaptor.forClass(DeleteReplicationSetRequest.class);
    verify(sdkClient).deleteReplicationSet(deleteRequest.capture());
    assertThat(deleteRequest.getValue()).isNotNull();
    assertThat(deleteRequest.getValue().arn()).isEqualTo("arn");

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_ResourceNotFoundException() {
    handlRequest_testException(ResourceNotFoundException.builder().build(), HandlerErrorCode.NotFound);
  }

  @Test
  public void handleRequest_ValidationException() {
    handlRequest_testException(ValidationException.builder().build(), HandlerErrorCode.InvalidRequest);
  }

  @Test
  public void handleRequest_OtherException() {
    handlRequest_testException(new RuntimeException(), HandlerErrorCode.GeneralServiceException);
  }
}
