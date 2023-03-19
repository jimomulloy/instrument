package jomu.instrument.lambda.control;

import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.constructs.Construct;

public interface AWSLambda {

	static Function createFunction(Construct thiz, String functionName, String functionHandler,
			Map<String, String> configuration, int memory, int timeout) {
		return Function.Builder.create(thiz, functionName).runtime(Runtime.JAVA_11).architecture(Architecture.ARM_64)
				.code(Code.fromAsset("../instrument-s3handler/target/function.zip")).handler(functionHandler)
				.memorySize(memory).functionName(functionName).environment(configuration)
				.timeout(Duration.seconds(timeout)).tracing(Tracing.ACTIVE).build();
	}

	static Function createFunction(Construct thiz, String functionName, String functionHandler) {
		return createFunction(thiz, functionName, functionHandler, Map.of(), 1024, 10);

	}

}
