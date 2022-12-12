package jomu.instrument.control;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsManager {

	private CompositeMeterRegistry registry;

	public void initialsie() {
		PrometheusMeterRegistry pr = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		SimpleMeterRegistry sr = new SimpleMeterRegistry();
		new ClassLoaderMetrics().bindTo(sr);
		new JvmMemoryMetrics().bindTo(sr);
		new JvmGcMetrics().bindTo(sr);
		new ProcessorMetrics().bindTo(sr);
		new JvmThreadMetrics().bindTo(sr);
		registry = new CompositeMeterRegistry();
		registry.add(sr);
		registry.add(pr);
	}

	MeterRegistry getMeterRegistry() {
		return registry;
	}

}
