package software.amazon.ssmincidents.replicationset;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.TagResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.UntagResourceRequest;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

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
            .then(progress -> updateTags(proxyClient, progress, request))
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

    private ProgressEvent<ResourceModel, CallbackContext> updateTags(ProxyClient<SsmIncidentsClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress, ResourceHandlerRequest<ResourceModel> request) {
        String replicationSetArn = request.getDesiredResourceState().getArn();
        Set<Tag> desiredTags = Optional.ofNullable(request.getDesiredResourceState().getTags()).orElse(new HashSet<>());
        Set<Tag> previousTags = Optional.ofNullable(request.getPreviousResourceState()).map(x -> x.getTags()).orElse(new HashSet<>());

        Set<Tag> tagsToAddSet = Sets.difference(desiredTags, previousTags);
        Map<String, String> tagsToAdd = tagsToAddSet.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        if (!tagsToAdd.isEmpty()) {
            try {
                proxyClient.injectCredentialsAndInvokeV2(TagResourceRequest.builder().resourceArn(replicationSetArn).tags(tagsToAdd).build(), proxyClient.client()::tagResource);
            } catch (ValidationException exception) {
                return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
            }
        }

        Set<Tag> tagsToRemoveSet = Sets.difference(previousTags, desiredTags);
        Map<String, String> tagsToRemove = tagsToRemoveSet.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        if (!tagsToRemove.isEmpty()) {
            try {
                proxyClient.injectCredentialsAndInvokeV2(UntagResourceRequest.builder().resourceArn(replicationSetArn).tagKeys(tagsToRemove.keySet()).build(), proxyClient.client()::untagResource);
            } catch (ValidationException exception) {
                return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

            }
        }
        return ProgressEvent.progress(request.getDesiredResourceState(), progress.getCallbackContext());
    }
}
