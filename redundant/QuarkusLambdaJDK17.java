package airhacks.lambda.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.LayerVersionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Version;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class QuarkusLambdaJDK17 extends Construct {

	static Map<String, String> configuration = Map.of("message", "hello, quarkus as AWS Lambda", "JAVA_TOOL_OPTIONS",
			"-XX:+TieredCompilation -XX:TieredStopAtLevel=1");
	static String lambdaHandler = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest";
	static int memory = 1024; // ~0.5 vCPU
	static int timeout = 10;
	IFunction function;

	public QuarkusLambdaJDK17(Construct scope, String functionName) {
		this(scope, functionName, true);
	}

	public QuarkusLambdaJDK17(Construct scope, String functionName, boolean snapStart) {
		super(scope, "QuarkusLambda");
		this.function = createFunction(functionName, lambdaHandler, configuration, memory, timeout, snapStart);
		if (snapStart) {
			var version = setupSnapStart(this.function);
			this.function = createAlias(version);
		}
	}

	Version setupSnapStart(IFunction function) {
		var defaultChild = this.function.getNode().getDefaultChild();
		if (defaultChild instanceof CfnFunction cfnFunction) {
			cfnFunction.addPropertyOverride("SnapStart", Map.of("ApplyOn", "PublishedVersions"));
		}
		// a fresh logicalId enfoces code redeployment
		var uniqueLogicalId = "SnapStartVersion" + System.currentTimeMillis();
		return Version.Builder.create(this, uniqueLogicalId).lambda(this.function).description("SnapStart").build();
	}

	Alias createAlias(Version version) {
		return Alias.Builder.create(this, "SnapstartAlias").aliasName("snapstart")
				.description("this alias is required for SnapStart").version(version).build();
	}

	IFunction createFunction(String functionName, String functionHandler, Map<String, String> configuration, int memory,
			int timeout, boolean snapStart) {
		var architecture = snapStart ? Architecture.X86_64 : Architecture.ARM_64;
		LayerVersion java17CDKlayer = new LayerVersion(this, "Java17CDKLayer",
				LayerVersionProps.builder().layerVersionName("Java17CDKLayer").description("Java 17 from CDK")
						.compatibleRuntimes(Arrays.asList(Runtime.PROVIDED_AL2))
						.code(Code.fromAsset("D:/java/java17layer.zip")).build());

//		final LayerVersion layer = new LayerVersion(this, "layer",
//				LayerVersionProps.builder().code(Code.fromAsset("../layer/target/bundle"))
//						.compatibleRuntimes(Arrays.asList(Runtime.JAVA_8)).build());
		return Function.Builder.create(this, functionName).runtime(Runtime.PROVIDED_AL2).architecture(architecture)
				.code(Code.fromAsset("../instrument-lambda/target/function.zip")).handler(functionHandler)
				.memorySize(memory).functionName(functionName).environment(configuration)
				.timeout(Duration.seconds(timeout)).logRetention(RetentionDays.ONE_WEEK)
				.layers(singletonList(java17CDKlayer)).build();
	}

	private List<LayerVersion> singletonList(LayerVersion layer) {
		// TODO Auto-generated method stub
		List<LayerVersion> list = new ArrayList<>();
		list.add(layer);
		return list;
	}

	public IFunction getFunction() {
		return this.function;
	}
}