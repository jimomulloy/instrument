package jomu.instrument.lambda.control;

import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Version;
import software.constructs.Construct;

public class QuarkusLambda extends Construct {

	static Map<String, String> configuration = Map.of("message", "hello, quarkus as AWS Lambda", "JAVA_TOOL_OPTIONS",
			"-XX:+TieredCompilation -XX:TieredStopAtLevel=1");
	static String lambdaHandler = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest";
	static int memory = 10240;
	static int timeout = 300;
	IFunction function;

	public QuarkusLambda(Construct scope, String functionName) {
		this(scope, functionName, true);
	}

	public QuarkusLambda(Construct scope, String functionName, boolean snapStart) {
		super(scope, "QuarkusLambda");
		this.function = createFunction(functionName, lambdaHandler, configuration, memory, timeout, snapStart);
		//this.function = createNativeFunction(functionName, lambdaHandler, configuration, memory, timeout, snapStart);
		if (snapStart) {
			var version = setupSnapStart(this.function);
			this.function = createAlias(version);
		}
	}

	Version setupSnapStart(IFunction function) {
		var defaultChild = this.function.getNode().getDefaultChild();
		// JDK17 ?? if (defaultChild instanceof CfnFunction cfnFunction) {
		if (defaultChild instanceof CfnFunction) {
			CfnFunction cfnFunction = (CfnFunction) defaultChild;
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

		// Create a layer from the layer module
		// final LayerVersion layer = new LayerVersion(this, "InstrumentLayer",
		// LayerVersionProps.builder().code(Code.fromAsset("../instrument-layer/target/bundle"))
		// .compatibleRuntimes(Arrays.asList(Runtime.JAVA_11)).build());
		return Function.Builder.create(this, functionName).runtime(Runtime.JAVA_11).architecture(architecture)
				.code(Code.fromAsset("../instrument-s3handler/target/function.zip")).handler(functionHandler)
				.memorySize(memory).functionName(functionName).environment(configuration)
				.environment(Map.of("INSTRUMENT_STORE", "jomu-instrument-store")).timeout(Duration.seconds(timeout))
				.build();
	}
	
	IFunction createNativeFunction(String functionName, String functionHandler, Map<String, String> configuration, int memory,
			int timeout, boolean snapStart) {
		var architecture = Architecture.X86_64;

		// Create a layer from the layer module
		// final LayerVersion layer = new LayerVersion(this, "InstrumentLayer",
		// LayerVersionProps.builder().code(Code.fromAsset("../instrument-layer/target/bundle"))
		// .compatibleRuntimes(Arrays.asList(Runtime.JAVA_11)).build());
		return Function.Builder.create(this, functionName).runtime(Runtime.PROVIDED_AL2).architecture(architecture)
				.code(Code.fromAsset("../instrument-s3handler/target/function.zip")).handler(functionHandler)
				.memorySize(memory).functionName(functionName).environment(configuration)
				.environment(Map.of("INSTRUMENT_STORE", "jomu-instrument-store")).timeout(Duration.seconds(timeout))
				.build();
	}

	public IFunction getFunction() {
		return this.function;
	}
}