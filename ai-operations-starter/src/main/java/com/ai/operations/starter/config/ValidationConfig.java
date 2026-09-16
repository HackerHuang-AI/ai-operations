package com.ai.operations.starter.config;

import jakarta.validation.MessageInterpolator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfig {

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setMessageInterpolator(new LocaleAwareMessageInterpolator(
                new ResourceBundleMessageInterpolator(
                        new PlatformResourceBundleLocator("i18n/validationMessages"))));
        return bean;
    }

    private record LocaleAwareMessageInterpolator(MessageInterpolator delegate) implements MessageInterpolator {
        @Override
        public String interpolate(String messageTemplate, Context context) {
            return delegate.interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
        }

        @Override
        public String interpolate(String messageTemplate, Context context, java.util.Locale locale) {
            return delegate.interpolate(messageTemplate, context, locale);
        }
    }
}

