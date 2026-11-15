package br.com.fiap.soat15.osservice;

import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsBillingService;
import br.com.fiap.soat15.osservice.adapter.out.saga.out.SqsExecucaoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.cloud.aws.sqs.enabled=false"
})
class OsServiceApplicationTests {

	@MockitoBean
	private SqsBillingService sqsBillingService;
	@MockitoBean
	private SqsExecucaoService sqsExecucaoService;

	@Test
	void contextLoads() {
	}

}
