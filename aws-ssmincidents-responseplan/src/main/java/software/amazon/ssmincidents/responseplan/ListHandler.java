package software.amazon.ssmincidents.responseplan;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.ListResponsePlansRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListResponsePlansResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SsmIncidentsClient> proxyClient,
        final Logger logger) {

        final ListResponsePlansRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

        try {
            ListResponsePlansResponse awsResponse = proxyClient
                .injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listResponsePlans);
            String nextToken = awsResponse.nextToken();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.translateFromListRequest(awsResponse))
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
        } catch (final Exception e) {
            throw Translator.handleException(e);
        }
    }
}
