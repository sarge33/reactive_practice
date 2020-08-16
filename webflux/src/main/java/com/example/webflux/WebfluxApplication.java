package com.example.webflux;

import com.example.model.Employee;
import java.awt.MediaTracker;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@SpringBootApplication
public class WebfluxApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxApplication.class, args);
	}

	protected static final List<Employee> list = Arrays.asList(
			new Employee(1, "Alex", 1000),
			new Employee(2, "Michael", 2000),
			new Employee(3, "Jack", 1500),
			new Employee(4, "Owen", 1500),
			new Employee(5, "Denny", 2000)
	);

	@RestController
	class GreetingController{
		@GetMapping("greeting")
		public Flux<String> greeting() {
			return Flux.just("Hello world flux");
		}
		@GetMapping("greeting2")
		public Mono<String> greeting2() {
			return Mono.just("Hello world mono");
		}
	}

	@RestController
	class SseController {
		@GetMapping(value="sse")
		public Flux<String> sse() {
			return Flux.interval((Duration.ofMillis(1000))).map(val -> " ->" + val);
		}
		@GetMapping(value="sse2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public Flux<String> sse2() {
			return Flux.interval((Duration.ofMillis(1000))).map(val -> " ->" + val);
		}
	}

	@RestController
	class EmployeeController{
		// return flux
		@ResponseStatus(HttpStatus.BAD_REQUEST)
		@GetMapping(value = "employees", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public Flux<Employee> findAll() {
			return Flux.range(1,5).map(val -> {
				return list.stream().filter(employee -> {
//					if (employee.getId() == val) {
//						int i = 1 / 0;
//						return false;
//					} else {
//						return employee.getId() == val;
//					}
					return employee.getId() == val;
				}).findFirst().get();
			}).delayElements(Duration.ofMillis(1000));
		}

		@GetMapping(value = "employees1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public Flux<Employee> findAll1() {
			return Flux.range(1,5).map(val -> {
				return list.stream().filter(employee -> employee.getId() == val).findFirst().get();
			}).delayElements(Duration.ofMillis(1000));
		}

		// return list
		@GetMapping("employees_list")
		public List<Employee> findAll2() throws InterruptedException {
			Thread.sleep(5000);
			return list;
		}

		@PostMapping("save")
		public Mono<Employee> save(@RequestBody Mono<Employee> employeeMono) {
			Mono<Employee> employeeMono1 = employeeMono.flatMap(employee -> {
				employee.setName(employee.getName() + " had updated");
				return Mono.just(employee);
			});
			return employeeMono1;
		}

		@PostMapping("save2")
		public Mono<Employee> save2(@RequestBody Mono<Employee> employeeMono) {
			Mono<Employee> employeeMono1 = employeeMono.map(employee -> {
				employee.setName(employee.getName() + " had updated");
				return employee;
			});
			return employeeMono1;
		}

		@GetMapping("employee_with_param")
		public Mono<String> request(@RequestParam Long id, @RequestParam String name, ServerHttpRequest request){
			return Mono.just(request.getURI().toString());
		}

		@GetMapping("employee_with_path_param/{id}/{name}")
		public Mono<String> request2(@PathVariable Long id, @PathVariable String name, ServerHttpRequest request){
			return Mono.just(request.getURI().toString());
		}

	}

	@RestController
	class SseController2 {
		// Note: we can ignore "produces = MediaType.TEXT_EVENT_STREAM_VALUE"
		//       since we return Flux<ServerSentEvent<String>>
		@GetMapping(value="sse3")
		public Flux<ServerSentEvent<String>> sse3() {
			Flux<Long> longFlux = Flux.interval(Duration.ofMillis(1000));
			Flux<ServerSentEvent<String>> serverSentEventFlux = longFlux.map(val -> {
				return ServerSentEvent.<String>builder()
					.id(UUID.randomUUID().toString())
					.event("Test_event")
					.data(val.toString())
					.build();
			});
			return serverSentEventFlux;
		}
		@GetMapping(value="sse4")
		public Flux<ServerSentEvent<String>> sse4() {
			return Flux.interval(Duration.ofMillis(1000)).map(val -> {
				return ServerSentEvent.<String>builder()
						.id(UUID.randomUUID().toString())
						.event("Test_event")
						.data(val.toString())
						.build();
			});
		}

		@CrossOrigin
		@GetMapping(value="ping")
		public Flux<ServerSentEvent<String>> sse5() {
			return Flux.interval(Duration.ofMillis(1000)).map(val -> {
				return ServerSentEvent.<String>builder()
						.id(UUID.randomUUID().toString())
						.event("ping")
						.data(val.toString())
						.build();
			});
		}
	}
}
