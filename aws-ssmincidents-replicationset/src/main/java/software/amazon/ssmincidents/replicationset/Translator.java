package software.amazon.ssmincidents.replicationset;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import software.amazon.awssdk.services.ssmincidents.model.AddRegionAction;
import software.amazon.awssdk.services.ssmincidents.model.CreateReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.DeleteRegionAction;
import software.amazon.awssdk.services.ssmincidents.model.DeleteReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.GetReplicationSetRequest;
import software.amazon.awssdk.services.ssmincidents.model.RegionMapInputValue;
import software.amazon.awssdk.services.ssmincidents.model.ReplicationSet;
import software.amazon.awssdk.services.ssmincidents.model.UpdateDeletionProtectionRequest;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetAction;
import software.amazon.awssdk.services.ssmincidents.model.UpdateReplicationSetRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    static CreateReplicationSetRequest translateToCreateRequest(ResourceModel model, String clientToken) {
        Map<String, RegionMapInputValue> regions;
        if (model.getRegions() != null) {
            ImmutableMap.Builder<String, RegionMapInputValue> mapBuilder = ImmutableMap.builder();
            model.getRegions().forEach(
                reg -> mapBuilder.put(
                    reg.getRegionName(),
                    RegionMapInputValue.builder()
                        .sseKmsKeyId(
                            Optional.ofNullable(reg.getRegionConfiguration())
                                .map(RegionConfiguration::getSseKmsKeyId)
                                .orElse(null)
                        )
                        .build()
                )
            );
            regions = mapBuilder.build();
        } else {
            regions = ImmutableMap.of();
        }

        return CreateReplicationSetRequest.builder()
            .regions(regions)
            .clientToken(clientToken)
            .build();
    }

    static GetReplicationSetRequest translateToReadRequest(ResourceModel model) {
        return GetReplicationSetRequest.builder()
            .arn(model.getArn())
            .build();
    }

    static DeleteReplicationSetRequest translateToDeleteRequest(ResourceModel model) {
        return DeleteReplicationSetRequest.builder()
            .arn(model.getArn())
            .build();
    }

    static UpdateReplicationSetRequest translateToUpdateRequest(
        ReplicationSet currentReplicationSet,
        ResourceModel desiredModelState,
        String clientToken) {
        Set<String> currentRegions = currentReplicationSet.regionMap().keySet();
        Set<String> desiredRegions = desiredModelState.getRegions().stream()
            .map(ReplicationRegion::getRegionName)
            .collect(Collectors.toSet());
        if (currentRegions.equals(desiredRegions)) {
            // nothing to update
            return null;
        }
        Set<String> regionsToAdd = Sets.difference(desiredRegions, currentRegions);
        Set<String> regionsToDelete = Sets.difference(currentRegions, desiredRegions);
        boolean differsByOneRegion = regionsToAdd.size() + regionsToDelete.size() == 1;
        if (!differsByOneRegion) {
            throw new CfnInvalidRequestException("Replication set regions differ by more then one region. " +
                "Current replication set regions: " + currentRegions.toString() + ". " +
                "Specified replication set regions: " + desiredRegions.toString());
        }
        if (regionsToAdd.size() > 0) {
            String regionToAdd = regionsToAdd.iterator().next();
            String optionalKmsKeyId = desiredModelState.getRegions()
                .stream().filter(x -> x.getRegionName().equals(regionToAdd))
                .findFirst()
                .map(ReplicationRegion::getRegionConfiguration)
                .map(RegionConfiguration::getSseKmsKeyId)
                .orElse(null);
            return UpdateReplicationSetRequest.builder()
                .arn(desiredModelState.getArn())
                .clientToken(clientToken)
                .actions(UpdateReplicationSetAction.builder()
                    .addRegionAction(AddRegionAction.builder()
                        .regionName(regionToAdd)
                        .sseKmsKeyId(optionalKmsKeyId)
                        .build())
                    .build())
                .build();
        }
        String regionToDelete = regionsToDelete.iterator().next();
        return UpdateReplicationSetRequest.builder()
            .arn(desiredModelState.getArn())
            .clientToken(clientToken)
            .actions(UpdateReplicationSetAction.builder()
                .deleteRegionAction(DeleteRegionAction.builder()
                    .regionName(regionToDelete)
                    .build())
                .build())
            .build();
    }

    static UpdateDeletionProtectionRequest translateToUpdateDeletionProtection(ResourceModel resourceModel) {
        if (resourceModel.getDeletionProtected() != null) {
            return UpdateDeletionProtectionRequest.builder()
                .arn(resourceModel.getArn())
                .deletionProtected(resourceModel.getDeletionProtected())
                .build();
        }
        return null;
    }
}
