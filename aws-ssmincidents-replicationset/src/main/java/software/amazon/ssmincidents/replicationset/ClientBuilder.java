package software.amazon.ssmincidents.replicationset;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssmincidents.SsmIncidentsClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

  private static final String REGION = System.getenv("AWS_REGION");

  public static SsmIncidentsClient getClient() {
    return SsmIncidentsClient.builder()
        .httpClient(LambdaWrapper.HTTP_CLIENT)
        .region(Region.of(REGION))
        .build();
  }
}
