
import java.util.ArrayList;

import java.util.List;

import java.util.Optional;

import java.util.UUID;

import java.util.concurrent.CompletableFuture;

import lombok.Setter;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.ParameterizedTypeReference;

import org.springframework.http.HttpEntity;

import org.springframework.http.HttpMethod;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Service;

import org.springframework.util.MultiValueMap;

import org.springframework.web.client.RestClientException;

import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import uk.gov.landregistry.alerting.client.AlertingClient;

import uk.gov.landregistry.alerting.models.SeverityCode;

import uk.gov.landregistry.cris.common.util.DefaultHeaders;

import uk.gov.landregistry.cris.orchestrator.dto.ApplicationDetails;

import uk.gov.landregistry.cris.orchestrator.dto.CustomerDetails;

import uk.gov.landregistry.cris.registerdetails.dto.RegisterDetails;

import uk.gov.landregistry.cris.registerdetails.dto.RegisterResponse;

import uk.gov.landregistry.cris.registerdetails.service.RegisterDetailsService;

import uk.gov.landregistry.cris.transaction.dto.ApplicationChannelResponse;

import uk.gov.landregistry.cris.transaction.dto.TransactionResponse;

import uk.gov.landregistry.cris.transaction.util.TransactionParameterBuilder;

import uk.gov.landregistry.cris.worklist.dto.WorklistItemResponse;

import uk.gov.landregistry.spring.exception.ApplicationException;

@Slf4j

@Service

public class TransactionService {

	@Setter

	@Value("${CF_TRANSACTION_API}")

	private String CF_TRANSACTION_API;

	@Setter

	@Value("${APP_NAME}")

	private String APPLICATION_NAME;

	@Autowired

	AlertingClient alertingClient;

	@Autowired

	RestTemplate restTemplate;

	@Autowired

	private RegisterDetailsService registerDetailsService;

	public static final String ENDPOINT_MAP_SEARCH = "/transactions/map-search";

	public static final String ENDPOINT_PROPERTY_ALERT = "/transactions/property-alert";

	public static final String ENDPOINT_DIGITAL_REGISTRATION_SERVICE = "/transactions/digital-registration-service";

	public static final String ENDPOINT_APPLICATION_ENQUIRY = "/transactions/application-enquiry";

	public static final String ENDPOINT_PROPERTY_SEARCH = "/transactions/property-search";

	public static final String ENDPOINT_VIEW_COLLEAGUES_APPLICATIONS = "/transactions/view-colleagues-applications";

	public static final String ENDPOINT_SERVICE_CHANNEL = "/transactions/application-channel/";

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return ApplicationDetailsCompletableFuture
	 * 
	 */

	@Async

