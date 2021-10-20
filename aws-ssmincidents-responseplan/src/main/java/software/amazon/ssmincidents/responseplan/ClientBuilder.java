package software.amazon.ssmincidents.responseplan;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.util.Optional;

public class ClientBuilder {
    public static SsmIncidentsClient getClient() {
        String region = getRegion();
        return SsmIncidentsClient.builder().region(Region.of(region))
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }

    private static String getRegion() {
        return Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-west-2");
    }
}
