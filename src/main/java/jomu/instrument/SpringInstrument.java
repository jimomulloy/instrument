package jomu.instrument;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

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
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		Instrument instrument = Instrument.getInstance();
		instrument.initialise();
	}
}