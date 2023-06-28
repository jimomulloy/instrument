package jomu.instrument.ws.control;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jomu.instrument.InstrumentException;
import jomu.instrument.ws.State;

@Path("/api")
public class InstrumentController {

	private static final Logger LOG = Logger.getLogger(InstrumentController.class.getName());

	@Inject
	EventBus bus;

	// @Inject
	// @Metric(name = "deposits", description = "Deposit histogram")
	// Histogram histogram;

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Bulkhead(value = 4, waitingTaskQueue = 10)
	@Timeout(value = 5000, unit = ChronoUnit.MILLIS)
	@CircuitBreaker(requestVolumeThreshold = 3, failureRatio = .66, delay = 1, delayUnit = ChronoUnit.SECONDS, successThreshold = 2)
	@Operation(summary = "Initiates processing of the given uploaded Audio file using the, (optional,) uploaded parameter properties file", description = "Client should make a POST call with Form data field containing an audio file and an optional a parameters file")
	@APIResponses({
			@APIResponse(name = "Upload Response", responseCode = "200", description = "the initial status od the process") })
	@ConcurrentGauge(name = "concurrentBlockingTransactions", absolute = true, description = "Number of concurrent transactions using blocking API")
	public State handleFileUploadForm(@MultipartForm MultipartFormDataInput input) {

		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<String> fileNames = new ArrayList<>();

		List<InputPart> inputParts = uploadForm.get("audio");
		if (inputParts == null) {
			throw new InstrumentException("InstrumentController handleFileUploadForm missing audio file part");
		}

		String fileName = null;
		String fileKey = null;
		String uploadId = UUID.randomUUID().toString();
		for (InputPart inputPart : inputParts) {
			try {

				MultivaluedMap<String, String> header = inputPart.getHeaders();
				fileKey = getFileKey(header);
				fileName = getFileName(header);
				fileNames.add(fileName);
				System.out.println("File Key: " + fileKey + ", name: " + fileName);
				InputStream inputStream = inputPart.getBody(InputStream.class, null);
				byte[] bytes = IOUtils.toByteArray(inputStream);
				java.nio.file.Path path = Paths
						.get(System.getProperty("user.home") + File.separator + ".instrumentuploads" + File.separator
								+ "input" + File.separator + uploadId + File.separator + "audio" + File.separator);
				Files.createDirectories(path);
				fileName = path + File.separator + fileName;
				Files.write(Paths.get(fileName), bytes, StandardOpenOption.CREATE_NEW);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "InstrumentController handleFileUploadForm error: " + e.getMessage(), e);
				throw new InstrumentException("InstrumentController handleFileUploadForm error: " + e.getMessage());
			}
		}

		inputParts = uploadForm.get("params");

		if (inputParts != null) {
			fileName = null;
			fileKey = null;

			for (InputPart inputPart : inputParts) {
				try {

					MultivaluedMap<String, String> header = inputPart.getHeaders();
					fileKey = getFileKey(header);
					fileName = getFileName(header);
					fileNames.add(fileName);
					System.out.println("File Key: " + fileKey + ", name: " + fileName);
					InputStream inputStream = inputPart.getBody(InputStream.class, null);
					byte[] bytes = IOUtils.toByteArray(inputStream);
					java.nio.file.Path path = Paths.get(
							System.getProperty("user.home") + File.separator + ".instrumentuploads" + File.separator
									+ "input" + File.separator + uploadId + File.separator + "params" + File.separator);
					Files.createDirectories(path);
					fileName = path + File.separator + fileName;
					Files.write(Paths.get(fileName), bytes, StandardOpenOption.CREATE_NEW);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "InstrumentController handleFileUploadForm error: " + e.getMessage(), e);
					throw new InstrumentException("InstrumentController handleFileUploadForm error: " + e.getMessage());
				}
			}
		}

		DeliveryOptions options = new DeliveryOptions();
		options.addHeader("send-time", Instant.now().toString()).setLocalOnly(true).setSendTimeout(60000);
		bus.publish("upload", uploadId, options);

		String uploadedFileNames = String.join(", ", fileNames);
		State state = new State();
		state.id = uploadId;
		state.message = "All files: " + uploadedFileNames + ", uploaded successfully.";
		return state;
	}

	@GET
	@Path("/download")
	@Cache(maxAge = 30)
	@Bulkhead(value = 4, waitingTaskQueue = 10)
	@Timeout(value = 5000, unit = ChronoUnit.MILLIS)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFileWithGet(@QueryParam("id") String id) {
		java.nio.file.Path inputPath = Paths.get(System.getProperty("user.home") + File.separator + ".instrumentuploads"
				+ File.separator + "input" + File.separator + id + File.separator + "audio" + File.separator);

		if (!Files.exists(inputPath)) {
			throw new InstrumentException("No instrument in process for id: " + id);
		}

		java.nio.file.Path resultOutputPath = Paths.get(System.getProperty("user.home") + File.separator
				+ ".instrumentuploads" + File.separator + "output" + File.separator + id + File.separator);

		if (!Files.exists(resultOutputPath)) {
			throw new InstrumentException("Instrument not completed processing for id: " + id);
		}

		StreamingOutput streamingOutput = outputStream -> {

			List<String> listFiles = new ArrayList<>();
			try {
				listFiles = Files.list(resultOutputPath).filter(Files::isRegularFile)
						.map(p -> p.getFileName().toString()).collect(Collectors.toList());
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "InstrumentController downloadFileWithGet error: " + e.getMessage(), e);
				throw new InstrumentException("InstrumentController downloadFileWithGet error: " + e.getMessage());
			}
			BufferedOutputStream baos = new BufferedOutputStream(outputStream);
			ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
			for (String fileName : listFiles) {
				// new zip entry and copying inputstream with file to zipOutputStream, after all
				// closing streams
				File file = new File(resultOutputPath + File.separator + fileName);
				zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
				FileInputStream fileInputStream = new FileInputStream(file);

				IOUtils.copy(fileInputStream, zipOutputStream);

				fileInputStream.close();
				zipOutputStream.closeEntry();
			}

			zipOutputStream.close();
			outputStream.flush();
			outputStream.close();
		};

		return Response.ok(streamingOutput).type("application/zip")
				.header("Content-Disposition", "attachment; filename=\"instrument.zip\"").build();
	}

	private String getFileName(MultivaluedMap<String, String> header) {
		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
		for (String filename : contentDisposition) {
			if (filename.trim().startsWith("filename")) {
				String[] name = filename.split("=");
				String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}

	private String getFileKey(MultivaluedMap<String, String> header) {
		for (Entry<String, List<String>> entry : header.entrySet()) {
			System.out.println(">>entry: " + entry.getKey() + ", " + entry.getValue());
		}
		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
		for (String entry : contentDisposition) {
			System.out.println(">>contentDisposition entry: " + entry);
			if (entry.trim().startsWith("name")) {
				String[] keyName = entry.split("=");
				String finalKeyName = keyName[1].trim().replaceAll("\"", "");
				System.out.println(">>finalKeyName: " + finalKeyName);
				return finalKeyName;
			}
		}
		return "unknown";
	}
}