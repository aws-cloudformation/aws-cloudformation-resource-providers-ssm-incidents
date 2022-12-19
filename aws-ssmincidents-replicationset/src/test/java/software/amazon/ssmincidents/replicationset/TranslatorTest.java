package software.amazon.ssmincidents.replicationset;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssmincidents.model.CreateReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.RegionInfo;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSet;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class TranslatorTest {

    public static final String TAG_KEY_1 = "tag_key_1";
    public static final String TAG_VALUE_1 = "tag_value_1";
    public static final String TAG_KEY_2 = "tag_key_2";
    public static final String TAG_VALUE_2 = "tag_value_2";
    public static final ImmutableMap<String, String> API_TAGS = ImmutableMap.of(TAG_KEY_1, TAG_VALUE_1, TAG_KEY_2, TAG_VALUE_2);
    public static final Set<Tag> TAGS = ImmutableSet.of(new Tag("tag_key_1", "tag_value_1"), new Tag("tag_key_2", "tag_value_2"));

    @Test
    public void testTranslateToCreateRequestTagOnCreateNoKmsKeys() {
        ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn")
            .regions(
                ImmutableSet.of(
                    ReplicationRegion.builder()
                        .regionName("us-west-2")
                        .build()
                )
            )
            .tags(TAGS)
            .build();
        CreateReplicationSetRequest apiRequest = Translator.translateToCreateRequest(
            resourceModel,
            "clientToken"
        );

        assertEquals("clientToken", apiRequest.clientToken());
        assertNotNull(apiRequest.regions().get("us-west-2"));
        assertEquals(API_TAGS, apiRequest.tags());
    }

    @Test
    public void testTranslateToUpdateRequestAddRegionNoKmsKeys() {
        ReplicationSet replicationSet = ReplicationSet.builder()
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().build()))
            .build();
        ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn")
            .regions(
                ImmutableSet.of(
                    ReplicationRegion.builder()
                        .regionName("us-east-1")
                        .build(),
                    ReplicationRegion.builder()
                        .regionName("us-west-2")
                        .build()
                )
            )
            .build();
        UpdateReplicationSetRequest apiRequest = Translator.translateToUpdateRequest(
            replicationSet,
            resourceModel,
            "clientToken"
        );

        assertEquals("clientToken", apiRequest.clientToken());
        assertEquals("arn", apiRequest.arn());
        assertNotNull(apiRequest.actions());
        assertEquals(1, apiRequest.actions().size());
        assertNotNull(apiRequest.actions().get(0));
        assertNotNull(apiRequest.actions().get(0).addRegionAction());
        assertNull(apiRequest.actions().get(0).deleteRegionAction());
        assertEquals("us-west-2", apiRequest.actions().get(0).addRegionAction().regionName());
        assertNull(apiRequest.actions().get(0).addRegionAction().sseKmsKeyId());
    }

    @Test
    public void testTranslateToUpdateRequestAddRegionWithKmsKeys() {
        ReplicationSet replicationSet = ReplicationSet.builder()
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().sseKmsKeyId("key-us-east-1").build()))
            .build();
        ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn")
            .regions(
                ImmutableSet.of(
                    ReplicationRegion.builder()
                        .regionName("us-east-1")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-east-1").build())
                        .build(),
                    ReplicationRegion.builder()
                        .regionName("us-west-2")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-west-2").build())
                        .build()
                )
            )
            .build();
        UpdateReplicationSetRequest apiRequest = Translator.translateToUpdateRequest(
            replicationSet,
            resourceModel,
            "clientToken"
        );

        assertEquals("clientToken", apiRequest.clientToken());
        assertEquals("arn", apiRequest.arn());
        assertNotNull(apiRequest.actions());
        assertEquals(1, apiRequest.actions().size());
        assertNotNull(apiRequest.actions().get(0));
        assertNotNull(apiRequest.actions().get(0).addRegionAction());
        assertNull(apiRequest.actions().get(0).deleteRegionAction());
        assertEquals("us-west-2", apiRequest.actions().get(0).addRegionAction().regionName());
        assertEquals("key-us-west-2", apiRequest.actions().get(0).addRegionAction().sseKmsKeyId());
    }

    @Test
    public void testTranslateToUpdateRequest_noDifference() {
        ReplicationSet replicationSet = ReplicationSet.builder()
            .regionMap(
                ImmutableMap.of(
                    "us-east-1", RegionInfo.builder().sseKmsKeyId("key-us-east-1").build(),
                    "us-west-2", RegionInfo.builder().sseKmsKeyId("key-us-west-2").build()
                )
            )
            .build();
        ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn")
            .regions(
                ImmutableSet.of(
                    ReplicationRegion.builder()
                        .regionName("us-east-1")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-east-1").build())
                        .build(),
                    ReplicationRegion.builder()
                        .regionName("us-west-2")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-west-2").build())
                        .build()
                )
            )
            .build();

        UpdateReplicationSetRequest apiRequest = Translator.translateToUpdateRequest(
            replicationSet,
            resourceModel,
            "clientToken"
        );

        assertNull(apiRequest);
    }

    @Test
    public void testTranslateToUpdateRequest_differByMoreThanOneRegion() {
        ReplicationSet replicationSet = ReplicationSet.builder()
            .regionMap(ImmutableMap.of("us-east-1", RegionInfo.builder().sseKmsKeyId("key-us-east-1").build()))
            .build();
        ResourceModel resourceModel = ResourceModel.builder()
            .arn("arn")
            .regions(
                ImmutableSet.of(
                    ReplicationRegion.builder()
                        .regionName("us-east-1")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-east-1").build())
                        .build(),
                    ReplicationRegion.builder()
                        .regionName("us-west-2")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-west-2").build())
                        .build(),
                    ReplicationRegion.builder()
                        .regionName("us-east-2")
                        .regionConfiguration(RegionConfiguration.builder().sseKmsKeyId("key-us-east-2").build())
                        .build()
                )
            )
            .build();

        try {
            Translator.translateToUpdateRequest(
                replicationSet,
                resourceModel,
                "clientToken"
            );
            fail("should have thrown an exception");
        } catch (CfnInvalidRequestException e) {
            // expected
        }
    }
}
