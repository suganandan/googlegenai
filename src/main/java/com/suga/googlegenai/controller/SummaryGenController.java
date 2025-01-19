package com.suga.googlegenai.controller;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "API for Summary, MOM and Mind Map Generation", description = "Generating summary, Minutes of Meeting and mind map xml  from Meeting Transcript")
@RestController
@RequestMapping(path = "/api")
@Validated
@Slf4j
public class SummaryGenController implements ErrorController {
    private static final String CONTENT = "content_";
    private static final String PARTS = "parts_";
    private static final String DATA = "data_";
    @org.springframework.beans.factory.annotation.Value("${genAi.projectId}")
    private String projectId;
    @org.springframework.beans.factory.annotation.Value("${genAi.location}")
    private String location;
    @org.springframework.beans.factory.annotation.Value("${genAi.modelName}")
    private String modelName;
    @org.springframework.beans.factory.annotation.Value("${genAi.publisher}")
    private String publisher;
    @org.springframework.beans.factory.annotation.Value("${genAi.latestModel}")
    private String latestModel;
    @org.springframework.beans.factory.annotation.Value("${sum.temperature}")
    private String temperature;
    @org.springframework.beans.factory.annotation.Value("${sum.tokens}")
    private String tokens;
    @org.springframework.beans.factory.annotation.Value("${sum.topP}")
    private String topP;
    @org.springframework.beans.factory.annotation.Value("${sum.topK}")
    private String topK;
    @Setter
    @Getter
    private String instance;

    @RequestMapping(value = "/error")
    public void error(HttpServletResponse response) throws IOException {
        response.sendRedirect("/");
    }

    public String getErrorPath() {
        return "/error";
    }


    @GetMapping("/search")
    public ModelAndView getSearchAI() {
        return new ModelAndView("summary");
    }

