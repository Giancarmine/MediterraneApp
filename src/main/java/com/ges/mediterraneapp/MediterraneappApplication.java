package com.ges.mediterraneapp;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableEncryptableProperties
public class MediterraneappApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediterraneappApplication.class, args);
	}

}
