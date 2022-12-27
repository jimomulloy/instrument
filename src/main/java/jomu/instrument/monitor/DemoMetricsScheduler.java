package jomu.instrument.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoMetricsScheduler {

	private final DemoMetrics demoMetrics;

	public DemoMetricsScheduler(DemoMetrics demoMetrics) {
		this.demoMetrics = demoMetrics;
		System.out.println(">>init: DemoMetricsScheduler");
	}

	@Scheduled(fixedRate = 1000)
	public void triggerCustomMetrics() {
		demoMetrics.getRandomMetricsData();
		System.out.println(">>call: DemoMetricsScheduler");
	}
}