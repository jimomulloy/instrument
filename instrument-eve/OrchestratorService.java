import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

import java.util.Collections;

import java.util.HashSet;

import java.util.List;

import java.util.Optional;

import java.util.Set;

import java.util.UUID;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.util.LinkedMultiValueMap;

import org.springframework.util.MultiValueMap;

import uk.gov.landregistry.cris.common.util.ServiceType;

import uk.gov.landregistry.cris.note.service.NoteService;

import uk.gov.landregistry.cris.orchestrator.dto.AccountDetails;

import uk.gov.landregistry.cris.orchestrator.dto.AccountInformationResponse;

import uk.gov.landregistry.cris.orchestrator.dto.ApplicationDetails;

import uk.gov.landregistry.cris.orchestrator.dto.CustomerDetails;

import uk.gov.landregistry.cris.orchestrator.dto.FormattedNote;

import uk.gov.landregistry.cris.orchestrator.dto.MatchedAccounts;

import uk.gov.landregistry.cris.orchestrator.dto.MatchedAccountsResponse;

import uk.gov.landregistry.cris.orchestrator.dto.PropertyEnquiryResponse;

import uk.gov.landregistry.cris.orchestrator.dto.Services;

import uk.gov.landregistry.cris.orchestrator.dto.WorklistInformationResponse;

import uk.gov.landregistry.cris.orchestrator.mappers.AccountDetailsMapper;

import uk.gov.landregistry.cris.orchestrator.mappers.CustomerDetailsMapper;

import uk.gov.landregistry.cris.registerdetails.dto.RegisterDetails;

import uk.gov.landregistry.cris.registerdetails.service.RegisterDetailsService;

import uk.gov.landregistry.cris.transaction.dto.ApplicationChannelResponse;

import uk.gov.landregistry.cris.transaction.dto.TransactionResponse;

import uk.gov.landregistry.cris.transaction.service.TransactionService;

import uk.gov.landregistry.cris.users.dto.CitizenUser;

import uk.gov.landregistry.cris.users.dto.CitizenUserHistory;

import uk.gov.landregistry.cris.users.dto.CitizenUserHistoryResponse;

import uk.gov.landregistry.cris.users.dto.LegacyUser;

import uk.gov.landregistry.cris.users.service.UserService;

import uk.gov.landregistry.cris.worklist.dto.WorklistItemResponse;

import uk.gov.landregistry.spring.exception.ApplicationException;

@Slf4j

@Service

public class OrchestratorService {

	@Autowired

	private UserService userService;

	@Autowired

	private NoteService noteService;

	@Autowired

	private RegisterDetailsService registerDetailsService;

	@Autowired

	private TransactionService transactionService;

	@Autowired

	private AccountDetailsMapper accountMapper;

	@Autowired

	private CustomerDetailsMapper customerMapper;

	@Autowired

	private ModelMapper modelMapper;

	@Autowired

	private ServiceType serviceType;

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 */

