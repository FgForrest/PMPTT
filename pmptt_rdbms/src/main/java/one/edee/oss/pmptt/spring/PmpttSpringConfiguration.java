package one.edee.oss.pmptt.spring;

import one.edee.darwin.Darwin;
import one.edee.darwin.DarwinBuilder;
import one.edee.darwin.spring.DarwinConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@Configuration
@EnableTransactionManagement
@Import(DarwinConfiguration.class)
public class PmpttSpringConfiguration {

	@Bean
	public Darwin pmpttDarwin(ApplicationContext applicationContext) {
		return new DarwinBuilder(applicationContext, "pmptt", "1.3")
				.withResourcePath("classpath:/META-INF/pmptt_rdbms/sql/")
				.build();
	}

}
