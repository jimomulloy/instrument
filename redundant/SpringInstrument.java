package jomu.instrument;

import java.lang.management.ManagementFactory;

import jakarta.management.MBeanServer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
//@EnableScheduling
public class SpringInstrument implements CommandLineRunner {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SpringInstrument.class).headless(false)
				.run(args);
		Instrument instrument = ctx.getBean(Instrument.class);
		Instrument.setInstance(instrument);
		instrument.initialise();
		instrument.start();
	}

	@Override
	public void run(String... args) throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
	}
}