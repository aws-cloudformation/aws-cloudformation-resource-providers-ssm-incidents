package software.amazon.ssmincidents.responseplan.translators;

import com.google.common.base.Converter;
import software.amazon.ssmincidents.responseplan.Action;
import software.amazon.ssmincidents.responseplan.SsmAutomation;
import software.amazon.ssmincidents.responseplan.SsmParameter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionConverter extends
    Converter<software.amazon.awssdk.services.ssmincidents.model.Action, Action> {

    Converter<Map<String, List<String>>, Set<SsmParameter>> ssmParameterConverter;

    public ActionConverter(
        Converter<Map<String, List<String>>, Set<SsmParameter>> ssmParameterConverter) {
        this.ssmParameterConverter = ssmParameterConverter;
    }

    @Override
    protected Action doForward(software.amazon.awssdk.services.ssmincidents.model.Action action) {
        software.amazon.awssdk.services.ssmincidents.model.SsmAutomation ssmAutomation = action.ssmAutomation();
        if (ssmAutomation == null) {
            return new Action();
        }
        return Action.builder().ssmAutomation(
            SsmAutomation.builder()
                .documentName(ssmAutomation.documentName())
                .documentVersion(ssmAutomation.documentVersion())
                .roleArn(ssmAutomation.roleArn())
                .targetAccount(ssmAutomation.targetAccountAsString())
                .parameters(ssmParameterConverter.convert(ssmAutomation.parameters()))
                .build()
        ).build();
    }

    @Override
    protected software.amazon.awssdk.services.ssmincidents.model.Action doBackward(Action action) {
        SsmAutomation ssmAutomation = action.getSsmAutomation();
        if (ssmAutomation == null) {
            return software.amazon.awssdk.services.ssmincidents.model.Action.builder().build();
        }
        return software.amazon.awssdk.services.ssmincidents.model.Action.builder().ssmAutomation(
            software.amazon.awssdk.services.ssmincidents.model.SsmAutomation.builder()
                .roleArn(ssmAutomation.getRoleArn())
                .documentName(ssmAutomation.getDocumentName())
                .documentVersion(ssmAutomation.getDocumentVersion())
                .targetAccount(ssmAutomation.getTargetAccount())
                .parameters(ssmParameterConverter.reverse().convert(ssmAutomation.getParameters()))
                .build()
        ).build();
    }
}
