package jomu.instrument;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SpringInstrument implements CommandLineRunner {

	public static void main(String[] args) {
		new SpringApplicationBuilder(SpringInstrument.class).headless(false).run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
	}
}