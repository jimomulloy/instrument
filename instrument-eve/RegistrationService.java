
import java.util.ArrayList;

import java.util.List;

import java.util.UUID;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.ParameterizedTypeReference;

import org.springframework.http.HttpEntity;

import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpMethod;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.web.client.HttpClientErrorException;

import org.springframework.web.client.RestTemplate;

import lombok.Setter;

import lombok.extern.slf4j.Slf4j;

import uk.gov.landregistry.cris.common.util.DefaultHeaders;

import uk.gov.landregistry.cris.orchestrator.dto.CustomerDetails;

import uk.gov.landregistry.cris.orchestrator.mappers.CustomerDetailsMapper;

import uk.gov.landregistry.cris.registerdetails.dto.Register;

import uk.gov.landregistry.cris.registerdetails.dto.RegisterDetails;

import uk.gov.landregistry.cris.registerdetails.dto.RegisterResponse;

import uk.gov.landregistry.cris.users.dto.CitizenUser;

import uk.gov.landregistry.cris.users.dto.LegacyUser;

import uk.gov.landregistry.cris.users.service.UserService;

import uk.gov.landregistry.spring.exception.ApplicationException;

import uk.gov.landregistry.cris.common.util.ServiceType;

@Slf4j

@Service

public class RegisterDetailsService {

	@Setter

	@Value("${REGISTER_BEARER_TOKEN}")

	private String REGISTER_BEARER_TOKEN;

	@Setter

	@Value("${DIGITAL_REGISTER_API}")

	private String DIGITAL_REGISTER_API;

	@Autowired

	private RestTemplate restTemplate;

	@Autowired

	private UserService userService;

	@Autowired

	private CustomerDetailsMapper customerMapper;

	@Autowired

	private ServiceType serviceType;

	/**
	 * 
	 * @param title the title number that requires details
	 * 
	 * @return ResponseEntity<RegisterDetails>
	 * 
	 * @throws HTTPClientErrorException if the API call fails
	 * 
	 */

	private ResponseEntity<Register> getDigitalRegister(String title) throws HttpClientErrorException {

		String url = DIGITAL_REGISTER_API;

		log.info("Calling land register API... ");

		HttpHeaders headers = DefaultHeaders.getDefaultHeaders();

		headers.add("Authorization", "Bearer " + REGISTER_BEARER_TOKEN);

		HttpEntity<?> entity = new HttpEntity<>(headers);

		ResponseEntity<Register> response = restTemplate.exchange(url + "/" + title,

				HttpMethod.GET,

				entity,

				new ParameterizedTypeReference<Register>() {

				});

		return response;

	}

	private List<Register> mapTitleNumbersToRegisterObjects(List<String> titlesList) {

		List<Register> registers = new ArrayList<>();

		for (String title : titlesList) {

			Register register = new Register(title, null, null, null, null, null, null);

			registers.add(register);

		}

		return registers;

	}

	private RegisterDetails processRegisterDetails(List<String> titlesList, Integer page, Integer size) {

		int statusCode;

		List<Register> registers = new ArrayList<>();

		try {

			for (String titles : titlesList) {

				if (titles != null && !titles.isEmpty()) {

					ResponseEntity<Register> response = getDigitalRegister(titles);

					registers.add(response.getBody());

				}

			}

			// If all requests are successful, httpStatus is 200 OK.

			statusCode = 200;

		} catch (HttpClientErrorException error) {

			log.debug("Error calling land-register-api: ", error.getStatusCode(), error.getMessage());

			statusCode = error.getStatusCode().value();

			// If 500, map title numbers only

			if (statusCode == 500) {

				registers = mapTitleNumbersToRegisterObjects(titlesList);

			} else {

				String errorCode = "E" + String.valueOf(statusCode);

				throw new ApplicationException(error.getMessage(),

						errorCode, statusCode, error, false);

			}

		}

		RegisterDetails digitalRegisterDetails;

		if (page == null) {

			digitalRegisterDetails = new RegisterDetails(registers, statusCode, 0, size);

		} else {

			digitalRegisterDetails = new RegisterDetails(registers, statusCode, page, size);

		}

		return digitalRegisterDetails;

	}

