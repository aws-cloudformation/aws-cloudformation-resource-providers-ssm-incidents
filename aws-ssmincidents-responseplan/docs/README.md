# AWS::SSMIncidents::ResponsePlan

Resource type definition for AWS::SSMIncidents::ResponsePlan

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SSMIncidents::ResponsePlan",
    "Properties" : {
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#displayname" title="DisplayName">DisplayName</a>" : <i>String</i>,
        "<a href="#chatchannel" title="ChatChannel">ChatChannel</a>" : <i><a href="chatchannel.md">ChatChannel</a></i>,
        "<a href="#engagements" title="Engagements">Engagements</a>" : <i>[ String, ... ]</i>,
        "<a href="#actions" title="Actions">Actions</a>" : <i>[ <a href="action.md">Action</a>, ... ]</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#incidenttemplate" title="IncidentTemplate">IncidentTemplate</a>" : <i><a href="incidenttemplate.md">IncidentTemplate</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SSMIncidents::ResponsePlan
Properties:
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#displayname" title="DisplayName">DisplayName</a>: <i>String</i>
    <a href="#chatchannel" title="ChatChannel">ChatChannel</a>: <i><a href="chatchannel.md">ChatChannel</a></i>
    <a href="#engagements" title="Engagements">Engagements</a>: <i>
      - String</i>
    <a href="#actions" title="Actions">Actions</a>: <i>
      - <a href="action.md">Action</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#incidenttemplate" title="IncidentTemplate">IncidentTemplate</a>: <i><a href="incidenttemplate.md">IncidentTemplate</a></i>
</pre>

## Properties

#### Name

The name of the response plan.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>200</code>

_Pattern_: <code>^[a-zA-Z0-9_-]*$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### DisplayName

The display name of the response plan.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>200</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ChatChannel

The chat channel configuration.

_Required_: No

_Type_: <a href="chatchannel.md">ChatChannel</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Engagements

The list of engagements to use.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Actions

The list of actions.

_Required_: No

_Type_: List of <a href="action.md">Action</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

The tags to apply to the response plan.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### IncidentTemplate

The incident template configuration.

_Required_: Yes

_Type_: <a href="incidenttemplate.md">IncidentTemplate</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The ARN of the response plan.

