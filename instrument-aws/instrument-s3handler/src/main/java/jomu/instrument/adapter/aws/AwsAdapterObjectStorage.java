package jomu.instrument.adapter.aws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import jomu.instrument.control.ParameterManager;
import jomu.instrument.store.ObjectStorage;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@ApplicationScoped
public class AwsAdapterObjectStorage implements ObjectStorage {

	private static final Logger LOG = Logger.getLogger(AwsAdapterObjectStorage.class.getName());

	private static final String INSTRUMENT_STORE = "INSTRUMENT_STORE";

	@Inject
	S3Client s3Client;

	@ConfigProperty(name = "instrument.bucket.name")
	String bucketName;

	@Inject
	ParameterManager parameterManager;

	private Map<String, String> environmentMap = new HashMap<String, String>();

	void onStart(@Observes StartupEvent ev) {
		initEnvironment();
	}

	@Override
	public Map<String, String> getMetaData(String name) {
		HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucketName).key(name).build();
		HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
		return headObjectResponse.metadata();
	}

	@Override
	public void write(String name, File file) {
		LOG.severe(">>AwsAdapterObjectStorage write: " + name + ", " + file.getAbsolutePath() + ", " + file.length());
		PutObjectRequest request = buildPutRequest(name.startsWith("/") ? name.substring(1) : name);
		byte[] bytes = new byte[(int) file.length()];
		try (FileInputStream fis = new FileInputStream(file)) {
			fis.read(bytes);
			RequestBody body = RequestBody.fromBytes(bytes);
			LOG.severe(">>AwsAdapterObjectStorage body len: " + body.optionalContentLength() + ", " + bytes.length);
			s3Client.putObject(request, body);
			LOG.log(Level.SEVERE, ">>AwsAdapterObjectStorage writen");
		} catch (IOException e) {
			LOG.log(Level.SEVERE, ">>AwsAdapterObjectStorage write IOException", e);
		} catch (Throwable t) {
			LOG.log(Level.SEVERE, ">>AwsAdapterObjectStorage write Exception", t);
		}
	}

	@Override
	public void writeString(String name, String contents) {
		PutObjectRequest request = buildPutRequest(name.startsWith("/") ? name.substring(1) : name);
		RequestBody body = RequestBody.fromString(contents);
		s3Client.putObject(request, body);
	}

	@Override
	public String readString(String name) {
		String contents = null;
		GetObjectRequest request = buildGetRequest(name);
		try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
			contents = new String(response.readAllBytes(), StandardCharsets.UTF_8);
		} catch (AwsServiceException | SdkClientException | IOException ex) {
			return null;
		}
		return contents;
	}

	@Override
	public InputStream read(String name) {
		GetObjectRequest request = buildGetRequest(name);
		try {
			return s3Client.getObject(request);
		} catch (NoSuchKeyException ex) {
			return null;
		}
	}

	private boolean initEnvironment() {
		if (System.getenv(INSTRUMENT_STORE) != null) {
			environmentMap.put(INSTRUMENT_STORE, System.getenv(INSTRUMENT_STORE));
			System.out.println("AwsAdapterObjectStorage initEnvironment INSTRUMENT_STORE: "
					+ environmentMap.get(INSTRUMENT_STORE));
		} else {
			System.out.println("AwsAdapterObjectStorage initEnvironment: missing DATA_PIPELINE_STORE");
			return false;
		}

		return true;
	}

	@Override
	public String getBasePath() {
		return "/tmp";
		// String basePath =
		// parameterManager.getParameter(InstrumentParameterNames.STORAGE_OBJECT_STORE_BASE_PATH);
		// if (basePath.equals("$user.home")) {
		// return System.getProperty("user.home");
		// } else {
		// return basePath;
		// }
	}

	protected PutObjectRequest buildPutRequest(String objectKey) {
		return PutObjectRequest.builder().bucket(bucketName).key(objectKey).build();
	}

	protected GetObjectRequest buildGetRequest(String objectKey) {
		return GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();
	}

	@Override
	public void clearStore(String name) {
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(bucketName).prefix(name).build();
		ListObjectsResponse objectListing = s3Client.listObjects(listObjectsRequest);
		for (S3Object objectSummary : objectListing.contents()) {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName)
					.key(objectSummary.key()).build();
			s3Client.deleteObject(deleteObjectRequest);
		}
	}

	@Override
	public String[] listStore(String name) {
		List<String> keys = new ArrayList<>();
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(bucketName).prefix(name).build();
		ListObjectsResponse objectListing = s3Client.listObjects(listObjectsRequest);
		for (S3Object objectSummary : objectListing.contents()) {
			keys.add(objectSummary.key());
		}
		return keys.toArray(new String[keys.size()]);
	}

	@Override
	public void delete(String name) {
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(name).build();
		s3Client.deleteObject(deleteObjectRequest);
	}

}
