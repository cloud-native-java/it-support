import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
@Grab("spring-boot-starter-actuator")
class GreetingsRestController {

    @GetMapping("/hi")
    String hi(@PathVariable String name) {
        return "hello ${name}!"
    }
}
