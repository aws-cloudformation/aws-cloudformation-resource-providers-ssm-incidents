package software.amazon.ssmincidents.responseplan;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import com.google.common.collect.Sets;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.TagResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.UntagResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanResponse;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
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

public class UpdateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SsmIncidentsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon
        // /cloudformation/proxy/CallChain.java
        if (request.getDesiredResourceState().getArn() == null) {
            return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext, HandlerErrorCode.NotFound, "Not Found");
        }
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            // STEP 1 [check if resource already exists]
            // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            // if target API does not support 'ResourceNotFoundException' then following check is required
            .then(progress ->
                      // STEP 1.0 [initialize a proxy context]
                      // If your service API does not return ResourceNotFoundException on update requests against some identifier (e.g; resource Name)
                      // and instead returns a 200 even though a resource does not exist, you must first check if the resource exists here
                      // NOTE: If your service API throws 'ResourceNotFoundException' for update requests this method is not necessary
                      proxy.initiate("AWS-SSMIncidents-ResponsePlan::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())

                          // STEP 1.1 [initialize a proxy context]
                          .translateToServiceRequest(Translator::translateToReadRequest)

                          // STEP 1.2 [TODO: make an api call]
                          .makeServiceCall((awsRequest, client) -> {
                              AwsResponse awsResponse = null;

                              // TODO: add custom read resource logic
                              // If describe request does not return ResourceNotFoundException, you must throw ResourceNotFoundException based on
                              // awsResponse values
                              awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getResponsePlan);
                              logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                              return awsResponse;
                          })
                          .handleError((awsRequest, exception, client, model, context) -> {
                              if (exception instanceof ResourceNotFoundException)
                                  return ProgressEvent.failed(model, context, HandlerErrorCode.NotFound, "Not Found");
                              throw Translator.handleException(exception);
                          })
                          .progress()
            )
            // STEP 2 [first update/stabilize progress chain - required for resource update]
            .then(progress ->
                      proxy.initiate("AWS-SSMIncidents-ResponsePlan::Update::first", proxyClient, progress.getResourceModel(),
                              progress.getCallbackContext())
                          .translateToServiceRequest(Translator::translateToFirstUpdateRequest)
                          .makeServiceCall((awsRequest, client) -> {
                              UpdateResponsePlanResponse awsResponse = null;
                              try {
                                  awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateResponsePlan);
                              } catch (final Exception e) {
                                  throw Translator.handleException(e);
                              }
                              logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
                              return awsResponse;
                          })
                          .done(
                              (updateResponsePlanRequest, updateResponsePlanResponse, client, model, context) ->
                                  ProgressEvent.defaultInProgressHandler(
                                      context,
                                      0,
                                      updateModelWithArn(model, request.getDesiredResourceState().getArn())
                                  )
                          )
            )
            .then(progress -> updateTags(proxyClient, progress, request))
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateTags(ProxyClient<SsmIncidentsClient> proxyClient, ProgressEvent<ResourceModel, CallbackContext> progress, ResourceHandlerRequest<ResourceModel> request) {
        String responsePlanArn = request.getDesiredResourceState().getArn();
        Set<Tag> desiredTags = Optional.ofNullable(request.getDesiredResourceState().getTags()).orElse(new HashSet<>());
        Set<Tag> previousTags = Optional.ofNullable(request.getPreviousResourceState()).map(x -> x.getTags()).orElse(new HashSet<>());

        Map<String, String> tagsToAdd = Translator.toApiTag(Sets.difference(desiredTags, previousTags));
        if (!tagsToAdd.isEmpty()) {
            try {
                proxyClient.injectCredentialsAndInvokeV2(TagResourceRequest.builder().resourceArn(responsePlanArn).tags(tagsToAdd).build(), proxyClient.client()::tagResource);
            } catch (ValidationException e) {
                throw Translator.handleException(e);
            }
        }

        Map<String, String> tagsToRemove = Translator.toApiTag(Sets.difference(previousTags, desiredTags));
        if (!tagsToRemove.isEmpty()) {
            try {
                proxyClient.injectCredentialsAndInvokeV2(UntagResourceRequest.builder().resourceArn(responsePlanArn).tagKeys(tagsToRemove.keySet()).build(), proxyClient.client()::untagResource);
            } catch (ValidationException e) {
                throw Translator.handleException(e);
            }
        }
        return ProgressEvent.progress(request.getDesiredResourceState(), progress.getCallbackContext());
    }
}
