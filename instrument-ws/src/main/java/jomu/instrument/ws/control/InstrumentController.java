package jomu.instrument.ws.control;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("/multiupload")
public class InstrumentController {

	private static String UPLOAD_DIR = "E:/sure-delete";

	@POST
	@Path("/files")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response handleFileUploadForm(@MultipartForm MultipartFormDataInput input) {

		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<String> fileNames = new ArrayList<>();

		List<InputPart> inputParts = uploadForm.get("file");
		System.out.println("inputParts size: " + inputParts.size());
		String fileName = null;
		for (InputPart inputPart : inputParts) {
			try {

				MultivaluedMap<String, String> header = inputPart.getHeaders();
				fileName = getFileName(header);
				fileNames.add(fileName);
				System.out.println("File Name: " + fileName);
				InputStream inputStream = inputPart.getBody(InputStream.class, null);
				byte[] bytes = IOUtils.toByteArray(inputStream);
				//
				File customDir = new File(UPLOAD_DIR);
				fileName = customDir.getAbsolutePath() + File.separator + fileName;
				Files.write(Paths.get(fileName), bytes, StandardOpenOption.CREATE_NEW);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String uploadedFileNames = String.join(", ", fileNames);
		return Response.ok().entity("All files " + uploadedFileNames + " successfully.").build();
	}

	private String getFileName(MultivaluedMap<String, String> header) {
		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
		for (String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {
				String[] name = filename.split("=");
				String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}
}