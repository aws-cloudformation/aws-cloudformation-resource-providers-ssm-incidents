package software.amazon.ssmincidents.replicationset.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.io.IOException;

// AWS SDK V2 model objects are a bit peculiar in how they support
// json serialization using Jackson-Databind.
// See second part of the section about Immutable POJOs here:
// https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-0-developer-preview/
// This class introduces support for AWS SDK V2 serialization for requests and response objects
// This is currently used for logging purposes.
public class AwsObjectsSerializerFactory extends SerializerFactory {

    private static class AwsRequestSerializer extends JsonSerializer<Object> {

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            serializers.defaultSerializeValue(((AwsRequest) value).toBuilder(), gen);
        }
    }

    private static class AwsResponseSerializer extends JsonSerializer<Object> {

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            serializers.defaultSerializeValue(((AwsResponse) value).toBuilder(), gen);
        }
    }


    private final SerializerFactory delegateSerializerFactory;

    public AwsObjectsSerializerFactory(SerializerFactory delegateSerializerFactory) {
        this.delegateSerializerFactory = delegateSerializerFactory;
    }

    @Override
    public SerializerFactory withAdditionalSerializers(Serializers additional) {
        return delegateSerializerFactory.withAdditionalSerializers(additional);
    }

    @Override
    public SerializerFactory withAdditionalKeySerializers(Serializers additional) {
        return delegateSerializerFactory.withAdditionalKeySerializers(additional);
    }

    @Override
    public SerializerFactory withSerializerModifier(BeanSerializerModifier modifier) {
        return delegateSerializerFactory.withSerializerModifier(modifier);
    }

    @Override
    public JsonSerializer<Object> createSerializer(SerializerProvider prov, JavaType baseType) throws JsonMappingException {
        if (AwsRequest.class.isAssignableFrom(baseType.getRawClass())) {
            return new AwsRequestSerializer();
        }
        if (AwsResponse.class.isAssignableFrom(baseType.getRawClass())) {
            return new AwsResponseSerializer();
        }
        return delegateSerializerFactory.createSerializer(prov, baseType);
    }

    @Override
    public TypeSerializer createTypeSerializer(SerializationConfig config, JavaType baseType) throws JsonMappingException {
        return delegateSerializerFactory.createTypeSerializer(config, baseType);
    }

    @Override
    @Deprecated
    public JsonSerializer<Object> createKeySerializer(SerializationConfig config, JavaType type, JsonSerializer<Object> defaultImpl) throws JsonMappingException {
        return delegateSerializerFactory.createKeySerializer(config, type, defaultImpl);
    }
}
