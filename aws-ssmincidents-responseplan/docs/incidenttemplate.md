# AWS::SSMIncidents::ResponsePlan IncidentTemplate

The incident template configuration.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#dedupestring" title="DedupeString">DedupeString</a>" : <i>String</i>,
    "<a href="#impact" title="Impact">Impact</a>" : <i>Integer</i>,
    "<a href="#notificationtargets" title="NotificationTargets">NotificationTargets</a>" : <i>[ <a href="notificationtargetitem.md">NotificationTargetItem</a>, ... ]</i>,
    "<a href="#summary" title="Summary">Summary</a>" : <i>String</i>,
    "<a href="#title" title="Title">Title</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#dedupestring" title="DedupeString">DedupeString</a>: <i>String</i>
<a href="#impact" title="Impact">Impact</a>: <i>Integer</i>
<a href="#notificationtargets" title="NotificationTargets">NotificationTargets</a>: <i>
      - <a href="notificationtargetitem.md">NotificationTargetItem</a></i>
<a href="#summary" title="Summary">Summary</a>: <i>String</i>
<a href="#title" title="Title">Title</a>: <i>String</i>
</pre>

## Properties

#### DedupeString

The deduplication string.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>1000</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Impact

The impact value.

_Required_: Yes

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NotificationTargets

The list of notification targets.

_Required_: No

_Type_: List of <a href="notificationtargetitem.md">NotificationTargetItem</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Summary

The summary string.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>4000</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Title

The title string.

_Required_: Yes

_Type_: String

_Maximum_: <code>200</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
