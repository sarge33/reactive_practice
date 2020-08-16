package com.example.webflux;

import com.example.model.Employee;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@SpringBootApplication
public class WebfluxConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxConsumerApplication.class, args);
	}

	protected static final List<Employee> list = Arrays.asList(
			new Employee(1, "Alex", 1000),
			new Employee(2, "Michael", 2000),
			new Employee(3, "Jack", 1500),
			new Employee(4, "Owen", 1500),
			new Employee(5, "Denny", 2000)
	);

	class Cum5xxException extends Exception {
		public Cum5xxException(String msg) {
			super(msg);
		}
	}

	class Cum4xxException extends Exception {
		public Cum4xxException(String msg) {
			super(msg);
		}
	}

	@RestController
	class EmployeeController{
		private static final String baseurl = "http://localhost:8080";
		@GetMapping(value = "employees", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public Flux<Employee> findAll() throws Exception{
			return WebClient.create("localhost:8080/employees")
					.get()
					.retrieve()
					.onStatus(HttpStatus::is5xxServerError, clientResponse -> {
						log.error(clientResponse.statusCode().value() + " error code");
						return Mono.error(new Cum5xxException(clientResponse.statusCode().value() + " error code"));
					})
					.onStatus(HttpStatus::is4xxClientError, clientResponse -> {
						log.error(clientResponse.statusCode().value() + " error code");
						return Mono.error(new Cum4xxException(clientResponse.statusCode().value() + " error code"));
					})
					.onStatus(HttpStatus::isError, clientResponse -> {
						log.error(clientResponse.statusCode().value() + " error code");
						return Mono.error(new Cum4xxException(clientResponse.statusCode().value() + " error code"));
					})
					.bodyToFlux(Employee.class);
		}

		@GetMapping(value = "employees_proxy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public Flux<Employee> findAll1() {
			return WebClient.create("localhost:8080/employees").get().retrieve().bodyToFlux(Employee.class);
		}

		@GetMapping(value = "employees_webclient")
		public Flux<Employee> findAll2() {
			long startTime = System.currentTimeMillis();
			Flux<Employee> employeeFlux = WebClient.create("localhost:8080/employees_list").get().retrieve().bodyToFlux(Employee.class);
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			log.info("webclient took times: {}", duration); // webclient took times: 20
			return employeeFlux;
		}

		@GetMapping(value = "employees_rest")
		public List<Employee> findAll3() {
			long startTime = System.currentTimeMillis();
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<Employee[]> responseEntity = restTemplate.getForEntity("http://localhost:8080/employees_list", Employee[].class);
			List<Employee> employeeList = Arrays.asList(responseEntity.getBody());
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			log.info("RestTemplate took times: {}", duration); // RestTemplate took times: 5020
			return employeeList;
		}
		@PostMapping("save")
		public Mono<Employee> save(@RequestBody Mono<Employee> employeeMono) {
			return WebClient.create("http://localhost:8080/save")
					.post().body(employeeMono, Employee.class)
					.retrieve().bodyToMono(Employee.class);
		}

		@GetMapping("employee")
		public Mono<String> request(@RequestParam Long id, @RequestParam String name, ServerHttpRequest request) {
			return WebClient.create(baseurl)
					.get()
					.uri(uriBuilder -> uriBuilder.path("employee_with_param")
							.queryParam("id", id)
							.queryParam("name", name)
							.build())
					.retrieve()
					.bodyToMono(String.class);
		}

		@GetMapping("employee2/{id}/{name}")
		public Mono<String> request2(@PathVariable Long id, @PathVariable String name) {
			return WebClient.create(baseurl)
					.get()
					.uri(uriBuilder -> uriBuilder.path("employee_with_path_param/{id}/{name}")
							.build(id, name))
					.retrieve()
					.bodyToMono(String.class);
		}
	}




}
