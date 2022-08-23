package software.amazon.ssmincidents.replicationset;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.BiFunction;
import java.util.function.Function;

public class UpdateHandler extends BaseHandlerStd {
  private Logger logger;

  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      AmazonWebServicesClientProxy proxy,
      ResourceHandlerRequest<ResourceModel> request,
      CallbackContext callbackContext,
      ProxyClient<SsmIncidentsClient> proxyClient,
      Logger logger) {

    this.logger = logger;
    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(waitForReplicationSetToBecomeActive(
            proxyClient,
            false,
            true,
            logger,
            "Timed out waiting for replication set to become ACTIVE"))
        .then(initiateUpdateReplicationSet(proxy, proxyClient, request.getClientRequestToken()))
        .then(waitForReplicationSetToBecomeActive(
            proxyClient,
            false,
            false,
            logger,
            "Timed out waiting for replication set to become ACTIVE")
        )
        .then(updateReplicationSetDeletionProtection(proxy, proxyClient, "Update", logger))
        .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
  }

  private Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> initiateUpdateReplicationSet(
      AmazonWebServicesClientProxy proxy,
      ProxyClient<SsmIncidentsClient> proxyClient,
      String clientToken
  ) {
    return progress -> {
      if (progress.getCallbackContext().mainAPICalled()) {
        return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0, progress.getResourceModel());
      }
      try {
        return proxy.initiate("ssm-incidents::UpdateReplicationSet",
            proxyClient,
            progress.getResourceModel(),
            progress.getCallbackContext()
        )
            .translateToServiceRequest(model -> {
              if (!progress.getCallbackContext().mainAPICalled()) {
                GetReplicationSetResponse currentReplicationSet = proxyClient.injectCredentialsAndInvokeV2(
                    Translator.translateToReadRequest(model),
                    proxyClient.client()::getReplicationSet
                );
                return Translator.translateToUpdateRequest(currentReplicationSet.replicationSet(), model, clientToken);
              } else {
                return null;
              }
            })
            .makeServiceCall(callUpdateReplicationSet())
            .handleError((awsRequest, exception, client, model, context) -> {
              if (exception instanceof ResourceNotFoundException) {
                return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
              }
              if (exception instanceof ValidationException) {
                return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
              }
              return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
            })
            .done((awsRequest, awsResponse, client, model, context) -> {
              context.setMainAPICalled(true);
              return ProgressEvent.defaultInProgressHandler(context, 0, model);
            });
      } catch (CfnInvalidRequestException e) {
        return ProgressEvent.defaultFailureHandler(e, e.getErrorCode());
      }
    };
  }

  private BiFunction<UpdateReplicationSetRequest, ProxyClient<SsmIncidentsClient>, UpdateReplicationSetResponse> callUpdateReplicationSet() {
    return (awsRequest, client) -> {
      if (awsRequest == null) {
        return null;
      }
      UpdateReplicationSetResponse awsResponse =
          client.injectCredentialsAndInvokeV2(
              awsRequest,
              client.client()::updateReplicationSet
          );
      logger.log(String.format("%s update has initiated successfully", ResourceModel.TYPE_NAME));
      return awsResponse;
    };
  }
}
