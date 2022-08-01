# AWS::SSMIncidents::ReplicationSet ReplicationRegion

The ReplicationSet regional configuration.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#regionname" title="RegionName">RegionName</a>" : <i>String</i>,
    "<a href="#regionconfiguration" title="RegionConfiguration">RegionConfiguration</a>" : <i><a href="regionconfiguration.md">RegionConfiguration</a></i>
}
</pre>

### YAML

<pre>
<a href="#regionname" title="RegionName">RegionName</a>: <i>String</i>
<a href="#regionconfiguration" title="RegionConfiguration">RegionConfiguration</a>: <i><a href="regionconfiguration.md">RegionConfiguration</a></i>
</pre>

## Properties

#### RegionName

The AWS region name.

_Required_: No

_Type_: String

_Maximum_: <code>20</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RegionConfiguration

The ReplicationSet regional configuration.

_Required_: No

_Type_: <a href="regionconfiguration.md">RegionConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
