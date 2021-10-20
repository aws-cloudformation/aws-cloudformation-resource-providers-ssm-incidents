package software.amazon.ssmincidents.responseplan;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ssmincidents.model.AccessDeniedException;
import software.amazon.awssdk.services.ssmincidents.model.ConflictException;
import software.amazon.awssdk.services.ssmincidents.model.CreateResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.DeleteResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanResponse;
import software.amazon.awssdk.services.ssmincidents.model.ListResponsePlansRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListResponsePlansResponse;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.ssmincidents.model.ResourceNotFoundException;
import software.amazon.awssdk.services.ssmincidents.model.ThrottlingException;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.ValidationException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.ssmincidents.responseplan.translators.TranslatorFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateResponsePlanRequest translateToCreateRequest(final ResourceModel model) {
    return TranslatorFactory.CREATE_RESPONSEPLAN_CONVERTER.reverse().convert(model);
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetResponsePlanRequest translateToReadRequest(final ResourceModel model) {
    return GetResponsePlanRequest.builder().arn(model.getArn()).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetResponsePlanResponse awsResponse) {
    return TranslatorFactory.GET_RESPONSEPLAN_CONVERTER.convert(awsResponse);
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteResponsePlanRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteResponsePlanRequest.builder().arn(model.getArn()).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateResponsePlanRequest translateToFirstUpdateRequest(final ResourceModel model) {
    return TranslatorFactory.UPDATE_RESPONSEPLAN_CONVERTER.reverse().convert(model);
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListResponsePlansRequest translateToListRequest(final String nextToken) {
    return ListResponsePlansRequest.builder().nextToken(nextToken).maxResults(50).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListResponsePlansResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(awsResponse.responsePlanSummaries())
        .map(x -> ResourceModel.builder()
            .arn(x.arn())
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  protected static BaseHandlerException handleException(Exception e)  {
    if (e instanceof ResourceNotFoundException) {
      return new CfnNotFoundException(e);
    } else if (e instanceof AccessDeniedException) {
      return new CfnAccessDeniedException(e);
    } else if (e instanceof ValidationException) {
      return new CfnInvalidRequestException(e);
    } else if (e instanceof ThrottlingException) {
      return new CfnThrottlingException(e);
    } else if (e instanceof ConflictException) {
      return new CfnAlreadyExistsException(e);
    } else if (e instanceof AwsServiceException) {
      return new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
    }
    return new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
  }

  public static ResourceModel mergeTags(GetResponsePlanResponse getResponsePlanResponse, ListTagsForResourceResponse listTagsForResourceResponse) {
    ResourceModel resourceModel = translateFromReadResponse(getResponsePlanResponse);
    Set<Tag> tags = TranslatorFactory.TAGS_CONVERTER.convert(listTagsForResourceResponse.tags());
    resourceModel.setTags(Optional.ofNullable(tags).orElse(new HashSet<>()));
    return resourceModel;
  }

  public static Map<String, String> toApiTag(Set<Tag> tags) {
    return TranslatorFactory.TAGS_CONVERTER.reverse().convert(tags);
  }
}
