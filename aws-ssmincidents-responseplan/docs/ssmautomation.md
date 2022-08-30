# AWS::SSMIncidents::ResponsePlan SsmAutomation

The configuration to use when starting the SSM automation document.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#rolearn" title="RoleArn">RoleArn</a>" : <i>String</i>,
    "<a href="#documentname" title="DocumentName">DocumentName</a>" : <i>String</i>,
    "<a href="#documentversion" title="DocumentVersion">DocumentVersion</a>" : <i>String</i>,
    "<a href="#targetaccount" title="TargetAccount">TargetAccount</a>" : <i>String</i>,
    "<a href="#parameters" title="Parameters">Parameters</a>" : <i>[ <a href="ssmparameter.md">SsmParameter</a>, ... ]</i>,
    "<a href="#dynamicparameters" title="DynamicParameters">DynamicParameters</a>" : <i>[ <a href="dynamicssmparameter.md">DynamicSsmParameter</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#rolearn" title="RoleArn">RoleArn</a>: <i>String</i>
<a href="#documentname" title="DocumentName">DocumentName</a>: <i>String</i>
<a href="#documentversion" title="DocumentVersion">DocumentVersion</a>: <i>String</i>
<a href="#targetaccount" title="TargetAccount">TargetAccount</a>: <i>String</i>
<a href="#parameters" title="Parameters">Parameters</a>: <i>
      - <a href="ssmparameter.md">SsmParameter</a></i>
<a href="#dynamicparameters" title="DynamicParameters">DynamicParameters</a>: <i>
      - <a href="dynamicssmparameter.md">DynamicSsmParameter</a></i>
</pre>

## Properties

#### RoleArn

The role ARN to use when starting the SSM automation document.

_Required_: Yes

_Type_: String

_Maximum_: <code>1000</code>

_Pattern_: <code>^arn:aws(-(cn|us-gov))?:[a-z-]+:(([a-z]+-)+[0-9])?:([0-9]{12})?:[^.]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DocumentName

The document name to use when starting the SSM automation document.

_Required_: Yes

_Type_: String

_Maximum_: <code>128</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DocumentVersion

The version of the document to use when starting the SSM automation document.

_Required_: No

_Type_: String

_Maximum_: <code>128</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TargetAccount

The account type to use when starting the SSM automation document.

_Required_: No

_Type_: String

_Allowed Values_: <code>IMPACTED_ACCOUNT</code> | <code>RESPONSE_PLAN_OWNER_ACCOUNT</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Parameters

The parameters to set when starting the SSM automation document.

_Required_: No

_Type_: List of <a href="ssmparameter.md">SsmParameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DynamicParameters

The parameters with dynamic values to set when starting the SSM automation document.

_Required_: No

_Type_: List of <a href="dynamicssmparameter.md">DynamicSsmParameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
