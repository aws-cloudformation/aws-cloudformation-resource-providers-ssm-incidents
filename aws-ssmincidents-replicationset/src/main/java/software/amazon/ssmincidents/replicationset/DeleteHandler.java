package software.amazon.ssmincidents.replicationset;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.DeleteReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.DeleteReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DeleteHandler extends BaseHandlerStd {
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
                "Timed out waiting for replication set become ACTIVE")
            )
            .then(initiateReplicationSetDeletion(proxy, proxyClient))
            .then(waitForReplicationSetToBecomeActive(
                proxyClient,
                true,
                false,
                logger,
                "Timed out waiting for replication set to be deleted")
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> initiateReplicationSetDeletion(AmazonWebServicesClientProxy proxy, ProxyClient<SsmIncidentsClient> proxyClient) {
        return progress -> {
            if (progress.getCallbackContext().mainAPICalled()) {
                logger.log("initiateReplicationSetDeletion: mainAPICalled = true, skipping.");
                return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0, progress.getResourceModel());
            }
            logger.log("initiateReplicationSetDeletion: making deleteReplicaitonSet call");
            return proxy.initiate(
                    "ssm-incidents::DeleteReplicationSet",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext()
                )
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall(callDeleteReplicationSet())
                .handleError((awsRequest, exception, client, model, context) -> {
                    StringWriter sw = new StringWriter();
                    exception.printStackTrace(new PrintWriter(sw));
                    logger.log("initiateReplicationSetDeletion: exception occurred while calling deleteReplicationSet: " + sw);
                    if (exception instanceof ResourceNotFoundException) {
                        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
                    }
                    if (exception instanceof ValidationException) {
                        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
                    }
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
                })
                .done((awsRequest, awsResponce, client, model, context) -> {
                    logger.log("initiateReplicationSetDeletion: deleteReplicationSet call was successful.");
                    context.setMainAPICalled(true);
                    return ProgressEvent.defaultInProgressHandler(context, 0, model);
                });
        };
    }

    private BiFunction<DeleteReplicationSetRequest, ProxyClient<SsmIncidentsClient>, DeleteReplicationSetResponse> callDeleteReplicationSet() {
        return (request, client) -> client.injectCredentialsAndInvokeV2(request, client.client()::deleteReplicationSet);
    }
}
