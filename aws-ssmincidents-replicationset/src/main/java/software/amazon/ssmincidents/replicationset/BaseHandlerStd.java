package software.amazon.ssmincidents.replicationset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetResponse;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSetStatus;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionResponse;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.ssmincidents.replicationset.util.AwsObjectsSerializerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  private final static ObjectMapper objectMapper = new ObjectMapper();

  private static final String KMS_KEY_REDACTED_PLACEHOLDER = "<PROVIDED>";

  static {
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializerFactory(new AwsObjectsSerializerFactory(objectMapper.getSerializerFactory()));
  }

  // total await time of 2 hours
  protected final static int INITIAL_AWAIT_COUNT = 240;
  protected final static int RETRY_DELAY_SECONDS = 30;

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      AmazonWebServicesClientProxy proxy,
      ResourceHandlerRequest<ResourceModel> request,
      CallbackContext callbackContext,
      Logger logger) {
    try {
      logger.log("Request from CFN: " + objectMapper.writeValueAsString(redactRequest(request)));
      logger.log("Callback content: " + objectMapper.writeValueAsString(callbackContext));
    } catch (JsonProcessingException e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      logger.log("Exception: " + sw);
    }
    ProgressEvent<ResourceModel, CallbackContext> res;
    try {
      res = handleRequest(
          proxy,
          request,
          callbackContext != null ? callbackContext : new CallbackContext(),
          proxy.newProxy(ClientBuilder::getClient),
          logger
      );
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      logger.log("Unhandled exception in handler: " + sw);
      res = ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.ServiceInternalError);
    }
    try {
      logger.log("Returning status: " + res.getStatus().name());
      if (res.getResourceModel() != null) {
        logger.log("Returning model definition: " +
            objectMapper.writeValueAsString(redactModel(res.getResourceModel()))
        );
      }
      if (res.getCallbackContext() != null) {
        logger.log("Returning callback content: " + objectMapper.writeValueAsString(res.getCallbackContext()));
      }
      if (res.getErrorCode() != null) {
        logger.log("Returning errorCode: " + res.getErrorCode().name());
      }
      if (res.getResourceModels() != null) {
        logger.log("Returning models: " + res.getResourceModels());
      }
    } catch (JsonProcessingException e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      logger.log("Exception: " + sw);
    }
    return res;
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      AmazonWebServicesClientProxy proxy,
      ResourceHandlerRequest<ResourceModel> request,
      CallbackContext callbackContext,
      ProxyClient<SsmIncidentsClient> proxyClient,
      Logger logger
  );

  protected Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> waitForReplicationSetToBecomeActive(
      ProxyClient<SsmIncidentsClient> proxyClient,
      boolean notFoundIsOkay,
      boolean beforeMainCall,
      Logger logger,
      String timeoutMessage
  ) {
    return progress -> {
      CallbackContext context = progress.getCallbackContext();
      ResourceModel model = progress.getResourceModel();
      // skip if await should be completed before main API call
      // and the main API call ahs already be done
      if (beforeMainCall & context.mainAPICalled()) {
        logger.log("waitForReplicationSetToBecomeActive: beforeMainCall requested, but main call was made already. Skipping.");
        return progress;
      }
      if (context.getAwaitRetryAttemptsRemaining() == null) {
        logger.log("waitForReplicationSetToBecomeActive: setting attempts to " + INITIAL_AWAIT_COUNT);
        context.setAwaitRetryAttemptsRemaining(INITIAL_AWAIT_COUNT);
      }
      try {
        GetReplicationSetRequest awsRequest = Translator.translateToReadRequest(progress.getResourceModel());
        GetReplicationSetResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
            awsRequest,
            req -> proxyClient.client().getReplicationSet(req)
        );
        ReplicationSetStatus status = awsResponse.replicationSet().status();
        logger.log("waitForReplicationSetToBecomeActive: replicationSet status = " + status.name());
        if (status == ReplicationSetStatus.ACTIVE) {
          logger.log("waitForReplicationSetToBecomeActive: removing remaining attempts");
          context.setAwaitRetryAttemptsRemaining(null);
          return ProgressEvent.defaultInProgressHandler(context, 0, model);
        }
        if (status == ReplicationSetStatus.FAILED) {
          logger.log("waitForReplicationSetToBecomeActive: replication set failed");
          return ProgressEvent.defaultFailureHandler(
              new RuntimeException("Replication Set creation failed"),
              HandlerErrorCode.NotStabilized
          );
        }
        context.setAwaitRetryAttemptsRemaining(context.getAwaitRetryAttemptsRemaining() - 1);
        logger.log("waitForReplicationSetToBecomeActive: decremented remaining attempt count to " +
            context.getAwaitRetryAttemptsRemaining());
        if (context.getAwaitRetryAttemptsRemaining() <= 0) {
          logger.log("waitForReplicationSetToBecomeActive: timed out waiting for replication set to become active");
          return ProgressEvent.defaultFailureHandler(
              new RuntimeException(timeoutMessage),
              HandlerErrorCode.NotStabilized
          );
        }
        logger.log("waitForReplicationSetToBecomeActive: Returning delay in seconds = " + RETRY_DELAY_SECONDS);
        return ProgressEvent.defaultInProgressHandler(context, RETRY_DELAY_SECONDS, model);
      } catch (ResourceNotFoundException e) {
        if (notFoundIsOkay) {
          logger.log("waitForReplicationSetToBecomeActive: replication set not found and it's okay, continuing.");
          return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0, progress.getResourceModel());
        }
        return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
      } catch (Exception exception) {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
      }
    };
  }

  protected Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> updateReplicationSetDeletionProtection(
      AmazonWebServicesClientProxy proxy,
      ProxyClient<SsmIncidentsClient> proxyClient,
      String operationName,
      Logger logger
  ) {
    return progress -> proxy
        .initiate(
            "ssm-incidents::" + operationName + "ReplicationSet::UpdateDeletionProtection",
            proxyClient,
            progress.getResourceModel(),
            progress.getCallbackContext()
        )
        .translateToServiceRequest(Translator::translateToUpdateDeletionProtection)
        .makeServiceCall(callUpdateDeletionProtection(logger))
        .handleError((awsRequest, exception, client, model, context) -> {
          StringWriter sw = new StringWriter();
          exception.printStackTrace(new PrintWriter(sw));
          logger.log("updateReplicationSetDeletionProtection: exception occurred during UpdateDeletionProtection call: " + sw);
          if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
          }
          if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
          }
          return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        })
        .progress();
  }

  private BiFunction<UpdateDeletionProtectionRequest, ProxyClient<SsmIncidentsClient>, UpdateDeletionProtectionResponse> callUpdateDeletionProtection(Logger logger) {
    return (awsRequest, client) -> {
      if (awsRequest == null) {
        logger.log("callUpdateDeletionProtection: No deletion protection requested, skipping.");
        // deletionProtection is not set, do nothing
        return null;
      }
      UpdateDeletionProtectionResponse res = client.injectCredentialsAndInvokeV2(
          awsRequest,
          req -> client.client().updateDeletionProtection(awsRequest)
      );
      logger.log("callUpdateDeletionProtection: updateDeletionProtection call was successful");
      return res;
    };
  }

  private ResourceHandlerRequest<ResourceModel> redactRequest(ResourceHandlerRequest<ResourceModel> request) {
    return request.toBuilder()
        .desiredResourceState(redactModel(request.getDesiredResourceState()))
        .previousResourceState(redactModel(request.getPreviousResourceState()))
        .build();
  }

  private ResourceModel redactModel(ResourceModel model) {
    if ((model == null) || (model.getRegions() == null)) {
      return model;
    }
    boolean kmsKeysPresent = model.getRegions().stream()
        .anyMatch(replicationRegion ->
            Optional.ofNullable(replicationRegion)
                .map(ReplicationRegion::getRegionConfiguration)
                .map(RegionConfiguration::getSseKmsKeyId).isPresent()
        );
    if (!kmsKeysPresent) {
      // nothing to redact
      return model;
    }
    // replace all KMS Key values with a placeholder
    Set<ReplicationRegion> redactedRegions = model.getRegions().stream()
        .map(x ->
            new ReplicationRegion(
                x.getRegionName(),
                Optional.ofNullable(x.getRegionConfiguration())
                    .map(y ->
                        new RegionConfiguration(y.getSseKmsKeyId() != null ? KMS_KEY_REDACTED_PLACEHOLDER : null)
                    )
                    .orElse(null)))
        .collect(Collectors.toSet());
    return ResourceModel.builder()
        .arn(model.getArn())
        .deletionProtected(model.getDeletionProtected())
        .regions(ImmutableSet.copyOf(redactedRegions))
        .build();
  }
}
