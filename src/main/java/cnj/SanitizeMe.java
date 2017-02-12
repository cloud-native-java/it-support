package cnj;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
public class SanitizeMe {
	static public void main(String[] args) throws Throwable {
		String sanitizerClass = "org.springframework.boot.actuate.endpoint.Sanitizer";
		Class<?> sanitizer = Class.forName(sanitizerClass);
		Constructor<?> ctor = sanitizer.getDeclaredConstructor();
		ctor.setAccessible(true);
		Object sanitizerObject = ctor.newInstance();
		Method sanitizeMethod = sanitizer.getMethod("sanitize", String.class, Object.class);
		sanitizeMethod.setAccessible(true);
		String pw = String.class.cast(sanitizeMethod.invoke(
				sanitizerObject, "my-dirty-secret", "cowbell08"));
		System.out.println("password: " + pw);

	}
}
