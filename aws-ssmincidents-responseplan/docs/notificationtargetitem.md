# AWS::SSMIncidents::ResponsePlan NotificationTargetItem

A notification target.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#snstopicarn" title="SnsTopicArn">SnsTopicArn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#snstopicarn" title="SnsTopicArn">SnsTopicArn</a>: <i>String</i>
</pre>

## Properties

#### SnsTopicArn

The ARN of the Chatbot SNS topic.

_Required_: No

_Type_: String

_Maximum_: <code>1000</code>

_Pattern_: <code>^arn:aws(-(cn|us-gov))?:sns:(([a-z]+-)+[0-9])?:([0-9]{12})?:[^.]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