	public ResponseEntity<WorklistInformationResponse> buildWorklistInformation(WorklistItemResponse worklistItem) {

		String service = worklistItem.getService();

		log.info("Requesting " + service + " Info");

		try {

			if (service.equals(Services.CITIZEN_ACCOUNT)) {

				return getCitizenAccountInfo(worklistItem);

			} else if (service.equals(Services.FAP)

					|| service.equals(Services.FPI)

					|| service.equals(Services.INSPIRE)) {

				return getPropertySearchInfo(worklistItem);

			} else if (service.equals(Services.PA)) {

				return getPropertyAlertInfo(worklistItem);

			} else if (service.equals(Services.SFLAPI)) {

				return getSFLAPIInfo(worklistItem);

			} else if (service.equals(Services.MAP_SEARCH)) {

				return getMapSearchInfo(worklistItem);

			} else if (service.equals(Services.APPLICATION_ENQUIRY)) {

				return getApplicationEnquiryInfo(worklistItem);

			} else if (service.equals(Services.PRELIMINARY_SERVICES)) {

				return getPrelimInfo(worklistItem);

			} else if (service.equals(Services.VIEW_MY_APPLICATION)) {

				return viewMyApplicationInfo(worklistItem);

			} else if (service.equals(Services.DRS)) {

				return getDigitalRegistrationServiceInfo(worklistItem);

			} else if (service.equals(Services.BG)) {

				return getBusinessGatewayInfo(worklistItem);

			} else if (service.equals(Services.VCA)) {

				return getViewColleaguesApplicationsInfo(worklistItem);

			} else {

				log.info("service name not found");

				throw new ApplicationException("Service name not found", "E400",

						HttpStatus.BAD_REQUEST.value(), false);

			}

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getCitizenAccountInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CitizenUser> citizenUser = userService.getCitizenUser(worklistItem.getUserUuid(),

				worklistItem.getService());

		MultiValueMap<String, String> citizenHistoryParameters = new LinkedMultiValueMap<>();

		citizenHistoryParameters.add("page_size", "5");

		citizenHistoryParameters.add("sort", "-changed_date");

		CompletableFuture<CitizenUserHistoryResponse> citizenUserHistoryResponse = userService
				.getCitizenUserHistory(worklistItem.getUserUuid(), citizenHistoryParameters);

		CompletableFuture<List<FormattedNote>> notes = noteService.getNotes(worklistItem.getUserUuid());

		CompletableFuture.allOf(citizenUser, notes, citizenUserHistoryResponse).join();

		MatchedAccounts matchedAccounts = userService.getMatchedAccounts(0, 10, Optional.empty(),

				citizenUser.get().getEmailAddress());

		AccountDetails accountDetails = accountMapper.map(notes.get(), citizenUser.get());

		CustomerDetails customerDetails = customerMapper.map(citizenUser.get());

		CitizenUserHistory citizenHistory = modelMapper.map(citizenUserHistoryResponse.get(), CitizenUserHistory.class);

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.accountDetails(accountDetails)

				.citizenUserHistory(citizenHistory)

				.customerDetails(customerDetails)

				.matchedAccounts(matchedAccounts)

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> viewMyApplicationInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(worklistItem.getUserId(),

				worklistItem.getService());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService.

				getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		if (worklistItem.getApplicationReference() != null) {

			CompletableFuture<ApplicationChannelResponse> channel = transactionService.getApplicationChannel(

					TransactionService.ENDPOINT_SERVICE_CHANNEL,

					worklistItem.getApplicationReference());

			CompletableFuture.allOf(customer, registerDetails, channel).join();

			WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

					.customerDetails(customer.get())

					.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

					.registerDetails(registerDetails.get())

					.channelDetails(channel.get())

					.build();

			return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

		} else {

			CompletableFuture.allOf(customer, registerDetails).join();

			WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

					.customerDetails(customer.get())

					.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

					.registerDetails(registerDetails.get())

					.build();

			return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

		}

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getPrelimInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CustomerDetails> customer = userService

				.getPrelimUserDetails(worklistItem.getApplicationReference());

		CompletableFuture<ApplicationDetails> applicationDetails = transactionService

				.getPrelimDetails(worklistItem);

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(customer, applicationDetails, registerDetails).join();

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customer.get())

				.applicationDetails(applicationDetails.get(), worklistItem.getResultTimestamp())

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getMapSearchInfo(

			WorklistItemResponse worklistItem) throws InterruptedException, ExecutionException {

		String worklistDate = worklistItem.getResultTimestamp().toLocalDate().toString();

		// create seperate threads for each rest call and join them back together

		MultiValueMap<String, String> parameters = transactionService.getMapSearchParameters(0, 10,

				worklistItem.getUserId(), worklistDate, Optional.empty());

		CompletableFuture<TransactionResponse> activity = transactionService.getAsyncTransactions(

				TransactionService.ENDPOINT_MAP_SEARCH, parameters);

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(worklistItem.getUserId(),

				worklistItem.getService());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(customer, activity, registerDetails).join();

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customer.get())

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.activityDetails(activity.get(), worklistItem.getResultTimestamp())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getApplicationEnquiryInfo(

			WorklistItemResponse worklistItem) throws InterruptedException, ExecutionException {

		String worklistTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

				.format(worklistItem.getResultTimestamp()).toString();

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(worklistItem.getUserId(),

				worklistItem.getService());

		MultiValueMap<String, String> parameters = transactionService.getApplicationEnquiryParameters(0, 10,

				worklistItem.getUserId(), worklistTimestamp, Optional.empty());

		CompletableFuture<TransactionResponse> activity = transactionService.getAsyncTransactions(

				TransactionService.ENDPOINT_APPLICATION_ENQUIRY, parameters);

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(customer, activity, registerDetails).join();

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customer.get())

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.activityDetails(activity.get(), worklistItem.getResultTimestamp())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getBusinessGatewayInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(worklistItem.getUserId(),

				worklistItem.getService());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(customer, registerDetails).join();

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customer.get())

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * .
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getViewColleaguesApplicationsInfo(
			WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		String worklistTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")

				.format(worklistItem.getResultTimestamp()).toString();

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(worklistItem.getUserId(),

				worklistItem.getService());

		MultiValueMap<String, String> parameters = transactionService.getApplicationEnquiryParameters(0, 10,

				worklistItem.getUserId(), worklistTimestamp, Optional.empty());

		CompletableFuture<TransactionResponse> activity = transactionService.getAsyncTransactions(

				TransactionService.ENDPOINT_VIEW_COLLEAGUES_APPLICATIONS, parameters);

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		// Get application channel lodgement information

		if (worklistItem.getApplicationReference() != null) {

			CompletableFuture<ApplicationChannelResponse> channel = transactionService.getApplicationChannel(

					TransactionService.ENDPOINT_SERVICE_CHANNEL,

					worklistItem.getApplicationReference());

			CompletableFuture.allOf(customer, activity, registerDetails, channel).join();

			WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

					.customerDetails(customer.get())

					.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

					.registerDetails(registerDetails.get())

					.activityDetails(activity.get(), worklistItem.getResultTimestamp())

					.channelDetails(channel.get())

					.build();

			return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

		} else {

			CompletableFuture.allOf(customer, activity, registerDetails).join();

			WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

					.customerDetails(customer.get())

					.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

					.registerDetails(registerDetails.get())

					.activityDetails(activity.get(), worklistItem.getResultTimestamp())

					.build();

			return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

		}

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getPropertySearchInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		MultiValueMap<String, String> parameters = transactionService.getPropertySearchParameters(0, 10,

				Optional.empty(), Optional.of(worklistItem.getUserId()), Optional.empty());

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_PROPERTY_SEARCH, parameters);

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(worklistItem.getService(),

				worklistItem.getUserId());

