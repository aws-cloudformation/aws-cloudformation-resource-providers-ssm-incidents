package software.amazon.ssmincidents.replicationset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
        ResourceHandlerRequest<ResourceModel> request,
        CallbackContext callbackContext,
        ProxyClient<SsmIncidentsClient> proxyClient,
        Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();

        // should never be null, returning NotFound according to RPDK contract test expectation
        if (model.getArn() == null) {
            return ProgressEvent.defaultFailureHandler(
                ResourceNotFoundException.builder()
                    .message("arn was null, cannot read replication set with null arn")
                    .build(),
                HandlerErrorCode.NotFound
            );
        }

        GetReplicationSetRequest awsRequest = Translator.translateToReadRequest(model);
        try {
            GetReplicationSetResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest,
                proxyClient.client()::getReplicationSet
            );
            Set<ReplicationRegion> replicationRegions = awsResponse.replicationSet().regionMap().entrySet().stream().map(
                    regionConfig ->
                        new ReplicationRegion(
                            regionConfig.getKey(),
                            Optional.ofNullable(regionConfig.getValue().sseKmsKeyId())
                                .map(RegionConfiguration::new)
                                .orElse(null)
                        )
                )
                .collect(Collectors.toSet());
            Map<String, String> tagMap = getTags(awsRequest.arn(), proxyClient).tags();
            Set<Tag> tags = tagMap.isEmpty()? null : tagMap.entrySet().stream().map(x -> new Tag(x.getKey(), x.getValue())).collect(Collectors.toSet());

            model.setArn(awsRequest.arn());
            model.setDeletionProtected(awsResponse.replicationSet().deletionProtected());
            model.setRegions(ImmutableSet.copyOf(replicationRegions));
            model.setTags(Optional.ofNullable(tags).orElse(null));
            return ProgressEvent.defaultSuccessHandler(model);

        } catch (ResourceNotFoundException exception) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
        } catch (ValidationException exception) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
        } catch (Exception exception) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }

    @VisibleForTesting
    static final String DEFAULT_KMS_KEY_ID = "DefaultKey";

    private ListTagsForResourceResponse getTags(String arn, ProxyClient<SsmIncidentsClient> proxyClient) {
        return proxyClient.injectCredentialsAndInvokeV2(
            ListTagsForResourceRequest.builder().resourceArn(arn).build(),
            proxyClient.client()::listTagsForResource);
    }
}
