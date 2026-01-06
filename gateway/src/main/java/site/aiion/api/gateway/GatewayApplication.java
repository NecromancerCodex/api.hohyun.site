package site.aiion.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Spring Cloud Gateway 제거로 @EnableDiscoveryClient 불필요
@SpringBootApplication
@ComponentScan(basePackages = {
	"site.aiion.api.gateway",
	"site.aiion.api.services"  // 모든 서비스 패키지 스캔 (user, diary, oauth)
})
@EntityScan(basePackages = {
	"site.aiion.api.services.diary",
	"site.aiion.api.services.diary.emotion",
	"site.aiion.api.services.diary.mbti",
	"site.aiion.api.services.user",
	"site.aiion.api.services.about"  // 자기소개 Entity
})
@EnableJpaRepositories(basePackages = {
	"site.aiion.api.services.user",
	"site.aiion.api.services.diary",  // 하위 패키지(diary.emotion, diary.mbti) 자동 포함
	"site.aiion.api.services.about"   // 자기소개 Repository
})
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}

