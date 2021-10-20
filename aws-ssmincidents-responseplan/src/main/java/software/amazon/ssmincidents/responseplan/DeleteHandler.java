package software.amazon.ssmincidents.responseplan;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

  private Logger logger;

  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<SsmIncidentsClient> proxyClient,
      final Logger logger) {

    this.logger = logger;

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        // STEP 1 [check if resource already exists]
        // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
        // if target API does not support 'ResourceNotFoundException' then following check is required
        .then(progress ->
            // STEP 1.0 [initialize a proxy context]
            // If your service API does not return ResourceNotFoundException on delete requests against some identifier (e.g; resource Name)
            // and instead returns a 200 even though a resource already deleted, you must first check if the resource exists here
            // NOTE: If your service API throws 'ResourceNotFoundException' for delete requests this method is not necessary
            proxy.initiate("AWS-SSMIncidents-ResponsePlan::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                // STEP 1.1 [initialize a proxy context]
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, client) -> {
                    AwsResponse awsResponse = null;
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
        .then(progress ->
            proxy.initiate("AWS-SSMIncidents-ResponsePlan::Delete", proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((awsRequest, client) -> {
                  AwsResponse awsResponse = null;
                  try {
                    awsResponse = client
                        .injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteResponsePlan);
                  } catch (final Exception e) {
                      throw Translator.handleException(e);
                  }

                  logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
                  return awsResponse;
                })
                .progress()
        )
        .then(progress -> ProgressEvent.defaultSuccessHandler(null));
  }
}
