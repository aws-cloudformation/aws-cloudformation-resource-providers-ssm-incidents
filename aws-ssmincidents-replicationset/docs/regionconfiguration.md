# AWS::SSMIncidents::ReplicationSet RegionConfiguration

The ReplicationSet regional configuration.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#ssekmskeyid" title="SseKmsKeyId">SseKmsKeyId</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#ssekmskeyid" title="SseKmsKeyId">SseKmsKeyId</a>: <i>String</i>
</pre>

## Properties

#### SseKmsKeyId

The ARN of the ReplicationSet.

_Required_: Yes

_Type_: String

_Maximum_: <code>1000</code>

_Pattern_: <code>^arn:aws(-(cn|us-gov|iso(-b)?))?:[a-z-]+:(([a-z]+-)+[0-9])?:([0-9]{12})?:[^.]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
