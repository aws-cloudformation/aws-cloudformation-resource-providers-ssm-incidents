# AWS::SSMIncidents::ResponsePlan DynamicSsmParameter

A parameter with a dynamic value to set when starting the SSM automation document.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#key" title="Key">Key</a>" : <i>String</i>,
    "<a href="#value" title="Value">Value</a>" : <i><a href="dynamicssmparametervalue.md">DynamicSsmParameterValue</a></i>
}
</pre>

### YAML

<pre>
<a href="#key" title="Key">Key</a>: <i>String</i>
<a href="#value" title="Value">Value</a>: <i><a href="dynamicssmparametervalue.md">DynamicSsmParameterValue</a></i>
</pre>

## Properties

#### Key

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>50</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Value

Value of the dynamic parameter to set when starting the SSM automation document.

_Required_: Yes

_Type_: <a href="dynamicssmparametervalue.md">DynamicSsmParameterValue</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
