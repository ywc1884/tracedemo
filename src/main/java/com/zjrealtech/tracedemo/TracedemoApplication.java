package com.zjrealtech.tracedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.zjrealtech.tracedemo.**")
public class TracedemoApplication {

	public static void main(String[] args) {

	    ApplicationContext context = SpringApplication.run(TracedemoApplication.class, args);
        TraceDownDemo demo = context.getBean(TraceDownDemo.class);
        demo.sqlTest();
	}
}
