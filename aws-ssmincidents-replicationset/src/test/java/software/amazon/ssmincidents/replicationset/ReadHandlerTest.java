package software.amazon.ssmincidents.replicationset;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.RegionInfo;
import software.amazon.awssdk.services.ssmincidents.model.RegionStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public class ReadHandlerTest extends AbstractTestBase {

  private AmazonWebServicesClientProxy proxy;

  private ProxyClient<SsmIncidentsClient> proxyClient;

  @Mock
  private SsmIncidentsClient sdkClient;

  private ReadHandler handler;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);
    handler = new ReadHandler();
  }

  @AfterEach
  public void cleanup() {
    verify(sdkClient, atMost(5)).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
  public void handleRequest_SimpleSuccess() {

    GetReplicationSetResponse getResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.ACTIVE)
                .deletionProtected(true)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder()
                            .status(RegionStatus.ACTIVE)
                            .sseKmsKeyId("kms-key-id-us-east-1")
                            .build(),
                        "us-west-2", RegionInfo.builder()
                            .status(RegionStatus.ACTIVE)
                            .sseKmsKeyId("kms-key-id-us-west-2")
                            .build()
                    )
                )
                .build()
        )
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getResponse);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(
        ResourceModel.builder()
            .arn("arn")
            .deletionProtected(true)
            .regions(
                ImmutableSet.of(
                    new ReplicationRegion(
                        "us-east-1",
                        RegionConfiguration.builder().sseKmsKeyId("kms-key-id-us-east-1"
                        ).build()),
                    new ReplicationRegion(
                        "us-west-2",
                        RegionConfiguration.builder().sseKmsKeyId("kms-key-id-us-west-2"
                        ).build())
                )
            )
            .build()
    );
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_NotFound() {

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("test not found exception").build());

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isEqualTo("test not found exception");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_ValidationException() {

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenThrow(ValidationException.builder().message("test validation exception").build());

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isEqualTo("test validation exception");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_OtherException() {

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenThrow(new RuntimeException("test exception"));

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isEqualTo("test exception");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_nullArn() {

    ResourceModel model = ResourceModel.builder()
        .arn(null)
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
  }

  @Test
  public void testDefaultKeyReturned() {
    GetReplicationSetResponse getResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.ACTIVE)
                .deletionProtected(true)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder()
                            .status(RegionStatus.ACTIVE)
                            .sseKmsKeyId(ReadHandler.DEFAULT_KMS_KEY_ID)
                            .build(),
                        "us-west-2", RegionInfo.builder()
                            .status(RegionStatus.ACTIVE)
                            .sseKmsKeyId(ReadHandler.DEFAULT_KMS_KEY_ID)
                            .build()
                    )
                )
                .build()
        )
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getResponse);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(
        ResourceModel.builder()
            .arn("arn")
            .deletionProtected(true)
            .regions(
                ImmutableSet.of(
                    new ReplicationRegion("us-east-1", null),
                    new ReplicationRegion("us-west-2", null)
                )
            )
            .build()
    );
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }
}
