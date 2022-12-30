package org.cloudfoundry.promregator.cfaccessor;

import org.cloudfoundry.promregator.JUnitTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CFAccessorCacheCaffeineSpringApplication.class)
@TestPropertySource(locations="../default.properties")
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
class CFAccessorCacheCaffeineInvalidationTest {
	
	/*
	 * Warning! Do not try to merge with CFAccessorCacheCaffeineTest
	 * These tests here require that we have a clean counting state in
	 * Mockito, which is hard to achieve, if we have the tests merged.
	 * If they are separate, this is quite trivial...
	 */

	@Autowired
	private CFAccessor parentMock;
	
	@Autowired
	private CFAccessorCacheCaffeine subject;
	
	@BeforeEach
	public void invalidateCaches() {
		this.subject.invalidateCacheApplications();
		this.subject.invalidateCacheSpace();
		this.subject.invalidateCacheOrg();
		this.subject.invalidateCacheDomain();
	}
	
	@AfterAll
	public static void runCleanup() {
		JUnitTestUtils.cleanUpAll();
	}
	
	@Test
	void testInvalidateCacheApplications() {
		subject.retrieveSpaceSummary("dummy");
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveSpaceSummary("dummy");
		
		subject.invalidateCacheApplications();

		subject.retrieveSpaceSummary("dummy");
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveSpaceSummary("dummy");
	}

	@Test
	void testInvalidateCacheSpace() {
		subject.retrieveSpaceId("dummy1", "dummy2");
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveSpaceId("dummy1", "dummy2");
		
		subject.invalidateCacheSpace();
		
		subject.retrieveSpaceId("dummy1", "dummy2");
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveSpaceId("dummy1", "dummy2");
	}

	@Test
	void testInvalidateCacheOrg() {
		subject.retrieveOrgIdV3("dummy");
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveOrgIdV3("dummy");
		
		subject.invalidateCacheOrg();
		
		subject.retrieveOrgIdV3("dummy");
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveOrgIdV3("dummy");
	}

	@Test
	void testInvalidateCacheDomain() {
		subject.retrieveAllDomains("dummy");
		Mockito.verify(this.parentMock, Mockito.times(1)).retrieveAllDomains("dummy");
		
		subject.invalidateCacheDomain();
		
		subject.retrieveAllDomains("dummy");
		Mockito.verify(this.parentMock, Mockito.times(2)).retrieveAllDomains("dummy");
	}

}
