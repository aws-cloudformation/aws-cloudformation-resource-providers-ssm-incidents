package software.amazon.ssmincidents.replicationset;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.CreateReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.CreateReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ListReplicationSetsRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListReplicationSetsResponse;
import software.amazon.awssdk.services.ssmincidents.model.ServiceQuotaExceededException;
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

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
        ResourceHandlerRequest<ResourceModel> request,
        CallbackContext callbackContext,
        ProxyClient<SsmIncidentsClient> proxyClient,
        Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(createReplicationSetPreCheck(proxyClient))
            .then(initiateReplicationSetCreation(proxy, proxyClient, request.getClientRequestToken()))
            .then(waitForReplicationSetToBecomeActive(
                proxyClient,
                false,
                false,
                logger,
                "Timed out waiting for replication set to become ACTIVE"))
            .then(updateReplicationSetDeletionProtection(proxy, proxyClient, "Create", logger))
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> initiateReplicationSetCreation(
        AmazonWebServicesClientProxy proxy,
        ProxyClient<SsmIncidentsClient> proxyClient,
        String clientToken
    ) {
        return progress -> {
            // if it already has been called, it's a no-op
            if (progress.getCallbackContext().mainAPICalled()) {
                logger.log("initiateReplicationSetCreation: mainAPICalled = true, skipping.");
                return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0, progress.getResourceModel());
            }
            return proxy
                .initiate(
                    "ssm-incidents::CreateReplicationSet",
                    proxyClient,
                    progress.getResourceModel(),
                    progress.getCallbackContext()
                )
                .translateToServiceRequest(model -> Translator.translateToCreateRequest(model, clientToken))
                .makeServiceCall(callCreateReplicationSet())
                .handleError((awsRequest, exception, client, model, context) -> {
                    StringWriter sw = new StringWriter();
                    exception.printStackTrace(new PrintWriter(sw));
                    logger.log("error occurred while calling crateReplicationSet: " + sw.toString());
                    if (exception instanceof ServiceQuotaExceededException) {
                        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.AlreadyExists);
                    }
                    if (exception instanceof ValidationException) {
                        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
                    }
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
                })
                .done((awsRequest, awsResponse, client, model, context) -> {
                    logger.log("crateReplicationSet called successfully");
                    model.setArn(awsResponse.arn());
                    context.setMainAPICalled(true);
                    return ProgressEvent.defaultInProgressHandler(context, 0, model);
                });
        };
    }

    private Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> createReplicationSetPreCheck(ProxyClient<SsmIncidentsClient> proxyClient) {
        return progress -> {
            CallbackContext context = progress.getCallbackContext();
            ResourceModel model = progress.getResourceModel();
            // only check for replicationSet existence before the API call is made
            if (context.mainAPICalled()) {
                logger.log("createReplicationSetPreCheck: mainAPICalled = true, skipping.");
                return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0, progress.getResourceModel());
            }
            try {
                ListReplicationSetsResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                    ListReplicationSetsRequest.builder().build(),
                    proxyClient.client()::listReplicationSets
                );
                if ((awsResponse.replicationSetArns() != null) && !awsResponse.replicationSetArns().isEmpty()) {
                    return ProgressEvent.defaultFailureHandler(new RuntimeException("Replication set " +
                        awsResponse.replicationSetArns().get(0) +
                        " already exists in this account"), HandlerErrorCode.AlreadyExists);
                }
                logger.log("createReplicationSetPreCheck: no existing replication sets found");
                return ProgressEvent.defaultInProgressHandler(context, 0, model);
            } catch (Exception exception) {
                return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
            }
        };
    }

    private BiFunction<CreateReplicationSetRequest, ProxyClient<SsmIncidentsClient>, CreateReplicationSetResponse> callCreateReplicationSet() {
        return (awsRequest, client) -> {
            CreateReplicationSetResponse awsResponse;
            awsResponse = client.injectCredentialsAndInvokeV2(
                awsRequest,
                client.client()::createReplicationSet
            );
            logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
            return awsResponse;
        };
    }
}
