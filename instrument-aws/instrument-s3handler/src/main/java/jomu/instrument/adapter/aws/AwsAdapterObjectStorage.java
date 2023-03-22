package jomu.instrument.adapter.aws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import jomu.instrument.store.ObjectStorage;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ApplicationScoped
public class AwsAdapterObjectStorage implements ObjectStorage {

	private static final Logger LOG = Logger.getLogger(AwsAdapterObjectStorage.class.getName());

	private static final String INSTRUMENT_STORE = "INSTRUMENT_STORE";
	private static final String WRITE_FILES_PATH = "output";

	@Inject
	S3Client s3Client;

	@ConfigProperty(name = "instrument.bucket.name")
	String bucketName;

	private Map<String, String> environmentMap = new HashMap<String, String>();

	void onStart(@Observes StartupEvent ev) {
		initEnvironment();
	}

	@Override
	public void write(String name, File file) {
		LOG.severe(">>AwsAdapterObjectStorage write: " + name + ", " + file.getAbsolutePath() + ", " + file.length());
		String writeKey = createWriteKeyName("test", name);
		PutObjectRequest request = buildPutRequest(name.startsWith("/") ? name.substring(1) : name);

		byte[] bytes = new byte[(int) file.length()];
		try (FileInputStream fis = new FileInputStream(file)) {
			fis.read(bytes);
			RequestBody body = RequestBody.fromBytes(bytes);
			LOG.severe(">>AwsAdapterObjectStorage body len: " + body.optionalContentLength() + ", " + bytes.length);
			s3Client.putObject(request, body);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "", e);
		}
	}

	@Override
	public InputStream read(String name) {
		String readKey = createReadKeyName(name);
		GetObjectRequest request = buildGetRequest(readKey);
		return s3Client.getObject(request);
	}

	private String createReadKeyName(String name) {
		return name;
	}

	private String createWriteKeyName(String stage, String name) {
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
//		String today = LocalDate.now().format(formatter);
//		return String.format("%s/%s/%s/%s-%s", WRITE_FILES_PATH, today, odsCode, stage, odsCode);
		return name;
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
	}

	protected PutObjectRequest buildPutRequest(String objectKey) {
		return PutObjectRequest.builder().bucket(bucketName).key(objectKey).build();
	}

	protected GetObjectRequest buildGetRequest(String objectKey) {
		return GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();
	}

}
