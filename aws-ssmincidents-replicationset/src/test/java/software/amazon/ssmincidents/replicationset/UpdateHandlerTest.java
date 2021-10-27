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
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionResponse;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetResponse;
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
public class UpdateHandlerTest extends AbstractTestBase {

  private AmazonWebServicesClientProxy proxy;

  private ProxyClient<SsmIncidentsClient> proxyClient;

  @Mock
  private SsmIncidentsClient sdkClient;

  private UpdateHandler handler;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);
    handler = new UpdateHandler();
  }

  @AfterEach
  public void cleanup() {
    verify(sdkClient, atMost(5)).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
  public void handleRequest_StabilizeBeforeUpdate() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(
            GetReplicationSetResponse.builder()
                .replicationSet(
                    ReplicationSet.builder()
                        .status(ReplicationSetStatus.UPDATING)
                        .build())
                .build()
        );

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(30);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext()).isEqualTo(new CallbackContext(239, null));

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue()).isNotNull();
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_AddRegion_StabilizationBeforeUpdateTimedOut() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().sseKmsKeyId("kms-key-id-us-west-2").build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    GetReplicationSetResponse updatingGetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.UPDATING)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder().build(),
                        "us-west-2", RegionInfo.builder().build()
                    )
                )
                .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(updatingGetResponse);

    CallbackContext context = new CallbackContext(1, false);

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isEqualTo("Timed out waiting for replication set to become ACTIVE");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotStabilized);
    assertThat(response.getCallbackContext()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue()).isNotNull();
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_AddRegion_StabilizationBeforeUpdateComplete() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().sseKmsKeyId("kms-key-id-us-west-2").build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    GetReplicationSetResponse activeGetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.ACTIVE)
                .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
                .build())
        .build();
    GetReplicationSetResponse updatingGetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.UPDATING)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder().build(),
                        "us-west-2", RegionInfo.builder().build()
                    )
                )
                .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(activeGetResponse, activeGetResponse, updatingGetResponse);

    when(sdkClient.updateReplicationSet(any(UpdateReplicationSetRequest.class)))
        .thenReturn(UpdateReplicationSetResponse.builder().build());

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(30);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext()).isEqualTo(new CallbackContext(239, true));

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(3)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues()).isNotNull().hasSize(3);
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(2).arn()).isEqualTo("arn");

    ArgumentCaptor<UpdateReplicationSetRequest> updateRequest =
        ArgumentCaptor.forClass(UpdateReplicationSetRequest.class);
    verify(sdkClient).updateReplicationSet(updateRequest.capture());
    assertThat(updateRequest.getValue()).isNotNull();
    assertThat(updateRequest.getValue().arn()).isEqualTo("arn");
    assertThat(updateRequest.getValue().actions()).isNotNull().hasSize(1);
    assertThat(updateRequest.getValue().actions().get(0).addRegionAction()).isNotNull();
    assertThat(updateRequest.getValue().actions().get(0).deleteRegionAction()).isNull();
    assertThat(updateRequest.getValue().actions().get(0).addRegionAction().regionName()).isEqualTo("us-west-2");
    assertThat(updateRequest.getValue().actions().get(0).addRegionAction().sseKmsKeyId()).isEqualTo("kms-key-id-us-west-2");
  }

  @Test
  public void handleRequest_DeleteRegion_StabilizationBeforeUpdateComplete() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    GetReplicationSetResponse activeGetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.ACTIVE)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder().build(),
                        "us-west-2", RegionInfo.builder().build()
                    )
                )
                .build())
        .build();
    GetReplicationSetResponse updatingGetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.UPDATING)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder().build(),
                        "us-west-2", RegionInfo.builder().build()
                    )
                )
                .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(activeGetResponse, activeGetResponse, updatingGetResponse);

    when(sdkClient.updateReplicationSet(any(UpdateReplicationSetRequest.class)))
        .thenReturn(UpdateReplicationSetResponse.builder().build());

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(30);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext()).isEqualTo(new CallbackContext(239, true));

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(3)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues()).isNotNull().hasSize(3);
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(2).arn()).isEqualTo("arn");

    ArgumentCaptor<UpdateReplicationSetRequest> updateRequest =
        ArgumentCaptor.forClass(UpdateReplicationSetRequest.class);
    verify(sdkClient).updateReplicationSet(updateRequest.capture());
    assertThat(updateRequest.getValue()).isNotNull();
    assertThat(updateRequest.getValue().arn()).isEqualTo("arn");
    assertThat(updateRequest.getValue().actions()).isNotNull().hasSize(1);
    assertThat(updateRequest.getValue().actions().get(0).addRegionAction()).isNull();
    assertThat(updateRequest.getValue().actions().get(0).deleteRegionAction()).isNotNull();
    assertThat(updateRequest.getValue().actions().get(0).deleteRegionAction().regionName()).isEqualTo("us-west-2");
  }

  @Test
  public void handleRequest_StabilizeAfterUpdate() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(
            GetReplicationSetResponse.builder()
                .replicationSet(
                    ReplicationSet.builder()
                        .status(ReplicationSetStatus.UPDATING)
                        .build())
                .build()
        );

    CallbackContext context = new CallbackContext(220, true);

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(30);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext()).isEqualTo(new CallbackContext(219, true));

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue()).isNotNull();
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_StabilizationAfterUpdateTimedOut() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().sseKmsKeyId("kms-key-id-us-west-2").build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    GetReplicationSetResponse updatingGetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .status(ReplicationSetStatus.UPDATING)
                .regionMap(
                    ImmutableMap.of(
                        "us-east-1", RegionInfo.builder().build(),
                        "us-west-2", RegionInfo.builder().build()
                    )
                )
                .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(updatingGetResponse);

    CallbackContext context = new CallbackContext(1, true);

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isEqualTo("Timed out waiting for replication set to become ACTIVE");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotStabilized);
    assertThat(response.getCallbackContext()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue()).isNotNull();
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_UpdateDeletionProtection() {
    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().build())
            )
        )
        .deletionProtected(true)
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(
            GetReplicationSetResponse.builder()
                .replicationSet(
                    ReplicationSet.builder()
                        .status(ReplicationSetStatus.ACTIVE)
                        .regionMap(
                            ImmutableMap.of(
                                "us-east-1", RegionInfo.builder().build(),
                                "us-west-2", RegionInfo.builder().build()
                            )
                        )
                        .build())
                .build()
        );

    when(sdkClient.updateDeletionProtection(any(UpdateDeletionProtectionRequest.class)))
        .thenReturn(UpdateDeletionProtectionResponse.builder().build());

    CallbackContext context = new CallbackContext(220, true);

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, context, proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(2)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues()).isNotNull().hasSize(2);
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");

    ArgumentCaptor<UpdateDeletionProtectionRequest> updateDeletionProtectionRequest =
        ArgumentCaptor.forClass(UpdateDeletionProtectionRequest.class);
    verify(sdkClient).updateDeletionProtection(updateDeletionProtectionRequest.capture());
    assertThat(updateDeletionProtectionRequest.getValue()).isNotNull();
    assertThat(updateDeletionProtectionRequest.getValue().arn()).isEqualTo("arn");
    assertThat(updateDeletionProtectionRequest.getValue().deletionProtected()).isTrue();
  }

  @Test
  public void handleRequest_noDifference() {
    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .regionMap(
                    ImmutableMap.of("us-east-1", RegionInfo.builder().status(RegionStatus.ACTIVE).build())
                )
                .deletionProtected(false)
                .status(ReplicationSetStatus.ACTIVE)
                .build())
        .build();
    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))).thenReturn(getReplicationSetResponse);

    ResourceModel model = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(model)
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getResourceModel()).isEqualTo(model);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(5)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues()).isNotNull().hasSize(5);
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(2).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(3).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(4).arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_differsByTwoRegions() {
    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(
            ReplicationSet.builder()
                .regionMap(
                    ImmutableMap.of("us-east-1", RegionInfo.builder().status(RegionStatus.ACTIVE).build())
                )
                .deletionProtected(false)
                .status(ReplicationSetStatus.ACTIVE)
                .build())
        .build();
    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))).thenReturn(getReplicationSetResponse);

    ResourceModel oldModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())
            )
        )
        .build();

    ResourceModel newModel = ResourceModel.builder()
        .arn("arn")
        .regions(
            ImmutableSet.of(
                new ReplicationRegion("us-east-1", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-west-2", RegionConfiguration.builder().build()),
                new ReplicationRegion("us-east-2", RegionConfiguration.builder().build())
            )
        )
        .deletionProtected(true)
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .previousResourceState(oldModel)
        .desiredResourceState(newModel)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response =
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isEqualTo("Invalid request provided: Replication set regions differ by more then " +
        "one region. Current replication set regions: [us-east-1]. Specified replication set regions: " +
        "[us-east-1, us-east-2, us-west-2]");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    assertThat(response.getCallbackContext()).isNull();

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(2)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues()).isNotNull().hasSize(2);
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");
  }
}
