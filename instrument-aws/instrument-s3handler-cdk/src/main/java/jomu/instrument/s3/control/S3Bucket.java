package jomu.instrument.s3.control;

import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

public class S3Bucket extends Construct {

	Bucket bucket;

	public S3Bucket(Construct scope) {
		super(scope, "Bucket");
		this.bucket = Bucket.Builder.create(this, "jomu-instrument-store").bucketName("jomu-instrument-store")
				.eventBridgeEnabled(true).build();
	}

	public Bucket getBucket() {
		return this.bucket;
	}

}
