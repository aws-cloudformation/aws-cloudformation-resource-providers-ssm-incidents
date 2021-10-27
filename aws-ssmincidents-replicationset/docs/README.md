# AWS::SSMIncidents::ReplicationSet

Resource type definition for AWS::SSMIncidents::ReplicationSet

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SSMIncidents::ReplicationSet",
    "Properties" : {
        "<a href="#regions" title="Regions">Regions</a>" : <i>[ <a href="replicationregion.md">ReplicationRegion</a>, ... ]</i>,
        "<a href="#deletionprotected" title="DeletionProtected">DeletionProtected</a>" : <i>Boolean</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SSMIncidents::ReplicationSet
Properties:
    <a href="#regions" title="Regions">Regions</a>: <i>
      - <a href="replicationregion.md">ReplicationRegion</a></i>
    <a href="#deletionprotected" title="DeletionProtected">DeletionProtected</a>: <i>Boolean</i>
</pre>

## Properties

#### Regions

_Required_: Yes

_Type_: List of <a href="replicationregion.md">ReplicationRegion</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DeletionProtected

Configures the ReplicationSet deletion protection.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The ARN of the ReplicationSet.
