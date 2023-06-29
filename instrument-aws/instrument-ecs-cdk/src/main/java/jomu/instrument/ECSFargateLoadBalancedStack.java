package jomu.instrument;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ClusterProps;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;

public class ECSFargateLoadBalancedStack extends Stack {

	public ECSFargateLoadBalancedStack(final Construct parent, final String id) {
		this(parent, id, null);
	}

	public ECSFargateLoadBalancedStack(final Construct parent, final String id, final StackProps props) {
		super(parent, id, props);

		// Create VPC with a AZ limit of two.
		Vpc vpc = new Vpc(this, "InstrumentEcsVpc", VpcProps.builder().maxAzs(2).build());

		// Create the ECS Service
		Cluster cluster = new Cluster(this, "InstrumentEcsCluster", ClusterProps.builder().vpc(vpc).build());

		// Use the ECS Network Load Balanced Fargate Service construct to create a ECS
		// service
//		NetworkLoadBalancedFargateService fargateService = new NetworkLoadBalancedFargateService(this,
//				"InstrumentFargateService",
//				NetworkLoadBalancedFargateServiceProps.builder().cluster(cluster)
//						.taskImageOptions(NetworkLoadBalancedTaskImageOptions.builder()
//								.image(ContainerImage
//										.fromRegistry("942706091699.dkr.ecr.eu-west-2.amazonaws.com/instrument:latest"))
//								.build())
//						.build());

		// Create a load-balanced Fargate service and make it public
		ApplicationLoadBalancedFargateService fargateService = ApplicationLoadBalancedFargateService.Builder
				.create(this, "InstrumentFargateService").cluster(cluster) // Required
				.cpu(512) // Default is 256
				.desiredCount(2) // Default is 1
				.taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
						.image(ContainerImage
								.fromRegistry("942706091699.dkr.ecr.eu-west-2.amazonaws.com/instrument:latest"))
						.build())
				.memoryLimitMiB(2048) // Default is 512
				.publicLoadBalancer(true) // Default is true
				.build();

		// Open port 80 inbound to IPs within VPC to allow network load balancer to
		// connect to the service
		// fargateService.getService().getConnections().getSecurityGroups().get(0)
		// .addIngressRule(Peer.ipv4(vpc.getVpcCidrBlock()), Port.tcp(80), "allow http
		// inbound from vpc");
		fargateService.getService().getConnections().getSecurityGroups().get(0)
				.addIngressRule(Peer.ipv4(vpc.getVpcCidrBlock()), Port.tcp(9080), "allow http inbound from vpc");

	}
}
