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
import software.amazon.awssdk.services.ssmincidents.model.AccessDeniedException;
import software.amazon.awssdk.services.ssmincidents.model.CreateReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.CreateReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ListReplicationSetsRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListReplicationSetsResponse;
import software.amazon.awssdk.services.ssmincidents.model.RegionInfo;
import software.amazon.awssdk.services.ssmincidents.model.RegionMapInputValue;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSet;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSetStatus;
import software.amazon.awssdk.services.ssmincidents.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionResponse;
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
public class CreateHandlerTest extends AbstractTestBase {

  private AmazonWebServicesClientProxy proxy;

  private ProxyClient<SsmIncidentsClient> proxyClient;

  private CreateHandler handler;

  @Mock
  private SsmIncidentsClient sdkClient;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);
    handler = new CreateHandler();
  }

  @AfterEach
  public void cleanup() {
    verify(sdkClient, atMost(5)).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
  public void handleRequest_AwaitStabilization() {

    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().build());

    when(
        sdkClient.createReplicationSet(any(CreateReplicationSetRequest.class))
    ).thenReturn(
        CreateReplicationSetResponse.builder()
            .arn("arn")
            .build()
    );

    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(ReplicationSet.builder()
            .status(ReplicationSetStatus.CREATING)
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build())
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class)))
        .thenReturn(getReplicationSetResponse);

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(false)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(30);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
    assertThat(response.getCallbackContext().mainAPICalled()).isTrue();
    assertThat(response.getCallbackContext().getAwaitRetryAttemptsRemaining()).isEqualTo(239);
    assertThat(response.getResourceModel().getArn()).isEqualTo("arn");
    assertThat(response.getResourceModel().getDeletionProtected()).isFalse();
    assertThat(response.getResourceModel().getRegions()).isEqualTo(model.getRegions());

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();

    ArgumentCaptor<CreateReplicationSetRequest> createRequest = ArgumentCaptor.forClass(CreateReplicationSetRequest.class);
    verify(sdkClient).createReplicationSet(createRequest.capture());
    assertThat(createRequest.getValue())
        .isNotNull();
    assertThat(createRequest.getValue().regions())
        .isNotNull()
        .hasSize(1)
        .isEqualTo(
            ImmutableMap.of("us-east-1", RegionMapInputValue.builder().build())
        );

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue()).isNotNull();
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_AwaitStabilization_StillCreating() {

    ReplicationSet replicationSetResponse = ReplicationSet.builder()
        .status(ReplicationSetStatus.CREATING)
        .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
        .build();
    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(replicationSetResponse)
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))).thenReturn(getReplicationSetResponse);

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(false)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    CallbackContext context = new CallbackContext(239, true);

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(30);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    assertThat(response.getCallbackContext().mainAPICalled()).isTrue();
    assertThat(response.getCallbackContext().getAwaitRetryAttemptsRemaining()).isEqualTo(238);

    assertThat(response.getResourceModel().getArn()).isEqualTo("arn");
    assertThat(response.getResourceModel().getDeletionProtected()).isFalse();
    assertThat(response.getResourceModel().getRegions()).isEqualTo(model.getRegions());

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getValue()).isNotNull();
    assertThat(getRequest.getValue().arn()).isEqualTo("arn");
  }


  @Test
  public void handleRequest_AwaitStabilization_CreationComplete() {

    ReplicationSet replicationSetResponse1 = ReplicationSet.builder()
        .deletionProtected(false)
        .status(ReplicationSetStatus.ACTIVE)
        .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
        .build();
    ReplicationSet replicationSetResponse2 = ReplicationSet.builder()
        .deletionProtected(true)
        .status(ReplicationSetStatus.ACTIVE)
        .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
        .build();
    GetReplicationSetResponse getReplicationSetResponse1 = GetReplicationSetResponse.builder()
        .replicationSet(replicationSetResponse1)
        .build();
    GetReplicationSetResponse getReplicationSetResponse2 = GetReplicationSetResponse.builder()
        .replicationSet(replicationSetResponse2)
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))).thenReturn(getReplicationSetResponse1, getReplicationSetResponse2);

    when(sdkClient.updateDeletionProtection(any(UpdateDeletionProtectionRequest.class)))
        .thenReturn(UpdateDeletionProtectionResponse.builder().build());

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(true)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    CallbackContext context = new CallbackContext(239, true);

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    assertThat(response.getCallbackContext()).isNull();

    assertThat(response.getResourceModel().getArn()).isEqualTo("arn");
    assertThat(response.getResourceModel().getDeletionProtected()).isTrue();
    assertThat(response.getResourceModel().getRegions()).isEqualTo(model.getRegions());

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(2)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues().get(0)).isNotNull();
    assertThat(getRequest.getAllValues().get(0)).isNotNull();
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");

    ArgumentCaptor<UpdateDeletionProtectionRequest> updateDeletionProtectionRequest =
        ArgumentCaptor.forClass(UpdateDeletionProtectionRequest.class);
    verify(sdkClient).updateDeletionProtection(updateDeletionProtectionRequest.capture());
    assertThat(updateDeletionProtectionRequest.getValue()).isNotNull();
    assertThat(updateDeletionProtectionRequest.getValue().arn()).isEqualTo("arn");
    assertThat(updateDeletionProtectionRequest.getValue().deletionProtected()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void handleRequest_AwaitStabilization_CreationTimeout() {

    ReplicationSet replicationSetResponse = ReplicationSet.builder()
        .deletionProtected(false)
        .status(ReplicationSetStatus.CREATING)
        .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
        .build();
    GetReplicationSetResponse getReplicationSetResponse = GetReplicationSetResponse.builder()
        .replicationSet(replicationSetResponse)
        .build();

    when(sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))).thenReturn(getReplicationSetResponse);

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(true)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

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
  public void handleRequest_SimpleSuccess() {
    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().build());

    when(
        sdkClient.createReplicationSet(any(CreateReplicationSetRequest.class))
    ).thenReturn(
        CreateReplicationSetResponse.builder()
            .arn("arn")
            .build()
    );

    when(
        sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))
    ).thenReturn(
        GetReplicationSetResponse.builder()
            .replicationSet(
                ReplicationSet.builder()
                    .deletionProtected(false)
                    .status(ReplicationSetStatus.ACTIVE)
                    .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
                    .build()
            )
            .build()
    );

    ResourceModel model = ResourceModel.builder()
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();

    ArgumentCaptor<CreateReplicationSetRequest> createRequest = ArgumentCaptor.forClass(CreateReplicationSetRequest.class);
    verify(sdkClient).createReplicationSet(createRequest.capture());
    assertThat(createRequest.getValue())
        .isNotNull();
    assertThat(createRequest.getValue().regions())
        .isNotNull()
        .hasSize(1)
        .isEqualTo(
            ImmutableMap.of("us-east-1", RegionMapInputValue.builder().build())
        );

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(2)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues().get(0)).isNotNull();
    assertThat(getRequest.getAllValues().get(1)).isNotNull();
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");
  }

  @Test
  public void handleRequest_ServiceQuotaException() {
    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().build());

    when(
        sdkClient.createReplicationSet(any(CreateReplicationSetRequest.class))
    ).thenThrow(ServiceQuotaExceededException.builder().message("test limit exceeded").build());

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(false)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getMessage()).isEqualTo("test limit exceeded");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();

    ArgumentCaptor<CreateReplicationSetRequest> createRequest = ArgumentCaptor.forClass(CreateReplicationSetRequest.class);
    verify(sdkClient).createReplicationSet(createRequest.capture());
    assertThat(createRequest.getValue())
        .isNotNull();
    assertThat(createRequest.getValue().regions())
        .isNotNull()
        .hasSize(1)
        .isEqualTo(
            ImmutableMap.of("us-east-1", RegionMapInputValue.builder().build())
        );
  }

  @Test
  public void handleRequest_OtherException() {
    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().build());

    when(
        sdkClient.createReplicationSet(any(CreateReplicationSetRequest.class))
    ).thenThrow(AccessDeniedException.builder().message("test access denied").build());

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(false)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getMessage()).isEqualTo("test access denied");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();

    ArgumentCaptor<CreateReplicationSetRequest> createRequest = ArgumentCaptor.forClass(CreateReplicationSetRequest.class);
    verify(sdkClient).createReplicationSet(createRequest.capture());
    assertThat(createRequest.getValue())
        .isNotNull();
    assertThat(createRequest.getValue().regions())
        .isNotNull()
        .hasSize(1)
        .isEqualTo(
            ImmutableMap.of("us-east-1", RegionMapInputValue.builder().build())
        );
  }

  @Test
  public void handleRequest_NonEmptyList() {
    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().replicationSetArns("arn").build());

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(false)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getMessage()).isEqualTo("Replication set arn already exists in this account");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();
  }

  @Test
  public void handleRequest_SetDeletionProtection() {
    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().replicationSetArns().build());

    when(
        sdkClient.createReplicationSet(any(CreateReplicationSetRequest.class))
    ).thenReturn(
        CreateReplicationSetResponse.builder()
            .arn("arn")
            .build()
    );

    when(
        sdkClient.getReplicationSet(any(GetReplicationSetRequest.class))
    ).thenReturn(
        GetReplicationSetResponse.builder()
            .replicationSet(
                ReplicationSet.builder()
                    .deletionProtected(false)
                    .status(ReplicationSetStatus.ACTIVE)
                    .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
                    .build()
            )
            .build()
    );

    when(sdkClient.updateDeletionProtection(any(UpdateDeletionProtectionRequest.class)))
        .thenReturn(UpdateDeletionProtectionResponse.builder().build());

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(true)
        .regions(ImmutableSet.of(new ReplicationRegion("us-east-1", RegionConfiguration.builder().build())))
        .arn("arn")
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();

    ArgumentCaptor<CreateReplicationSetRequest> createRequest = ArgumentCaptor.forClass(CreateReplicationSetRequest.class);
    verify(sdkClient).createReplicationSet(createRequest.capture());
    assertThat(createRequest.getValue())
        .isNotNull();
    assertThat(createRequest.getValue().regions())
        .isNotNull()
        .hasSize(1)
        .isEqualTo(
            ImmutableMap.of("us-east-1", RegionMapInputValue.builder().build())
        );

    ArgumentCaptor<GetReplicationSetRequest> getRequest = ArgumentCaptor.forClass(GetReplicationSetRequest.class);
    verify(sdkClient, times(2)).getReplicationSet(getRequest.capture());
    assertThat(getRequest.getAllValues().get(0)).isNotNull();
    assertThat(getRequest.getAllValues().get(1)).isNotNull();
    assertThat(getRequest.getAllValues().get(0).arn()).isEqualTo("arn");
    assertThat(getRequest.getAllValues().get(1).arn()).isEqualTo("arn");

    ArgumentCaptor<UpdateDeletionProtectionRequest> updateRequest = ArgumentCaptor.forClass(UpdateDeletionProtectionRequest.class);
    verify(sdkClient).updateDeletionProtection(updateRequest.capture());
    assertThat(updateRequest.getValue()).isNotNull();
    assertThat(updateRequest.getValue().arn()).isEqualTo("arn");
    assertThat(updateRequest.getValue().deletionProtected()).isEqualTo(true);
  }

  @Test
  public void handleRequest_ValidationException() {
    when(
        sdkClient.listReplicationSets(any(ListReplicationSetsRequest.class))
    ).thenReturn(ListReplicationSetsResponse.builder().build());

    when(
        sdkClient.createReplicationSet(any(CreateReplicationSetRequest.class))
    ).thenThrow(ValidationException.builder().message("test validation exception").build());

    ResourceModel model = ResourceModel.builder()
        .deletionProtected(false)
        .regions(ImmutableSet.of(new ReplicationRegion("mars-east-1", RegionConfiguration.builder().build())))
        .build();

    ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(response.getMessage()).isEqualTo("test validation exception");
    assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

    ArgumentCaptor<ListReplicationSetsRequest> listRequest = ArgumentCaptor.forClass(ListReplicationSetsRequest.class);
    verify(sdkClient).listReplicationSets(listRequest.capture());
    assertThat(listRequest.getValue()).isNotNull();

    ArgumentCaptor<CreateReplicationSetRequest> createRequest = ArgumentCaptor.forClass(CreateReplicationSetRequest.class);
    verify(sdkClient).createReplicationSet(createRequest.capture());
    assertThat(createRequest.getValue())
        .isNotNull();
    assertThat(createRequest.getValue().regions())
        .isNotNull()
        .hasSize(1)
        .isEqualTo(
            ImmutableMap.of("mars-east-1", RegionMapInputValue.builder().build())
        );
  }
}
