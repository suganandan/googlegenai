package com.suga.googlegenai.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.EndpointName;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.PredictResponse;
import com.google.cloud.vertexai.api.PredictionServiceClient;
import com.google.cloud.vertexai.api.PredictionServiceSettings;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.gson.Gson;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.suga.googlegenai.dto.ErrorResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "API for Summary, MOM and Mind Map Generation", description = "Generating summary, Minutes of Meeting and mind map xml  from Meeting Transcript")
@RestController
@RequestMapping(path = "/api")
@Validated
public class SummaryGenController implements ErrorController{
	@org.springframework.beans.factory.annotation.Value("${genai.projectId}")
	private String projectId;
	@org.springframework.beans.factory.annotation.Value("${genai.location}")
	private String location;
	@org.springframework.beans.factory.annotation.Value("${genai.modelname}")
	private String modelName;
	@org.springframework.beans.factory.annotation.Value("${genai.publisher}")
	private String publisher;
	@org.springframework.beans.factory.annotation.Value("${genai.latestmodel}")
	private String latestmodel;

	@org.springframework.beans.factory.annotation.Value("${sum.temparature}")
	private String temperature;
	@org.springframework.beans.factory.annotation.Value("${sum.tokens}")
	private String tokens;
	@org.springframework.beans.factory.annotation.Value("${sum.topP}")
	private String topp;
	@org.springframework.beans.factory.annotation.Value("${sum.topK}")
	private String topk;

	private String instance;
	
	 private static final String PATH = "/error";

	    @RequestMapping(value = PATH)
	    public void error(HttpServletResponse response) throws IOException {
	         response.sendRedirect("/");   
	    }

	    public String getErrorPath() {
	        return PATH;
	    }
	

	@GetMapping("/searchai")
	public ModelAndView getSearchAI() {
		return new ModelAndView("chatsummaryprompt");
	}

