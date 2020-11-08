package one.edee.oss.pmptt;

import one.edee.oss.pmptt.spring.DatabaseLayerConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = {DatabaseLayerConfig.class}
)
@ActiveProfiles("MYSQL")
public class MySqlPMPTTTest extends AbstractPMPTTTest {

}