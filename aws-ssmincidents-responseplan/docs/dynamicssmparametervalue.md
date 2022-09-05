# AWS::SSMIncidents::ResponsePlan DynamicSsmParameterValue

Value of the dynamic parameter to set when starting the SSM automation document.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#variable" title="Variable">Variable</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#variable" title="Variable">Variable</a>: <i>String</i>
</pre>

## Properties

#### Variable

The variable types used as dynamic parameter value when starting the SSM automation document.

_Required_: No

_Type_: String

_Allowed Values_: <code>INCIDENT_RECORD_ARN</code> | <code>INVOLVED_RESOURCES</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
