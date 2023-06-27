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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;
import jomu.instrument.ws.State;

@Path("/instrument")
public class InstrumentController {

	@Inject
	EventBus bus;

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public State handleFileUploadForm(@MultipartForm MultipartFormDataInput input) {

		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<String> fileNames = new ArrayList<>();

		List<InputPart> inputParts = uploadForm.get("audio");
		System.out.println("inputParts size: " + inputParts.size());
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
				e.printStackTrace();
			}
		}

		inputParts = uploadForm.get("params");
		System.out.println("params inputParts size: " + inputParts.size());
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
				java.nio.file.Path path = Paths
						.get(System.getProperty("user.home") + File.separator + ".instrumentuploads" + File.separator
								+ "input" + File.separator + uploadId + File.separator + "params" + File.separator);
				Files.createDirectories(path);
				fileName = path + File.separator + fileName;
				Files.write(Paths.get(fileName), bytes, StandardOpenOption.CREATE_NEW);
			} catch (Exception e) {
				e.printStackTrace();
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
		// return Response.ok().entity("All files " + uploadedFileNames + "
		// successfully.").build();
	}

	@POST
	@Path("/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes("application/x-www-form-urlencoded")
	public Response downloadFileWithPost(@FormParam("file") String uploadId) {
		java.nio.file.Path path = Paths.get(System.getProperty("user.home") + File.separator + ".instrumentuploads"
				+ File.separator + "input" + File.separator + uploadId + File.separator + "params" + File.separator);
		File fileDownload = new File(path + File.separator + uploadId);
		ResponseBuilder response = Response.ok((Object) fileDownload);
		response.header("Content-Disposition", "attachment;filename=" + uploadId);
		return response.build();
	}

	@GET
	@Path("/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFileWithGet(@QueryParam("id") String uploadId) {

		StreamingOutput streamingOutput = outputStream -> {

			java.nio.file.Path resultOutputPath = Paths.get(System.getProperty("user.home") + File.separator
					+ ".instrumentuploads" + File.separator + "output" + File.separator + uploadId + File.separator);

			List<String> listFiles = new ArrayList<>();
			try {
				listFiles = Files.list(resultOutputPath).filter(Files::isRegularFile)
						.map(p -> p.getFileName().toString()).collect(Collectors.toList());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw e;
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