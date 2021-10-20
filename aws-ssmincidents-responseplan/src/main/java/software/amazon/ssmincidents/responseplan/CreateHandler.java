package software.amazon.ssmincidents.responseplan;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {

  private Logger logger;

  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<SsmIncidentsClient> proxyClient,
      final Logger logger) {

    this.logger = logger;
    if (request.getDesiredResourceState().getArn() != null) {
      throw new CfnInvalidRequestException("Attempting to set a ReadOnly Property.");
    }

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(progress ->
            proxy.initiate("AWS-SSMIncidents-ResponsePlan::Create", proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((awsRequest, client) -> {
                  CreateResponsePlanResponse awsResponse = null;
                  try {
                    awsResponse = client
                        .injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createResponsePlan);
                  } catch (final Exception e) {
                    e.printStackTrace();
                    throw Translator.handleException(e);
                  }

                  logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
                  return awsResponse;
                })
                .done((createResponsePlanRequest, createResponsePlanResponse, client, model, context) -> ProgressEvent.defaultInProgressHandler(context, 0, updateModelWithArn(model, createResponsePlanResponse.arn())))
        )
        .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
  }
}
