package jomu.instrument.monitor;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DemoMetricsScheduler {

	private final DemoMetrics demoMetrics;

	public DemoMetricsScheduler(DemoMetrics demoMetrics) {
		this.demoMetrics = demoMetrics;
	}

	// @Scheduled(fixedRate = 1000)
	public void triggerCustomMetrics() {
		demoMetrics.getRandomMetricsData();
	}
}