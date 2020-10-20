package cz.fg.oss.pmptt;

import cz.fg.oss.pmptt.spring.DatabaseLayerConfig;
import cz.fg.oss.pmptt.spring.MySqlDataSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = {MySqlDataSource.class, DatabaseLayerConfig.class}
)
@Transactional
public class MySqlPMPTTTest extends AbstractPMPTTTest {

}