    @Operation(summary = "Generate Summary from Transcript", description = "API to generate a summary based on the uploaded transcript files. Accepts multiple files as input and returns the generated summary.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Summary generated successfully.", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request. Possible reasons: no files uploaded or unsupported file type.", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error occurred while processing the request.", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))})
    @PostMapping("/summary")
    public ResponseEntity<String> getSummary(@RequestParam("files") MultipartFile[] uploadFiles, HttpServletRequest request, HttpServletResponse response) {
        StringBuilder responseData = new StringBuilder();
        StringBuilder summaryContent = new StringBuilder();

        for (MultipartFile file : uploadFiles) {
            if (!file.isEmpty()) {
                try {
                    String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);

                    List<String> contentChunks = new ArrayList<>();
                    int chunkSize = 4000;
                    for (int i = 0; i < fileContent.length(); i += chunkSize) {
                        contentChunks.add(fileContent.substring(i, Math.min(i + chunkSize, fileContent.length())));
                    }

                    log.info("Number of chunks available: {}", contentChunks.size());

                    try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                        GenerativeModel model = new GenerativeModel(modelName, vertexAI);

                        for (String chunk : contentChunks) {
                            String prompt = "Please provide a short summary for the following article:\n" + chunk;

                            GenerateContentResponse aiResponse = model.generateContent(prompt);

                            for (Candidate candidate : aiResponse.getCandidatesList()) {
                                JSONObject candidateJson = new JSONObject(new Gson().toJson(candidate));
                                JSONArray parts = candidateJson.getJSONObject(CONTENT).getJSONArray(PARTS);

                                for (int j = 0; j < parts.length(); j++) {
                                    summaryContent.append(parts.getJSONObject(j).getString(DATA));
                                }
                            }
                        }
                    }
                    Path summaryFilePath = Paths.get("summary.txt");
                    Files.writeString(summaryFilePath, summaryContent.toString(), StandardCharsets.UTF_8);
                    responseData.append("Summary for the transcript <a href=./summary>click here to download</a>")
                            .append(System.lineSeparator());
                    log.info("Summary generated for the transcript.");

                } catch (Exception exception) {
                    log.error("Error processing file: {}", exception.getMessage(), exception);
                    responseData.append("Error: ").append(exception.getMessage()).append(System.lineSeparator());
                }
            }
        }
        try {
            getMom(responseData, summaryContent);
            getMindMap(responseData, summaryContent);
        } catch (final IOException iOException) {
            log.error(iOException.getMessage());
        }
        log.info("Minutes of Meeting generated for the transcript.");
        return ResponseEntity.status(HttpStatus.OK).body(responseData.toString());
    }


    public void getMom(StringBuilder responseData, StringBuilder summaryString) throws IOException {
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            String prompt = "Generate minutes of meeting with Date,Time, Meeting Particpants, Meeting Agenda summary and Action Points for the below meeting transcript.\n "
                    + summaryString;

            GenerativeModel model = new GenerativeModel(modelName, vertexAI);

            GenerateContentResponse response02 = model.generateContent(prompt);
            List<Candidate> candidate = response02.getCandidatesList();
            for (Candidate temp : candidate) {
                Gson gson = new Gson();
                String json = gson.toJson(temp);
                JSONObject json1 = new JSONObject(json);

                JSONArray jsonArray01 = (JSONArray) json1.getJSONObject(CONTENT).get(PARTS);
                for (int count = 0; count < jsonArray01.length(); count++) {
                    JSONObject jsonArray02 = jsonArray01.getJSONObject(count);
                    String tempResp = jsonArray02.get(DATA).toString();
                    Files.deleteIfExists(Paths.get("mom.txt"));
                    try (PrintWriter out = new PrintWriter("mom.txt")) {
                        out.println(tempResp);
                    }
                }
            }
        }
        responseData.append("Minutes of Meeting  <a href=/api/mom> click here to download</a> ");
        responseData.append(System.lineSeparator());
    }

    public void getMindMap(StringBuilder responseData, StringBuilder summaryString) throws IOException {
        try {
            StringBuilder sbTemp = new StringBuilder();
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                String prompt = "please Provide summary for the following article \n " + summaryString;
                GenerativeModel model = new GenerativeModel(modelName, vertexAI);

                GenerateContentResponse response01 = model.generateContent(prompt);
                List<Candidate> candidate = response01.getCandidatesList();
                for (Candidate temp : candidate) {
                    Gson gson = new Gson();
                    String json = gson.toJson(temp);
                    JSONObject json1 = new JSONObject(json);

                    JSONArray jsonArray01 = (JSONArray) json1.getJSONObject(CONTENT).get(PARTS);
                    for (int count = 0; count < jsonArray01.length(); count++) {
                        JSONObject jsonArray02 = jsonArray01.getJSONObject(count);

                        String resOutput = jsonArray02.get(DATA).toString();
                        sbTemp.append(resOutput);
                    }
                }
            }

            setInstance("{\n" +
                    "            \"content\":  \"input: Following are the Agile Manifesto principles\n" +
                    "Individuals and interactions In Agile development, self-organization and motivation are important, as are interactions like co-location and pair programming.\n" +
                    "Working software Demo working software is considered the best means of communication with the customers to understand their requirements, instead of just depending on documentation.\n" +
                    "\n" +
                    "output: " +  // Add a missing double quote here
                    "   <node TEXT=\\\"Agile Manifesto principles\\\">\n" +
                    "   <node TEXT=\\\"Individuals and interactions\\\">\n" +
                    "      <node TEXT=\\\"Self-organization and motivation\\\"\\/>\r\n" +
                    "      <node TEXT=\\\"Interactions like co-location and pair programming\\\"\\/>\r\n" +
                    "    <\\/node>\r\n" +
                    "   <node TEXT=\\\"Working software\\\">\r\n" +
                    "      <node TEXT=\\\"Demo working software\\\"\\/>\r\n" +
                    "      <node TEXT=\\\"Communication with the customers\\\"\\/>\r\n" +
                    "      <node TEXT=\\\"Understanding their requirements\\\"\\/>\r\n" +
                    "    <\\/node>\r\n" +
                    "   <\\/node>\r\n" +
                    "input: " + sbTemp + "\n" +
                    "output:\r\n" +
                    "\"\r\n" +
                    "        }");

            StringBuilder sbTemp01 = new StringBuilder();
            try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                String prompt = "please generate the xml content using the example given \n " + getInstance();
                GenerativeModel model = new GenerativeModel(modelName, vertexAI);

                GenerateContentResponse response01 = model.generateContent(prompt);
                List<Candidate> candidate = response01.getCandidatesList();
                for (Candidate temp : candidate) {
                    Gson gson = new Gson();
                    String json = gson.toJson(temp);
                    JSONObject json1 = new JSONObject(json);

                    JSONArray jsonArray01 = (JSONArray) json1.getJSONObject(CONTENT).get(PARTS);
                    for (int count = 0; count < jsonArray01.length(); count++) {
                        JSONObject jsonArray02 = jsonArray01.getJSONObject(count);

                        String resOutput = jsonArray02.get(DATA).toString();
                        sbTemp01.append(resOutput);
                    }
                }
            }

            StringBuilder sb = new StringBuilder("<map version=\"1.0.1\">\r\n");
            sb.append(" <node TEXT=\"Mind Map\">\r\n");
            sb.append(sbTemp01);
            sb.append(" </node>\r\n");
            sb.append("</map>\r\n");
            Files.deleteIfExists(Paths.get("map.mm"));

            try (PrintWriter out = new PrintWriter("map.mm")) {
                out.println(sb);

            }
            sb.setLength(0);
            sbTemp.setLength(0);
            responseData.append("Mind Map  is created <a href=/api/map> click here to download</a> ");
            log.info("Mind Map Generated for the Transcript");
        } catch (Exception exception) {
            log.error("Exception in generating mind map  : {}", exception.getMessage(), exception);

        }
    }
}
