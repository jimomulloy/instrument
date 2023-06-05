package jomu.instrument.desktop.control;

import jakarta.enterprise.context.ApplicationScoped;
import jomu.instrument.desktop.monitor.DemoMetrics;

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