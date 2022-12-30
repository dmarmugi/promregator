package org.cloudfoundry.promregator.cfaccessor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationDomainsResponse;
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryResponse;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.promregator.JUnitTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CFAccessorCacheCaffeineTimeoutSpringApplication.class)
@TestPropertySource(locations= { "../default.properties" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
class CFAccessorCacheCaffeineTimeoutTest {

	@Autowired
	private CFAccessor parentMock;
	
	@Autowired
	private CFAccessorCacheCaffeine subject;

	@BeforeEach
	void clearCaches() {
		this.subject.invalidateCacheApplications();
		this.subject.invalidateCacheSpace();
		this.subject.invalidateCacheOrg();
		this.subject.invalidateCacheDomain();
	}
	
	@BeforeEach
	void setupMocks() {
		Mockito.reset(this.parentMock);
		Mockito.when(this.parentMock.retrieveOrgIdV3("dummy")).then(new TimeoutMonoAnswer());
		Mockito.when(this.parentMock.retrieveSpaceId("dummy1", "dummy2")).then(new TimeoutMonoAnswer());
		Mockito.when(this.parentMock.retrieveAllApplicationIdsInSpace("dummy1", "dummy2")).then(new TimeoutMonoAnswer());
		Mockito.when(this.parentMock.retrieveSpaceSummary("dummy")).then(new TimeoutMonoAnswer());
		Mockito.when(this.parentMock.retrieveAllDomains("dummy")).then(new TimeoutMonoAnswer());
	}

	public static class TimeoutMonoAnswer implements Answer<Mono<?>> {
		@Override
		public Mono<?> answer(InvocationOnMock invocation) throws Throwable {
			return Mono.error(new TimeoutException("Unit test timeout raised"));
		}
	}


	@AfterAll
	static void runCleanup() {
		JUnitTestUtils.cleanUpAll();
	}
	
	@Test
	void testRetrieveOrgId() throws InterruptedException {
		Mono<org.cloudfoundry.client.v3.organizations.ListOrganizationsResponse> response1 = subject.retrieveOrgIdV3("dummy");
		response1.subscribe();
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveOrgIdV3("dummy");
		
		// required to permit asynchronous updates of caches => test stability
		Thread.sleep(10);
		
		Mono<org.cloudfoundry.client.v3.organizations.ListOrganizationsResponse> response2 = subject.retrieveOrgIdV3("dummy");
		response2.subscribe();
		assertThat(response1).isNotEqualTo(response2);
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveOrgIdV3("dummy");
	}

	@Test
	void testRetrieveSpaceId() throws InterruptedException {
		
		Mono<ListSpacesResponse> response1 = subject.retrieveSpaceId("dummy1", "dummy2");
		response1.subscribe();
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveSpaceId("dummy1", "dummy2");
		
		// required to permit asynchronous updates of caches => test stability
		Thread.sleep(10);
		
		Mono<ListSpacesResponse> response2 = subject.retrieveSpaceId("dummy1", "dummy2");
		response2.subscribe();
		assertThat(response1).isNotEqualTo(response2);
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveSpaceId("dummy1", "dummy2");
	}

	@Test
	void testRetrieveAllApplicationIdsInSpace() throws InterruptedException {
		Mono<ListApplicationsResponse> response1 = subject.retrieveAllApplicationIdsInSpace("dummy1", "dummy2");
		response1.subscribe();
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveAllApplicationIdsInSpace("dummy1", "dummy2");
		
		// required to permit asynchronous updates of caches => test stability
		Thread.sleep(10);
		
		Mono<ListApplicationsResponse> response2 = subject.retrieveAllApplicationIdsInSpace("dummy1", "dummy2");
		response2.subscribe();
		assertThat(response1).isNotEqualTo(response2);
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveAllApplicationIdsInSpace("dummy1", "dummy2");
	}

	@Test
	void testRetrieveSpaceSummary() throws InterruptedException {
		Mono<GetSpaceSummaryResponse> response1 = subject.retrieveSpaceSummary("dummy");
		response1.subscribe();
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveSpaceSummary("dummy");
		
		// required to permit asynchronous updates of caches => test stability
		Thread.sleep(10);
		
		Mono<GetSpaceSummaryResponse> response2 = subject.retrieveSpaceSummary("dummy");
		response2.subscribe();
		assertThat(response1).isNotEqualTo(response2);
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveSpaceSummary("dummy");
	}

	@Test
	void testRetrieveDomains() throws InterruptedException {
		Mono<ListOrganizationDomainsResponse> response1 = subject.retrieveAllDomains("dummy");
		response1.subscribe();
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveAllDomains("dummy");
		
		// required to permit asynchronous updates of caches => test stability
		Thread.sleep(10);
		
		Mono<ListOrganizationDomainsResponse> response2 = subject.retrieveAllDomains("dummy");
		response2.subscribe();
		assertThat(response1).isNotEqualTo(response2);
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveAllDomains("dummy");
	}

}
