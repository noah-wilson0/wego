package com.wego.wego;

import com.wego.wego.external.route.kakao.config.KaKaoProperties;
import com.wego.wego.external.route.tmap.config.TMapTransitProperties;
import com.wego.wego.external.tourapi.config.TourApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({TourApiProperties.class, KaKaoProperties.class, TMapTransitProperties.class})
@SpringBootApplication
public class WegoApplication {

	public static void main(String[] args) {
		SpringApplication.run(WegoApplication.class, args);
	}

}
