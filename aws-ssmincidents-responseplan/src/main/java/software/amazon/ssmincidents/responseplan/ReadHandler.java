package software.amazon.ssmincidents.responseplan;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SsmIncidentsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        return proxy
            .initiate("AWS-SSMIncidents-ResponsePlan::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, client) -> {
                GetResponsePlanResponse awsResponse = null;
                try {
                    awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getResponsePlan);
                } catch (final Exception e) {
                    throw Translator.handleException(e);
                }
                logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                return Translator.mergeTags(awsResponse, getTags(awsRequest.arn(), client));
            })
            .done(combinedResourceModel -> ProgressEvent.defaultSuccessHandler(combinedResourceModel));
    }

    private ListTagsForResourceResponse getTags(String arn, ProxyClient<SsmIncidentsClient> proxyClient) {
        return proxyClient.injectCredentialsAndInvokeV2(
            ListTagsForResourceRequest.builder().resourceArn(arn).build(),
            proxyClient.client()::listTagsForResource);
    }
}
