# How to integrate to your application

Include PMPTT library in your Maven descriptor (`pom.xml`):

``` xml
<dependency>   
    <groupId>one.edee.oss</groupId>   
    <artifactId>pmptt_core</artifactId>   
    <version>1.0.0</version>
</dependency>
<dependency>   
    <groupId>one.edee.oss</groupId>   
    <artifactId>pmptt_rdbms</artifactId>   
    <version>1.0.0</version>
</dependency>
```

Or Gradle:

```
dependencies {
    compile 'one.edee.oss:pmptt_core:1.0.0','one.edee.oss:pmptt_rdbms:1.0.0'
}
```

### Setup database schema

Create Spring Java configuration file:

``` java
@Configuration
@Import(PmpttSpringConfiguration.class)
public class DatabaseLayerConfig {

	@Bean
	public PMPTT pmptt(DataSource dataSource, PlatformTransactionManager transactionManager) {
		return new PMPTT(
			new MySqlStorage(dataSource, transactionManager)
		);
	}
	
	@Bean
	public Hierarchy categoryHierarchy(PMPTT pmptt) {
		return pmptt.getOrCreateHierarchy("category", (short)10, (short)55);
	}
}
```