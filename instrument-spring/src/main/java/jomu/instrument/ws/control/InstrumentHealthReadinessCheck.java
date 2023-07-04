package jomu.instrument.ws.control;

import java.math.BigDecimal;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.inject.Inject;
import jomu.instrument.ws.service.InstrumentRunner;

@Readiness
public class InstrumentHealthReadinessCheck implements HealthCheck {

	@Inject
	InstrumentRunner instrumentRunner;

	BigDecimal balance;

	@Override
	public HealthCheckResponse call() {
		if (instrumentRunner.isReady()) {
			return HealthCheckResponse.named("InstrumentRunnerCheck").up().build();
		} else {
			return HealthCheckResponse.named("InstrumentRunnerCheck").down().build();
		}
	}
}