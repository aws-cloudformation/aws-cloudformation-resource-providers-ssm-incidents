package software.amazon.ssmincidents.replicationset;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.ListReplicationSetsRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListReplicationSetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

  @Override
  public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      AmazonWebServicesClientProxy proxy,
      ResourceHandlerRequest<ResourceModel> request,
      CallbackContext callbackContext,
      ProxyClient<SsmIncidentsClient> proxyClient,
      Logger logger
  ) {

    try {
      ListReplicationSetsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
          ListReplicationSetsRequest.builder().build(),
          req -> proxyClient.client().listReplicationSets(req)
      );

      ImmutableList.Builder<ResourceModel> modelsBuilder = ImmutableList.builder();
      if (awsResponse.replicationSetArns() != null) {
        awsResponse.replicationSetArns().forEach(arn -> modelsBuilder.add(ResourceModel.builder().arn(arn).build()));
      }
      return ProgressEvent.<ResourceModel, CallbackContext>builder()
          .resourceModels(modelsBuilder.build())
          .nextToken(awsResponse.nextToken())
          .status(OperationStatus.SUCCESS)
          .build();
    } catch (Exception exception) {
      return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
    }
  }
}
