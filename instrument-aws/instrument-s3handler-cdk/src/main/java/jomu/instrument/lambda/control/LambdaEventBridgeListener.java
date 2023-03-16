package jomu.instrument.lambda.control;

import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;

public class LambdaEventBridgeListener extends Construct {

    static Map<String, String> configuration = Map.of("message", "hello,duke");
    static int memory = 512;
    static int timeout = 10;
    Function function;

    public LambdaEventBridgeListener(Construct scope, String lambdaHandler,String functionName) {
        super(scope, "LambdaEventBridgeListener");
        
        this.function = AWSLambda.createFunction(this,functionName, lambdaHandler, configuration, memory, timeout);
        
        CfnOutput.Builder.create(this, "FunctionARN").value(function.getFunctionArn()).build();
    }
    



    public Function getFunction(){
        return this.function;
    }
    
}