	/**
	 * 
	 * This method calls the API and passes back a JSON response entity.
	 * 
	 * If there is a 200 OK, all data is passed back with the status code
	 * 
	 * If there is a 500 INTERNAL_SERVER_ERROR, only titles are passed back with the
	 * 
	 * status code
	 * 
	 * This data is mapped to the same JSON and returned.
	 *
	 * 
	 * 
	 * @param titlesList a list of title numbers that require details
	 * 
	 * @param page       the page number for the page to load
	 * 
	 * @param size       the amount of elements we want to show on each page
	 * 
	 * @return RegisterDetails CompletableFuture
	 * 
	 * @throws ApplicationException if status code is not 200 or 500
	 * 
	 **/

	public CompletableFuture<RegisterDetails> getRegisterDetailsSummary(List<String> titlesList, Integer page,
			Integer size) {

		RegisterDetails digitalRegisterDetails = processRegisterDetails(titlesList, page, size);

		return CompletableFuture.completedFuture(digitalRegisterDetails);

	}

	/**
	 * 
	 * This method calls the API and passes back a JSON response entity.
	 * 
	 * If there is a 200 OK, all data is passed back with the status code
	 * 
	 * If there is a 500 INTERNAL_SERVER_ERROR, only titles are passed back with the
	 * 
	 * status code
	 * 
	 * This data is mapped to the same JSON and returned.
	 *
	 * 
	 * 
	 * @param titlesList a list of title numbers that require details
	 * 
	 * @param page       the page number for the page to load
	 * 
	 * @param size       the amount of elements we want to show on each page
	 * 
	 * @return RegisterDetails
	 * 
	 * @throws ApplicationException if status code is not 200 or 500
	 * 
	 **/

	public RegisterDetails getRegisterDetails(List<String> titlesList, Integer page, Integer size) {

		RegisterDetails digitalRegisterDetails = processRegisterDetails(titlesList, page, size);

		return digitalRegisterDetails;

	}

	/**
	 * 
	 * This method sets up calls to register api and also gets customer details
	 *
	 * 
	 * 
	 * @param titleNumber a title number that requires details
	 * 
	 * @param service     the service name
	 * 
	 * @param appnRef     the prelim reference number
	 * 
	 * @param userId      the reference of the customer
	 * 
	 * @param userUuid    the uuid of the customer
	 * 
	 * @return RegisterResponse
	 * 
	 **/

	public RegisterResponse getRegisterInformation(String titleNumber, String service, String appnRef, String userId,
			UUID userUuid) {

		log.info("get register for title number {}", titleNumber);

		try {

			List<String> titles = new ArrayList<String>();

			titles.add(titleNumber);

			CompletableFuture<RegisterDetails> registerDetails = getRegisterDetailsSummary(titles, 0, 1);

			int serviceTypeValue = serviceType.getServiceType(service);

			if (serviceTypeValue == 1) {

				CompletableFuture<CustomerDetails> customerDetails = new CompletableFuture<CustomerDetails>();

				if (service.equals("Preliminary Services")) {

					customerDetails = userService.getPrelimUserDetails(appnRef);

				} else {

					customerDetails = userService.getPortalUserDetails(userId, service);

				}

				CompletableFuture.allOf(customerDetails, registerDetails).join();

				RegisterResponse registerResponse = new RegisterResponse()

						.registers(registerDetails.get().getRegisters().get(0))

						.customerDetails(customerDetails.get())

						.build();

				return registerResponse;

			} else {

				CustomerDetails customerDetails = new CustomerDetails();

				if (service.equals("Search for Land and Property Information")) {

					CompletableFuture<CitizenUser> customer = userService.getCitizenUser(userUuid, service);

					CompletableFuture.allOf(customer, registerDetails).join();

					customerDetails = customerMapper.map(customer.get());

				} else {

					CompletableFuture<LegacyUser> customer = userService.getLegacyUser(service, userId);

					CompletableFuture.allOf(customer, registerDetails).join();

					customerDetails = customerMapper.map(customer.get());

				}

				RegisterResponse registerResponse = new RegisterResponse()

						.registers(registerDetails.get().getRegisters().get(0))

						.customerDetails(customerDetails)

						.build();

				return registerResponse;

			}

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

}