		CompletableFuture<List<FormattedNote>> notes = noteService.getNotes(worklistItem.getUserId());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(legacyUser, notes, propertyEnquiries, registerDetails).join();

		MatchedAccounts matchedAccounts = userService.getMatchedAccounts(0, 10, Optional.empty(),

				legacyUser.get().getEmailAddress());

		// map fields from legacy user to the correct objects

		AccountDetails accountDetails = accountMapper.map(notes.get(), legacyUser.get());

		CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customerDetails)

				.accountDetails(accountDetails)

				.matchedAccounts(matchedAccounts)

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.propertyEnquiries(propertyEnquiries.get())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getPropertyAlertInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		MultiValueMap<String, String> parameters = transactionService.getPropertyAlertParameters(0, 10,

				worklistItem.getUserId(),

				Optional.empty());

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_PROPERTY_ALERT, parameters);

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(worklistItem.getService(),

				worklistItem.getUserId());

		CompletableFuture<List<FormattedNote>> notes = noteService.getNotes(worklistItem.getUserId());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(legacyUser, notes, propertyEnquiries, registerDetails).join();

		MatchedAccounts matchedAccounts = userService.getMatchedAccounts(0, 10, Optional.empty(),

				legacyUser.get().getEmailAddress());

		// map fields from legacy user to the correct objects

		AccountDetails accountDetails = accountMapper.map(notes.get(), legacyUser.get());

		CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customerDetails)

				.accountDetails(accountDetails)

				.matchedAccounts(matchedAccounts)

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.propertyEnquiries(propertyEnquiries.get())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getSFLAPIInfo(WorklistItemResponse worklistItem)

			throws InterruptedException, ExecutionException {

		// create seperate threads for each rest call and join them back together

		MultiValueMap<String, String> parameters = transactionService.getPropertySearchParameters(0, 10,

				Optional.of(worklistItem.getUserUuid()), Optional.empty(), Optional.empty());

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_PROPERTY_SEARCH, parameters);

		CompletableFuture<CitizenUser> citizenUser = userService.getCitizenUser(worklistItem.getUserUuid(),

				worklistItem.getService());

		CompletableFuture<List<FormattedNote>> notes = noteService.getNotes(worklistItem.getUserUuid());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		MultiValueMap<String, String> citizenHistoryParameters = new LinkedMultiValueMap<>();

		citizenHistoryParameters.add("page_size", "5");

		citizenHistoryParameters.add("sort", "-changed_date");

		CompletableFuture<CitizenUserHistoryResponse> citizenUserHistoryResponse = userService
				.getCitizenUserHistory(worklistItem.getUserUuid(), citizenHistoryParameters);

		CompletableFuture.allOf(citizenUser, notes, propertyEnquiries, registerDetails, citizenUserHistoryResponse)
				.join();

		MatchedAccounts matchedAccounts = userService.getMatchedAccounts(0, 10, Optional.empty(),

				citizenUser.get().getEmailAddress());

		// map fields from legacy user to the correct objects

		AccountDetails accountDetails = accountMapper.map(notes.get(), citizenUser.get());

		CustomerDetails customerDetails = customerMapper.map(citizenUser.get());

		CitizenUserHistory citizenHistory = modelMapper.map(citizenUserHistoryResponse.get(), CitizenUserHistory.class);

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customerDetails)

				.citizenUserHistory(citizenHistory)

				.accountDetails(accountDetails)

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.propertyEnquiries(propertyEnquiries.get())

				.matchedAccounts(matchedAccounts)

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	/**
	 * 
	 * @param worklistItem the worklist item
	 * 
	 * @return WorklistInformationResponseResponseEntity
	 * 
	 * @throws ExecutionException
	 * 
	 * @throws InterruptedException
	 * 
	 */

	private ResponseEntity<WorklistInformationResponse> getDigitalRegistrationServiceInfo(

			WorklistItemResponse worklistItem) throws InterruptedException, ExecutionException {

		MultiValueMap<String, String> parameters = transactionService.getDRSDraftOrderParameters(0, 10,

				worklistItem.getDraftOrderId(), Optional.empty());

		CompletableFuture<TransactionResponse> activity = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_DIGITAL_REGISTRATION_SERVICE,

						parameters);

		// create seperate threads for each rest call and join them back together

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(worklistItem.getUserId(),

				worklistItem.getService());

		CompletableFuture<RegisterDetails> registerDetails = registerDetailsService

				.getRegisterDetailsSummary(getTitles(worklistItem.getTitleNumbers(), 5, null), null, 5);

		CompletableFuture.allOf(customer, activity, registerDetails).join();

		WorklistInformationResponse worklistInformation = new WorklistInformationResponse(worklistItem)

				.customerDetails(customer.get())

				.activityDetails(activity.get(), worklistItem.getResultTimestamp())

				.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

				.registerDetails(registerDetails.get())

				.build();

		return new ResponseEntity<WorklistInformationResponse>(worklistInformation, HttpStatus.OK);

	}

	public ResponseEntity<AccountInformationResponse> getCitizenAccountInformation(UUID id) {

		CompletableFuture<CitizenUser> citizenUser = userService.getCitizenUser(id, Services.CITIZEN_ACCOUNT);

		CompletableFuture<List<FormattedNote>> notes = noteService.getNotes(id);

		MultiValueMap<String, String> citizenHistoryParameters = new LinkedMultiValueMap<>();

		citizenHistoryParameters.add("page_size", "5");

		citizenHistoryParameters.add("sort", "-changed_date");

		CompletableFuture<CitizenUserHistoryResponse> citizenUserHistoryResponse = userService.getCitizenUserHistory(id,
				citizenHistoryParameters);

		MultiValueMap<String, String> propertySearchParameters = transactionService.getPropertySearchParameters(0, 10,

				Optional.of(id), Optional.empty(), Optional.empty());

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_PROPERTY_SEARCH, propertySearchParameters);

		CompletableFuture.allOf(citizenUser, notes, propertyEnquiries, citizenUserHistoryResponse).join();

		try {

			MatchedAccounts matchedAccounts = userService.getMatchedAccounts(0, 10, Optional.empty(),

					citizenUser.get().getEmailAddress());

			// map fields from legacy user to the correct objects

			AccountDetails accountDetails = accountMapper.map(notes.get(), citizenUser.get());

			CustomerDetails customerDetails = customerMapper.map(citizenUser.get());

			CitizenUserHistory citizenHistory = modelMapper.map(citizenUserHistoryResponse.get(),
					CitizenUserHistory.class);

			AccountInformationResponse accountInformation = new AccountInformationResponse()

					.customerDetails(customerDetails)

					.citizenUserHistory(citizenHistory)

					.accountDetails(accountDetails)

					.propertyEnquiries(propertyEnquiries.get())

					.matchedAccounts(matchedAccounts)

					.build();

			return new ResponseEntity<AccountInformationResponse>(accountInformation, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<AccountInformationResponse> getLegacyAccountInformation(String id, String service) {

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(service, id);

		CompletableFuture<List<FormattedNote>> notes = noteService.getNotes(id);

		CompletableFuture<TransactionResponse> propertyEnquiries;

		if (service.equalsIgnoreCase(Services.PA)) {

			MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

			parameters = transactionService.getPropertyAlertParameters(0, 10, id, Optional.empty());

			propertyEnquiries = transactionService.getAsyncTransactions(

					TransactionService.ENDPOINT_PROPERTY_ALERT,

					parameters);

		} else {

			MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

			parameters = transactionService.getPropertySearchParameters(0, 10, Optional.empty(),

					Optional.of(id),

					Optional.empty());

			propertyEnquiries = transactionService.getAsyncTransactions(

					TransactionService.ENDPOINT_PROPERTY_SEARCH,

					parameters);

		}

		CompletableFuture.allOf(legacyUser, notes, propertyEnquiries).join();

		try {

			MatchedAccounts matchedAccounts = userService.getMatchedAccounts(0, 10, Optional.empty(),

					legacyUser.get().getEmailAddress());

			// map fields from legacy user to the correct objects

			AccountDetails accountDetails = accountMapper.map(notes.get(), legacyUser.get());

			CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

			AccountInformationResponse accountInformation = new AccountInformationResponse()

					.customerDetails(customerDetails)

					.accountDetails(accountDetails)

					.propertyEnquiries(propertyEnquiries.get())

					.matchedAccounts(matchedAccounts)

					.build();

			return new ResponseEntity<AccountInformationResponse>(accountInformation, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<PropertyEnquiryResponse> getCitizenEnquiries(UUID id, Integer page, Integer size,

			Optional<String> sort, Boolean export) {

		CompletableFuture<CitizenUser> citizenUser = userService.getCitizenUser(id, Services.CITIZEN_ACCOUNT);

		MultiValueMap<String, String> parameters = transactionService.getPropertySearchParameters(page, size,

				Optional.of(id), Optional.empty(), sort);

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getEnquiriesTransactions(TransactionService.ENDPOINT_PROPERTY_SEARCH, parameters, export);

		CompletableFuture.allOf(citizenUser, propertyEnquiries).join();

		try {

			// map fields from legacy user to the correct objects

			CustomerDetails customerDetails = customerMapper.map(citizenUser.get());

			PropertyEnquiryResponse enquiryResponse = new PropertyEnquiryResponse(customerDetails,

					propertyEnquiries.get());

			return new ResponseEntity<PropertyEnquiryResponse>(

					enquiryResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<PropertyEnquiryResponse> getPropertyAlertEnquiries(String id, Integer page, Integer size,

			Optional<String> sort, Boolean export) {

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(Services.PA, id);

		MultiValueMap<String, String> parameters = transactionService.getPropertyAlertParameters(page, size,

				id, sort);

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getEnquiriesTransactions(TransactionService.ENDPOINT_PROPERTY_ALERT, parameters, export);

		CompletableFuture.allOf(legacyUser, propertyEnquiries).join();

		try {

			// map fields from legacy user to the correct objects

			CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

			PropertyEnquiryResponse enquiryResponse = new PropertyEnquiryResponse(customerDetails,

					propertyEnquiries.get());

			return new ResponseEntity<PropertyEnquiryResponse>(enquiryResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<PropertyEnquiryResponse> getPropertySearchEnquiries(String id, Integer page, Integer size,

			Optional<String> sort) {

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(Services.FAP, id);

		MultiValueMap<String, String> parameters = transactionService.getPropertySearchParameters(page, size,

				Optional.empty(), Optional.of(id), sort);

		CompletableFuture<TransactionResponse> propertyEnquiries = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_PROPERTY_SEARCH, parameters);

		CompletableFuture.allOf(legacyUser, propertyEnquiries).join();

		try {

			// map fields from legacy user to the correct objects

			CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

			PropertyEnquiryResponse enquiryResponse = new PropertyEnquiryResponse(customerDetails,

					propertyEnquiries.get());

			return new ResponseEntity<PropertyEnquiryResponse>(enquiryResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<MatchedAccountsResponse> getPropertyAlertMatchedAccounts(String id, Integer page,

			Integer size, Optional<String> sort) {

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(Services.PA, id);

		CompletableFuture.allOf(legacyUser).join();

		try {

			// map fields from legacy user to the correct objects

			CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

			MatchedAccounts matchedAccounts = userService.getMatchedAccounts(page, size, sort,

					legacyUser.get().getEmailAddress());

			MatchedAccountsResponse matchedAccountsResponse = new MatchedAccountsResponse(customerDetails,

					matchedAccounts);

			return new ResponseEntity<MatchedAccountsResponse>(matchedAccountsResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<MatchedAccountsResponse> getPropertySearchMatchedAccounts(String id, Integer page,

			Integer size, Optional<String> sort) {

		CompletableFuture<LegacyUser> legacyUser = userService.getLegacyUser(Services.FAP, id);

		CompletableFuture.allOf(legacyUser).join();

		try {

			// map fields from legacy user to the correct objects

			CustomerDetails customerDetails = customerMapper.map(legacyUser.get());

			MatchedAccounts matchedAccounts = userService.getMatchedAccounts(page, size, sort,

					legacyUser.get().getEmailAddress());

			MatchedAccountsResponse matchedAccountsResponse = new MatchedAccountsResponse(customerDetails,

					matchedAccounts);

			return new ResponseEntity<MatchedAccountsResponse>(matchedAccountsResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<MatchedAccountsResponse> getCitizenMatchedAccounts(UUID id, Integer page, Integer size,

			Optional<String> sort) {

		CompletableFuture<CitizenUser> citizenUser = userService.getCitizenUser(id,

				Services.CITIZEN_ACCOUNT);

		CompletableFuture.allOf(citizenUser).join();

		try {

			// map fields from legacy user to the correct objects

			CustomerDetails customerDetails = customerMapper.map(citizenUser.get());

			MatchedAccounts matchedAccounts = userService.getMatchedAccounts(page, size, sort,

					citizenUser.get().getEmailAddress());

			MatchedAccountsResponse matchedAccountsResponse = new MatchedAccountsResponse(customerDetails,
					matchedAccounts);

			return new ResponseEntity<MatchedAccountsResponse>(matchedAccountsResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<PropertyEnquiryResponse> getMapOrApplicationActivities(String id, String dateOrTimestamp,

			Integer page, Integer size, Optional<String> sort, String service) {

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(id, service);

		CompletableFuture<TransactionResponse> activity;

		if (service.equals(Services.MAP_SEARCH)) {

			MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

			String date = dateOrTimestamp;

			parameters = transactionService.getMapSearchParameters(page, size, id, date, sort);

			activity = transactionService.getAsyncTransactions(TransactionService.ENDPOINT_MAP_SEARCH,

					parameters);

		} else {

			MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

			String timestamp = dateOrTimestamp;

			parameters = transactionService.getApplicationEnquiryParameters(page, size, id, timestamp,

					sort);

			activity = transactionService.getEnquiriesTransactions(

					TransactionService.ENDPOINT_APPLICATION_ENQUIRY, parameters, false);

		}

		CompletableFuture.allOf(customer, activity).join();

		try {

			PropertyEnquiryResponse enquiryResponse = new PropertyEnquiryResponse(customer.get(), activity.get());

			return new ResponseEntity<PropertyEnquiryResponse>(enquiryResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<PropertyEnquiryResponse> getDRSActivities(String id, Integer draftOrderId,

			Integer page, Integer size, Optional<String> sort) {

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(id, Services.DRS);

		MultiValueMap<String, String> parameters = transactionService.getDRSDraftOrderParameters(page, size,

				draftOrderId, sort);

		CompletableFuture<TransactionResponse> activity = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_DIGITAL_REGISTRATION_SERVICE,

						parameters);

		CompletableFuture.allOf(customer, activity).join();

		try {

			PropertyEnquiryResponse enquiryResponse = new PropertyEnquiryResponse(customer.get(), activity.get());

			return new ResponseEntity<PropertyEnquiryResponse>(enquiryResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<PropertyEnquiryResponse> getViewColleaguesApplicationsActivities(String id, String timestamp,

			Integer page, Integer size, Optional<String> sort) {

		CompletableFuture<CustomerDetails> customer = userService.getPortalUserDetails(id, Services.VCA);

		MultiValueMap<String, String> parameters = transactionService.getApplicationEnquiryParameters(page, size, id,

				timestamp, sort);

		CompletableFuture<TransactionResponse> activity = transactionService

				.getAsyncTransactions(TransactionService.ENDPOINT_VIEW_COLLEAGUES_APPLICATIONS,

						parameters);

		CompletableFuture.allOf(customer, activity).join();

		try {

			PropertyEnquiryResponse enquiryResponse = new PropertyEnquiryResponse(customer.get(), activity.get());

			return new ResponseEntity<PropertyEnquiryResponse>(enquiryResponse, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	public ResponseEntity<WorklistInformationResponse> buildWorklistInformationRegister(
			WorklistItemResponse worklistItem, Integer page,

			Integer size, String titleNumber) {

		try {

			List<String> titles = new ArrayList<String>();

			if (titleNumber.equals("")) {

				titles = getTitles(worklistItem.getTitleNumbers(), size, page);

			} else {

				titles.add(titleNumber);

			}

			CompletableFuture<RegisterDetails> registerDetails = registerDetailsService
					.getRegisterDetailsSummary(titles, page, size);

			int serviceTypeValue = serviceType.getServiceType(worklistItem.getService());

			if (serviceTypeValue == 1) {

				CompletableFuture<CustomerDetails> customerDetails = new CompletableFuture<CustomerDetails>();

				if (worklistItem.getService().equals("Preliminary Services")) {

					customerDetails = userService.getPrelimUserDetails(worklistItem.getApplicationReference());

				} else {

					customerDetails = userService.getPortalUserDetails(worklistItem.getUserId(),
							worklistItem.getService());

				}

				CompletableFuture.allOf(customerDetails, registerDetails).join();

				WorklistInformationResponse result = new WorklistInformationResponse(worklistItem)

						.customerDetails(customerDetails.get())

						.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

						.registerDetails(registerDetails.get())

						.build();

				return new ResponseEntity<WorklistInformationResponse>(result, HttpStatus.OK);

			} else {

				CustomerDetails customerDetails = new CustomerDetails();

				if (worklistItem.getService().equals("Search for Land and Property Information")) {

					CompletableFuture<CitizenUser> customer = userService.getCitizenUser(worklistItem.getUserUuid(),

							worklistItem.getService());

					CompletableFuture.allOf(customer, registerDetails).join();

					customerDetails = customerMapper.map(customer.get());

				} else {

					CompletableFuture<LegacyUser> customer = userService.getLegacyUser(worklistItem.getService(),

							worklistItem.getUserId());

					CompletableFuture.allOf(customer, registerDetails).join();

					customerDetails = customerMapper.map(customer.get());

				}

				WorklistInformationResponse result = new WorklistInformationResponse(worklistItem)

						.customerDetails(customerDetails)

						.titleDetails(rmBlankTitles(worklistItem.getTitleNumbers()))

						.registerDetails(registerDetails.get())

						.build();

				return new ResponseEntity<WorklistInformationResponse>(result, HttpStatus.OK);

			}

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

	private List<String> getTitles(Set<String> setOfTitles, Integer size, Integer page) {

		log.info("Title list needing registers {}", setOfTitles);

		// Convert set to list for sorting

		List<String> titlesList = new ArrayList<String>();

		titlesList.addAll(rmBlankTitles(setOfTitles));

		Collections.sort(titlesList);

		List<String> returnList = new ArrayList<String>();

		if (page == null) {

			page = 1;

		}

		if (page == 0) {

			// Export required so need all titles

			returnList.addAll(titlesList);

		} else {

			// Set start and end indexes for getting page data

			Integer startIndex = (page - 1) * size;

			Integer endIndex = startIndex + size;

			for (int i = startIndex; i < endIndex && i < titlesList.size(); i++) {

				returnList.add(titlesList.get(i));

			}

		}

		log.info("Returning titles {}", returnList);

		return returnList;

	}

	// Code to handle situation where worklist item has blank title numbers

	private Set<String> rmBlankTitles(Set<String> setOfTitles) {

		log.info("Removing any blank title numbers");

		Set<String> setNoBlanks = new HashSet<String>();

		for (String title : setOfTitles) {

			if (!title.trim().equals("")) {

				setNoBlanks.add(title);

			}

		}

		log.info("Title list after blanks removed {}", setNoBlanks);

		return setNoBlanks;

	}

	public ResponseEntity<CitizenUserHistory> getCitizenUserHistory(UUID userUuid,
			MultiValueMap<String, String> parameters) {

		try {

			CompletableFuture<CitizenUserHistoryResponse> citizenHistory = userService.getCitizenUserHistory(userUuid,
					parameters);

			CompletableFuture.allOf(citizenHistory).join();

			CustomerDetails customerDetails = customerMapper.map(citizenHistory.get());

			CitizenUserHistory response = modelMapper.map(citizenHistory.get(), CitizenUserHistory.class);

			response.setCustomerDetails(customerDetails);

			return new ResponseEntity<CitizenUserHistory>(response, HttpStatus.OK);

		} catch (InterruptedException | ExecutionException e) {

			throw new ApplicationException("CompletableFuture Error", "E500",

					HttpStatus.INTERNAL_SERVER_ERROR.value(), e, false);

		}

	}

}