	@Operation(summary = "Summary Geneartion API", description = "REST API to populate summary for the uploaded transcript")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
			@ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))) })

	@PostMapping("/summaryprompt")
	public ResponseEntity<String> getSummaryPrompt(@RequestParam("files") MultipartFile[] uploadfiles,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuilder responseData = new StringBuilder();
		StringBuilder tempVal = new StringBuilder();
		MultipartFile[] mFiles = uploadfiles;
		for (MultipartFile file : mFiles) {
			if (!file.isEmpty()) {
				try {

					ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
					String completeData = IOUtils.toString(stream, "UTF-8");
					List<String> linesOfMaxSize = new ArrayList<>();
					int i = 0;
					int maxSize = 4000;

					while (i < completeData.length()) {
						linesOfMaxSize.add(completeData.substring(i, Math.min(i + maxSize, completeData.length())));
						i += maxSize;
					}
					System.out.println("Size :" + linesOfMaxSize.size());

					String resOutput = null;
					try (VertexAI vertexAI = new VertexAI(projectId, location)) {
						for (String temVal : linesOfMaxSize) {
							String prompt = "please Provide a short summary for the following article \n " + temVal;

							GenerativeModel model = new GenerativeModel(modelName, vertexAI);

							GenerateContentResponse response01 = model.generateContent(prompt);
							List<Candidate> cand = response01.getCandidatesList();
							for (Candidate temp : cand) {

								Gson gson = new Gson();
								String json = gson.toJson(temp);
								JSONObject json1 = new JSONObject(json);

								JSONArray jsontemp1 = (JSONArray) json1.getJSONObject("content_").get("parts_");
								for (int i0 = 0; i0 < jsontemp1.length(); i0++) {
									JSONObject jsontemp2 = jsontemp1.getJSONObject(i0);

									resOutput = jsontemp2.get("data_").toString();
									tempVal.append(resOutput);
								}
							}
						}

					}

					Files.deleteIfExists(Paths.get("summary.txt"));

					try (PrintWriter out = new PrintWriter("summary.txt")) {
						out.println(tempVal);

					}

					responseData.append(
							"Summary for the transcript  <a href=./summarydownload> click here to download</a>");
					responseData.append(System.getProperty("line.separator"));
					System.out.println("Summary Generated for the Transcript");

				} catch (Exception exception) {
					responseData.append(exception.toString());

				}
			}
		}
		responseData.append(System.getProperty("line.separator"));
		getMom(responseData, tempVal);
		responseData.append(System.getProperty("line.separator"));
		getMindMap(responseData, tempVal);

		System.out.println("Minutes of Meeting Generated for the Transcript");

		return ResponseEntity.status(HttpStatus.OK).body(responseData.toString());
	}

	public void getMom(StringBuilder responseData, StringBuilder summaryString) throws IOException {
		try (VertexAI vertexAI = new VertexAI(projectId, location)) {
			String prompt = "Generate minutes of meeting with Date,Time, Meeting Particpants, Meeting Agenda summary and Action Points for the below meeting transcript.\n "
					+ summaryString;

			GenerativeModel model = new GenerativeModel(modelName, vertexAI);

			GenerateContentResponse response02 = model.generateContent(prompt);
			List<Candidate> cand = response02.getCandidatesList();
			for (Candidate temp : cand) {

				Gson gson = new Gson();
				String json = gson.toJson(temp);
				JSONObject json1 = new JSONObject(json);

				JSONArray jsontemp1 = (JSONArray) json1.getJSONObject("content_").get("parts_");
				for (int i = 0; i < jsontemp1.length(); i++) {
					JSONObject jsontemp2 = jsontemp1.getJSONObject(i);

					String tempResp = jsontemp2.get("data_").toString();
					Files.deleteIfExists(Paths.get("minutesofmeeting.txt"));

					try (PrintWriter out = new PrintWriter("minutesofmeeting.txt")) {
						out.println(tempResp);
					}
				}
			}
		}
		responseData.append("Minutes of Meeting  <a href=/api/momdownload> click here to download</a> ");

		responseData.append(System.getProperty("line.separator"));
	}

	public void getMindMap(StringBuilder responseData, StringBuilder summaryString) throws IOException {

		StringBuilder sbTemp = new StringBuilder();
		try (VertexAI vertexAI = new VertexAI(projectId, location)) {

			String prompt = "please Provide summary for the following article \n " + summaryString;

			GenerativeModel model = new GenerativeModel(modelName, vertexAI);

			GenerateContentResponse response01 = model.generateContent(prompt);
			List<Candidate> cand = response01.getCandidatesList();
			for (Candidate temp : cand) {

				Gson gson = new Gson();
				String json = gson.toJson(temp);
				JSONObject json1 = new JSONObject(json);

				JSONArray jsontemp1 = (JSONArray) json1.getJSONObject("content_").get("parts_");
				for (int i0 = 0; i0 < jsontemp1.length(); i0++) {
					JSONObject jsontemp2 = jsontemp1.getJSONObject(i0);

					String resOutput = jsontemp2.get("data_").toString();
					sbTemp.append(resOutput);
				}
			}

		}

		setInstance("{\r\n" + "            \"content\":  \"input: Following are the Agile Manifesto principles\r\n"
				+ "Individuals and interactions In Agile development, self-organization and motivation are important, as are interactions like co-location and pair programming.\r\n"
				+ "Working software Demo working software is considered the best means of communication with the customers to understand their requirements, instead of just depending on documentation.\r\n"
				+ "\r\n" + "output: " + "   <node TEXT=\\\"Agile Manifesto principles\\\">\r\n"
				+ "   <node TEXT=\\\"Individuals and interactions\\\">\r\n"
				+ "      <node TEXT=\\\"Self-organization and motivation\\\"\\/>\r\n"
				+ "      <node TEXT=\\\"Interactions like co-location and pair programming\\\"\\/>\r\n"
				+ "    <\\/node>\r\n" + "   <node TEXT=\\\"Working software\\\">\r\n"
				+ "      <node TEXT=\\\"Demo working software\\\"\\/>\r\n"
				+ "      <node TEXT=\\\"Communication with the customers\\\"\\/>\r\n"
				+ "      <node TEXT=\\\"Understanding their requirements\\\"\\/>\r\n" + "    <\\/node>\r\n"
				+ "   <\\/node>\r\n" + "input: " + sbTemp + "\r\n" + "output:\r\n" + "\"\r\n" + "        }");
		String parameters1 = "{\n" + "  \"temperature\":" + temperature + ",\n" + "  \"maxOutputTokens\": " + tokens
				+ ",\n" + "  \"topP\": " + topp + ",\n" + "  \"topK\": " + topk + "\n" + "}";
		String tempResp1 = predictTextSummarization(getInstance(), parameters1, projectId, location, publisher,
				latestmodel);

		StringBuilder sb = new StringBuilder("<map version=\"1.0.1\">\r\n");
		sb.append(" <node TEXT=\"Mind Map\">\r\n");
		sb.append(tempResp1);
		sb.append(" </node>\r\n");
		sb.append("</map>\r\n");
		Files.deleteIfExists(Paths.get("mindmapxml.mm"));

		try (PrintWriter out = new PrintWriter("mindmapxml.mm")) {
			out.println(sb.toString());

		}
		sb.setLength(0);
		sbTemp.setLength(0);
		responseData.append("mindmap  is created <a href=/api/mindmapdownload> click here to download</a> ");
		System.out.println("mindmap Generated for the Transcript");

	}

	public String predictTextSummarization(String instance, String parameters, String project, String location,
			String publisher, String model) throws IOException {
		String response = null;

		String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
		PredictionServiceSettings predictionServiceSettings = PredictionServiceSettings.newBuilder()
				.setEndpoint(endpoint).build();

		try (PredictionServiceClient predictionServiceClient = PredictionServiceClient
				.create(predictionServiceSettings)) {
			final EndpointName endpointName = EndpointName.ofProjectLocationPublisherModelName(project, location,
					publisher, model);

			Value.Builder instanceValue = Value.newBuilder();

			JsonFormat.parser().merge(instance, instanceValue);

			List<Value> instances = new ArrayList<>();
			instances.add(instanceValue.build());

			Value.Builder parameterValueBuilder = Value.newBuilder();
			JsonFormat.parser().merge(parameters, parameterValueBuilder);
			Value parameterValue = parameterValueBuilder.build();

			PredictResponse predictResponse = predictionServiceClient.predict(endpointName, instances, parameterValue);
			for (Value prediction : predictResponse.getPredictionsList()) {
				Map<String, Value> tempMap = prediction.getStructValue().getFieldsMap();
				Set<String> temKeySet = tempMap.keySet();
				for (String keyVal : temKeySet) {
					if (keyVal.equals("content")) {
						Gson gson = new Gson();
						String json = gson.toJson(tempMap.get(keyVal));
						JSONObject json1 = new JSONObject(json);
						response = json1.get("kind_").toString();
					}
				}
			}
		}

		return response;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}
}
