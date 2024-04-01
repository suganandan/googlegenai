package com.suga.googlegenai.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/api/momdownload")
public class MomDownlaodController {

@GetMapping
public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {

Path file = Paths.get("minutesofmeeting.txt");

String contentType = Files.probeContentType(file);
if (contentType == null) {
contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
}

response.setContentType(contentType);
response.setContentLengthLong(Files.size(file));

response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
.filename(file.getFileName().toString(), StandardCharsets.UTF_8).build().toString());
Files.copy(file, response.getOutputStream());
}
}
