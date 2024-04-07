package com.suga.googlegenai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GooglegenaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GooglegenaiApplication.class, args);
	}

	public int add(int a,int b ) {
		return a+b;
	}
}
