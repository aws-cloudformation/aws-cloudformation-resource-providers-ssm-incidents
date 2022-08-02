package software.amazon.ssmincidents.responseplan;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.awssdk.services.ssmincidents.model.EmptyChatChannel;
import software.amazon.awssdk.services.ssmincidents.model.GetResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.ssmincidents.model.TagResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.UntagResourceRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateResponsePlanResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    public static final ResourceModel DESIRED_MODEL_BASE = ResourceModel.builder()
        .arn(TestData.ARN)
        .name(TestData.NAME)
        .incidentTemplate(
            IncidentTemplate.builder().title(TestData.TITLE).impact(TestData.IMPACT).build()
        )
        .build();
    public static final ResourceModel RETURNED_MODEL_BASE = ResourceModel.builder()
        .arn(TestData.ARN)
        .name(TestData.NAME)
        .incidentTemplate(
            IncidentTemplate.builder()
                .title(TestData.TITLE).impact(TestData.IMPACT)
                .build()
        )
        .engagements(new HashSet<>())
        .actions(new ArrayList<>())
        .tags(new HashSet<>())
        .build();
    public static final UpdateResponsePlanResponse UPDATE_RESPONSE_PLAN_RESPONSE = UpdateResponsePlanResponse
        .builder()
        .build();
    public static final ResourceModel PREVIOUS_MODEL_BASE = ResourceModel.builder()
        .arn(TestData.ARN)
        .name(TestData.NAME)
        .displayName(TestData.DISPLAY_NAME)
        .incidentTemplate(
            IncidentTemplate.builder()
                .title(TestData.TITLE)
                .impact(TestData.IMPACT)
                .build()
        )
        .tags(ImmutableSet.of(new Tag(TestData.TAG_KEY_3, TestData.TAG_VALUE_3)))
        .build();
    @Mock
    SsmIncidentsClient sdkClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<SsmIncidentsClient> proxyClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(SsmIncidentsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_CompleteToBaseSuccess() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = DESIRED_MODEL_BASE;
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .previousResourceState(ResourceModel.builder()
                .arn(TestData.ARN)
                .name(TestData.NAME)
                .incidentTemplate(
                    IncidentTemplate.builder().title(TestData.TITLE).impact(TestData.IMPACT).build()
                )
                .tags(TestData.TAGS_1)
                .build())
            .logicalResourceIdentifier(TestData.ARN)
            .build();

        when(proxyClient.client().updateResponsePlan(any(UpdateResponsePlanRequest.class)))
            .thenReturn(UPDATE_RESPONSE_PLAN_RESPONSE);

        when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
            .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_COMPLETE)
            .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_BASE);

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
            .thenReturn(ListTagsForResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
            .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(sdkClient, times(2)).getResponsePlan(any(GetResponsePlanRequest.class));
        verify(sdkClient, times(1)).updateResponsePlan(argThat((UpdateResponsePlanRequest x) -> assertUpdateResponsePlanRequestBase(x)));
        verify(sdkClient, times(1)).untagResource(argThat((UntagResourceRequest x) -> x.resourceArn().equals(TestData.ARN) && x.tagKeys().equals(ImmutableList.of(TestData.TAG_KEY_1, TestData.TAG_KEY_2))));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(RETURNED_MODEL_BASE);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_BaseToCompleteSuccess() {
        final UpdateHandler handler = new UpdateHandler();
        final ResourceModel model = TestData.MODEL_COMPLETE;
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .previousResourceState(PREVIOUS_MODEL_BASE)
            .logicalResourceIdentifier(TestData.ARN)
            .build();

        when(proxyClient.client().updateResponsePlan(any(UpdateResponsePlanRequest.class)))
            .thenReturn(UPDATE_RESPONSE_PLAN_RESPONSE);

        when(proxyClient.client().getResponsePlan(any(GetResponsePlanRequest.class)))
            .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_BASE)
            .thenReturn(TestData.GET_RESPONSE_PLAN_RESPONSE_COMPLETE);

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
            .thenReturn(ListTagsForResourceResponse.builder().tags(TestData.API_TAGS_1).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(sdkClient, times(2)).getResponsePlan(any(GetResponsePlanRequest.class));
        verify(sdkClient, times(1)).updateResponsePlan(argThat((UpdateResponsePlanRequest x) -> assertUpdateResponsePlanRequestComplete(x)));
        verify(sdkClient, times(1)).tagResource(argThat((TagResourceRequest x) -> x.resourceArn().equals(TestData.ARN) && x.tags().equals(TestData.API_TAGS_1)));
        verify(sdkClient, times(1)).untagResource(argThat((UntagResourceRequest x) -> x.resourceArn().equals(TestData.ARN) && x.tagKeys().equals(ImmutableList.of(TestData.TAG_KEY_3))));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).usingRecursiveComparison().isEqualTo(TestData.MODEL_COMPLETE);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    private boolean assertUpdateResponsePlanRequestComplete(UpdateResponsePlanRequest updateResponsePlanRequest) {
        assertThat(updateResponsePlanRequest.arn()).isEqualTo(TestData.ARN);
        assertThat(updateResponsePlanRequest.displayName()).isEqualTo(TestData.DISPLAY_NAME);
        assertThat(updateResponsePlanRequest.actions()).containsExactlyInAnyOrder(TestData.API_ACTION_1);
        assertThat(updateResponsePlanRequest.engagements()).containsExactlyInAnyOrder(TestData.CONTACT, TestData.ESCALATION);
        assertThat(updateResponsePlanRequest.incidentTemplateTitle()).isEqualTo(TestData.TITLE);
        assertThat(updateResponsePlanRequest.incidentTemplateImpact()).isEqualTo(TestData.IMPACT);
        assertThat(updateResponsePlanRequest.incidentTemplateSummary()).isEqualTo(TestData.SUMMARY);
        assertThat(updateResponsePlanRequest.incidentTemplateDedupeString()).isEqualTo(TestData.DEDUP);
        assertThat(updateResponsePlanRequest.incidentTemplateNotificationTargets()).containsExactlyInAnyOrder(TestData.API_NOTIFICATION_TARGET_ITEM_1, TestData.API_NOTIFICATION_TARGET_ITEM_2);
        assertThat(updateResponsePlanRequest.incidentTemplateTags()).isEqualTo(TestData.API_TAGS_1);
        assertThat(updateResponsePlanRequest.chatChannel()).isEqualTo(TestData.API_CHAT_CHANNEL);
        return true;
    }

    private boolean assertUpdateResponsePlanRequestBase(UpdateResponsePlanRequest updateResponsePlanRequest) {
        assertThat(updateResponsePlanRequest.arn()).isEqualTo(TestData.ARN);
        assertThat(updateResponsePlanRequest.displayName()).isNull();
        assertThat(updateResponsePlanRequest.actions()).isEmpty();
        assertThat(updateResponsePlanRequest.engagements()).isEmpty();
        assertThat(updateResponsePlanRequest.incidentTemplateTitle()).isEqualTo(TestData.TITLE);
        assertThat(updateResponsePlanRequest.incidentTemplateImpact()).isEqualTo(TestData.IMPACT);
        assertThat(updateResponsePlanRequest.incidentTemplateSummary()).isEmpty();
        assertThat(updateResponsePlanRequest.incidentTemplateDedupeString()).isEmpty();
        assertThat(updateResponsePlanRequest.incidentTemplateNotificationTargets()).isEmpty();
        assertThat(updateResponsePlanRequest.incidentTemplateTags()).isEmpty();
        assertThat(updateResponsePlanRequest.chatChannel().empty()).isEqualTo(EmptyChatChannel.builder().build());
        return true;
    }

}