	public CompletableFuture<ApplicationDetails> getPrelimDetails(WorklistItemResponse worklistItem) {

		log.info(Thread.currentThread().getName() + " TransactionApiService - Retrieving prelim details");

		String url = CF_TRANSACTION_API + "/transactions/preliminary-service/" + worklistItem.getApplicationReference();

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		HttpEntity<?> entity = new HttpEntity<>(DefaultHeaders.getDefaultHeaders());

		ApplicationDetails appDetails;

		try {

			ResponseEntity<ApplicationDetails> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,

					entity, new ParameterizedTypeReference<ApplicationDetails>() {

					});

			System.out.println(alertingClient);

			if (response.getStatusCode() == HttpStatus.OK) {

				appDetails = response.getBody();

			} else {

				throw new RestClientException(

						"Received the following status code from cf-transaction-api: " + response.getStatusCode());

			}

		} catch (RestClientException e) {

			String errorMessage = "Unable to access CF-TRANSACTION-API";

			alertingClient.sendAlert(APPLICATION_NAME, "", errorMessage, e, SeverityCode.ERROR);

			log.info("Error alerted");

			throw new ApplicationException(errorMessage, "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

		return CompletableFuture.completedFuture(appDetails);

	}

	/**
	 * 
	 * @param endpoint   endpoint for query
	 * 
	 * @param parameters parameters for query
	 * 
	 * @return TransactionResponseCompletableFuture
	 * 
	 */

	@Async

	public CompletableFuture<TransactionResponse> getAsyncTransactions(String endpoint,

			MultiValueMap<String, String> parameters) {

		log.info(Thread.currentThread().getName() + " TransactionApiService - getAsyncTransactions - " + endpoint);

		ResponseEntity<TransactionResponse> response = getTransactions(endpoint, parameters);

		TransactionResponse activity = response.getBody();

		return CompletableFuture.completedFuture(activity);

	}

	/**
	 * 
	 * @param endpoint   endpoint for query
	 * 
	 * @param parameters parameters for query
	 * 
	 * @param export     export
	 * 
	 * @return TransactionResponseCompletableFuture
	 * 
	 */

	@Async

	public CompletableFuture<TransactionResponse> getEnquiriesTransactions(String endpoint,

			MultiValueMap<String, String> parameters, Boolean export) {

		log.info(Thread.currentThread().getName() + " TransactionApiService - getEnquiriesTransactions - " + endpoint);

		ResponseEntity<TransactionResponse> response = getTransactions(endpoint, parameters);

		TransactionResponse activity = response.getBody();

		if (activity != null && !activity.getContent().isEmpty() && !export) {

			for (int x = 0; x < activity.getContent().size(); x++) {

				List<String> titles = new ArrayList<String>();

				titles.add(activity.getContent().get(x).getTitleNumber());

				RegisterDetails registerDetails = registerDetailsService.getRegisterDetails(titles, 0, 1);

				CustomerDetails customer = new CustomerDetails();

				if (registerDetails.getRegisters().size() != 0) {

					RegisterResponse registerResponse = new RegisterResponse()

							.registers(registerDetails.getRegisters().get(0))

							.customerDetails(customer)

							.build();

					activity.getContent().get(x).setProprietors(registerResponse.getRegisters().getProprietors());

					activity.getContent().get(x).setProperties(registerResponse.getRegisters().getProperties());

				}

			}

		}

		return CompletableFuture.completedFuture(activity);

	}

	/**
	 * 
	 * @param endpoint   endpoint for query
	 * 
	 * @param parameters parameters for query
	 * 
	 * @return TransactionResponseResponseEntity
	 * 
	 */

	public ResponseEntity<TransactionResponse> getTransactions(String endpoint,

			MultiValueMap<String, String> parameters) {

		String url = CF_TRANSACTION_API + endpoint;

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParams(parameters);

		log.info(builder.toUriString());

		HttpEntity<?> entity = new HttpEntity<>(DefaultHeaders.getDefaultHeaders());

		try {

			ResponseEntity<TransactionResponse> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,

					entity, new ParameterizedTypeReference<TransactionResponse>() {

					});

			if (response.getStatusCode() == HttpStatus.OK) {

				return response;

			} else {

				throw new RestClientException(

						"Received the following status code from cf-transaction-api: " + response.getStatusCode());

			}

		} catch (RestClientException e) {

			String errorMessage = "Unable to access CF-TRANSACTION-API";

			alertingClient.sendAlert(APPLICATION_NAME, "", errorMessage, e, SeverityCode.ERROR);

			log.info("Error alerted");

			throw new ApplicationException(errorMessage, "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	/**
	 * .
	 * 
	 * @param endpoint             endpoint for query
	 * 
	 * @param applicationReference applicationReference for query
	 * 
	 * @return ApplicationChannelCompletableFuture
	 * 
	 */

	public CompletableFuture<ApplicationChannelResponse> getApplicationChannel(String endpoint,

			String applicationReference) {

		String url = CF_TRANSACTION_API + endpoint + applicationReference;

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

		log.info(builder.toUriString());

		HttpEntity<?> entity = new HttpEntity<>(DefaultHeaders.getDefaultHeaders());

		ApplicationChannelResponse appChannelResponse;

		try {

			ResponseEntity<ApplicationChannelResponse> response = restTemplate.exchange(builder.toUriString(),
					HttpMethod.GET,

					entity, new ParameterizedTypeReference<ApplicationChannelResponse>() {

					});

			if (response.getStatusCode() == HttpStatus.OK) {

				appChannelResponse = response.getBody();

			} else {

				throw new RestClientException(

						"Received the following status code from cf-transaction-api: " + response.getStatusCode());

			}

		} catch (RestClientException e) {

			String errorMessage = "Unable to access CF-TRANSACTION-API";

			alertingClient.sendAlert(APPLICATION_NAME, "", errorMessage, e, SeverityCode.ERROR);

			log.info("Error alerted");

			throw new ApplicationException(errorMessage, "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

		return CompletableFuture.completedFuture(appChannelResponse);

	}

	public MultiValueMap<String, String> getPropertySearchParameters(Integer page, Integer size,

			Optional<UUID> userUuid,

			Optional<String> userId, Optional<String> sort) {

		return new TransactionParameterBuilder(page, size, sort)

				.userUuid(userUuid)

				.userId(userId)

				.build();

	}

	public MultiValueMap<String, String> getPropertyAlertParameters(Integer page, Integer size, String userId,

			Optional<String> sort) {

		return new TransactionParameterBuilder(page, size, sort)

				.userId(userId)

				.build();

	}

	public MultiValueMap<String, String> getMapSearchParameters(Integer page, Integer size, String userId,

			String date, Optional<String> sort) {

		return new TransactionParameterBuilder(page, size, sort)

				.userId(userId)

				.date(date)

				.build();

	}

	public MultiValueMap<String, String> getApplicationEnquiryParameters(Integer page, Integer size,

			String userId,

			String timestamp, Optional<String> sort) {

		return new TransactionParameterBuilder(page, size, sort)

				.userId(userId)

				.timestamp(timestamp)

				.build();

	}

	public MultiValueMap<String, String> getDRSDraftOrderParameters(Integer page, Integer size,

			Integer draftOrderId, Optional<String> sort) {

		return new TransactionParameterBuilder(page, size, sort)

				.draftOrderId(draftOrderId)

				.build();

	}

	public MultiValueMap<String, String> getDRSDraftOrderParameters(Integer draftOrderId) {

		return new TransactionParameterBuilder(0, 10, Optional.empty())

				.draftOrderId(draftOrderId)

				.build();

	}

	public MultiValueMap<String, String> getCitizenServiceParameters(Integer page, Integer size,

			Optional<String> sort, Optional<UUID> userUuid, Optional<String> userId,

			Optional<String> titleNumber, Optional<String> postcode) {

		return new TransactionParameterBuilder(page, size, sort)

				.userUuid(userUuid)

				.userId(userId)

				.titleNumber(titleNumber)

				.postcode(postcode)

				.build();

	}

}