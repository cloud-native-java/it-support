@RestController
@Grab("spring-boot-starter-actuator")
class GreetingsRestController {

    @GetMapping("/hi/{name}")
    def hi(@PathVariable String name) {
        [greetings: "Hello, " + name + "!"]
    }
}
